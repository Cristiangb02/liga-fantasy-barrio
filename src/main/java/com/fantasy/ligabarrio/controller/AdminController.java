package com.fantasy.ligabarrio.controller;

import com.fantasy.ligabarrio.model.*;
import com.fantasy.ligabarrio.repository.*;
import com.fantasy.ligabarrio.service.CalculadoraPuntosService;
import com.fantasy.ligabarrio.service.FantasyService;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/admin")
public class AdminController {

    private final EquipoRepository equipoRepository;
    private final JugadorRepository jugadorRepository;
    private final UsuarioRepository usuarioRepository;
    private final JornadaRepository jornadaRepository;
    private final ActuacionRepository actuacionRepository;
    private final NoticiaRepository noticiaRepository;
    private final OfertaRepository ofertaRepository;
    private final CalculadoraPuntosService calculadora;
    private final FantasyService fantasyService;

    public AdminController(EquipoRepository equipoRepository, JugadorRepository jugadorRepository,
                           UsuarioRepository usuarioRepository, JornadaRepository jornadaRepository,
                           ActuacionRepository actuacionRepository, NoticiaRepository noticiaRepository,
                           OfertaRepository ofertaRepository, CalculadoraPuntosService calculadora,
                           FantasyService fantasyService) {
        this.equipoRepository = equipoRepository;
        this.jugadorRepository = jugadorRepository;
        this.usuarioRepository = usuarioRepository;
        this.jornadaRepository = jornadaRepository;
        this.actuacionRepository = actuacionRepository;
        this.noticiaRepository = noticiaRepository;
        this.ofertaRepository = ofertaRepository;
        this.calculadora = calculadora;
        this.fantasyService = fantasyService;
    }

    // --- GET ---
    @GetMapping("/estado-bloqueo")
    public boolean getEstadoBloqueo() {
        return fantasyService.getJornadaActiva().isBloqueada();
    }

    @GetMapping("/usuarios-gestion")
    public List<Usuario> getUsuariosGestion() {
        return usuarioRepository.findAll();
    }

    @GetMapping("/pendientes")
    public List<Usuario> verUsuariosPendientes() {
        return usuarioRepository.findAll().stream().filter(u -> !u.isActivo()).collect(Collectors.toList());
    }

    @GetMapping("/jugadores-puntuados")
    public List<Jugador> getJugadoresPuntuados() {
        Jornada actual = fantasyService.getJornadaActiva();
        return jugadorRepository.findAll().stream()
                .filter(j -> actuacionRepository.findByJugadorAndJornada(j, actual).isPresent())
                .sorted((j1, j2) -> {
                    int p1 = fantasyService.getPesoPosicion(j1.getPosicion());
                    int p2 = fantasyService.getPesoPosicion(j2.getPosicion());
                    if (p1 != p2) return Integer.compare(p1, p2);
                    else return j1.getNombre().compareToIgnoreCase(j2.getNombre());
                }).collect(Collectors.toList());
    }

    @GetMapping("/jugadores-pendientes")
    public List<Jugador> getJugadoresPendientes() {
        Jornada actual = fantasyService.getJornadaActiva();
        List<Jugador> todos = jugadorRepository.findAll();
        List<Long> idsPuntuados = actuacionRepository.findAll().stream()
                .filter(a -> a.getJornada().getId().equals(actual.getId()))
                .map(a -> a.getJugador().getId())
                .collect(Collectors.toList());
        return todos.stream()
                .filter(j -> !idsPuntuados.contains(j.getId()))
                .sorted(Comparator.comparing(Jugador::getNombre))
                .collect(Collectors.toList());
    }

    // --- POST ---
    @PostMapping("/toggle-bloqueo")
    public String toggleBloqueo() {
        Jornada actual = fantasyService.getJornadaActiva();
        if (actual.isBloqueada()) {
            actual.setBloqueada(false);
            actual.setDiaBloqueo(null);
        } else {
            actual.setBloqueada(true);
            actual.setDiaBloqueo(java.time.LocalDate.now(java.time.ZoneId.of("Europe/Madrid")));
        }
        jornadaRepository.save(actual);
        return "Bloqueo de acciones " + (actual.isBloqueada() ? "ACTIVADO 🔒" : "DESACTIVADO 🔓");
    }

    @PostMapping("/aprobar/{idUsuario}")
    public String aprobarUsuario(@PathVariable Long idUsuario) {
        Usuario u = usuarioRepository.findById(idUsuario).orElseThrow();
        u.setActivo(true);
        usuarioRepository.save(u);
        noticiaRepository.save(new Noticia("👋 BIENVENIDA: " + u.getNombre() + " ha entrado a la liga."));
        return "✅ Usuario aprobado.";
    }

    @PostMapping("/editar-usuario/{idUsuario}")
    public String editarUsuario(@PathVariable Long idUsuario, @RequestBody Map<String, String> datos) {
        String nuevoNombre = datos.get("nombre");
        if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) return "❌ El nombre no puede estar vacío.";
        Usuario existente = usuarioRepository.findByNombre(nuevoNombre);
        if (existente != null && !existente.getId().equals(idUsuario)) return "❌ Ese nombre ya está en uso por otro jugador.";
        Usuario u = usuarioRepository.findById(idUsuario).orElseThrow();
        String antiguo = u.getNombre();
        u.setNombre(nuevoNombre);
        usuarioRepository.save(u);
        return "✅ Se ha cambiado el nombre '" + antiguo + "' a '" + nuevoNombre + "'.";
    }

    @PostMapping("/registrar")
    public String registrarPartido(@RequestBody DatosPartido datos) {
        Jugador jugador = jugadorRepository.findById(datos.idJugador).orElseThrow();
        Jornada jornada = fantasyService.getJornadaActiva();
        Actuacion actuacion = actuacionRepository.findByJugadorAndJornada(jugador, jornada).orElse(new Actuacion(jugador, jornada));

        actuacion.setJugado(datos.jugado);
        actuacion.setVictoria(datos.victoria);
        actuacion.setDerrota(datos.derrota);
        actuacion.setGolesMarcados(datos.goles);
        actuacion.setGolesEncajados(datos.golesEncajados);
        actuacion.setAutogoles(datos.autogoles);
        actuacion.setColorEquipo(datos.colorEquipo);

        int puntos = calculadora.calcularPuntos(actuacion);
        actuacion.setPuntosTotales(puntos);
        actuacionRepository.save(actuacion);

        jugador.setPuntosAcumulados(jugador.getPuntosAcumulados() + puntos);
        int nuevoValor = Math.max(150_000, jugador.getValor() + (puntos * 100_000));
        jugador.setValor(nuevoValor);

        if (jugador.getPropietario() != null && jugador.getClausula() < jugador.getValor()) {
            jugador.setClausula(jugador.getValor());
        }
        jugadorRepository.save(jugador);
        return "✅ Puntos de " + jugador.getNombre() + " en esta jornada: " + puntos;
    }

    @PostMapping("/cerrar-jornada")
    public String cerrarJornada() {
        Jornada actual = fantasyService.getJornadaActiva();
        List<Equipo> equipos = equipoRepository.findByJornada(actual);
        StringBuilder res = new StringBuilder();
        for (Equipo e : equipos) {
            Usuario m = e.getUsuario();
            if (m.getPresupuesto() < 0) {
                e.setPuntosTotalesJornada(0);
                equipoRepository.save(e);
                res.append("🚫 ").append(m.getNombre()).append(" (Saldo Negativo)\n");
                continue;
            }
            int total = 0;
            for(Jugador j : e.getJugadoresAlineados()) {
                total += actuacionRepository.findByJugadorAndJornada(j, actual).map(Actuacion::getPuntosTotales).orElse(0);
            }
            e.setPuntosTotalesJornada(total);
            equipoRepository.save(e);
            res.append("✅ ").append(m.getNombre()).append(": ").append(total).append("p\n");
        }

        Jornada nueva = new Jornada();
        nueva.setNumero(actual.getNumero() + 1);
        nueva.setBloqueada(actual.isBloqueada());
        jornadaRepository.save(nueva);

        noticiaRepository.save(new Noticia("🏁 JORNADA " + actual.getNumero() + " FINALIZADA.\n" + res));
        return "✅ Jornada terminada.";
    }

    @PostMapping("/reset-mercado")
    public String resetMercado() {
        fantasyService.incrementarDesplazamiento();
        return "✅ Mercado renovado con éxito.";
    }

    @PostMapping("/reset-liga")
    public String resetearLiga() {
        List<Jugador> jugadores = jugadorRepository.findAll();
        for (Jugador j : jugadores) {
            j.setPropietario(null);
            j.setPuntosAcumulados(0);
            j.setClausula(j.getValor());
            j.setJornadaFichaje(0L);
            j.setFechaFichaje(null);
            j.setFechaFinBlindaje(null);
            j.setFechaVenta(null);
        }
        jugadorRepository.saveAll(jugadores);
        List<Usuario> usuarios = usuarioRepository.findAll();
        for (Usuario u : usuarios) {
            u.setPresupuesto(100_000_000);
            u.setActivo(true);
            u.setPuntosExtra(0);
        }
        usuarioRepository.saveAll(usuarios);
        equipoRepository.deleteAll();
        actuacionRepository.deleteAll();
        noticiaRepository.deleteAll();
        ofertaRepository.deleteAll();
        jornadaRepository.deleteAll();

        Jornada j1 = new Jornada();
        j1.setNumero(1);
        j1.setBloqueada(false);
        jornadaRepository.save(j1);

        noticiaRepository.save(new Noticia("☢️ LIGA RESETEADA."));
        return "✅ Liga reseteada.";
    }

    @PostMapping("/reset-puntos/{idJugador}")
    public String resetearPuntosJugador(@PathVariable Long idJugador) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Jornada jornada = fantasyService.getJornadaActiva();
        Optional<Actuacion> actaOpt = actuacionRepository.findByJugadorAndJornada(jugador, jornada);

        if (actaOpt.isEmpty()) return "❌ Este jugador no tiene puntos registrados en esta jornada.";

        Actuacion acta = actaOpt.get();
        int puntosRestar = acta.getPuntosTotales();
        int valorRestar = puntosRestar * 100_000;

        jugador.setPuntosAcumulados(jugador.getPuntosAcumulados() - puntosRestar);
        jugador.setValor(jugador.getValor() - valorRestar);
        jugador.setClausula(jugador.getClausula() - valorRestar);

        actuacionRepository.delete(acta);
        jugadorRepository.save(jugador);
        return "✅ CORREGIDO: Puntos de " + jugador.getNombre() + " reseteados. (Restados " + puntosRestar + " pts y " + fantasyService.formatearDinero(valorRestar) + " valor)";
    }

    @PostMapping("/eliminar-jugador/{id}")
    public String eliminarJugador(@PathVariable Long id) {
        Optional<Jugador> jOpt = jugadorRepository.findById(id);
        if (jOpt.isEmpty()) return "❌ Error: El jugador no existe.";

        Jugador j = jOpt.get();
        String nombre = j.getNombre();
        if(j.getPropietario() != null) {
            j.setPropietario(null);
            jugadorRepository.save(j);
        }

        actuacionRepository.deleteAll(actuacionRepository.findByJugador(j));
        ofertaRepository.deleteAll(ofertaRepository.findByJugador(j));
        jugadorRepository.delete(j);
        return "✅ Jugador eliminado: " + nombre;
    }

    @PostMapping("/reset-puntos-jornada/{idJugador}/{numJornada}")
    public String resetPuntosJornada(@PathVariable Long idJugador, @PathVariable int numJornada) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Jornada jornada = jornadaRepository.findAll().stream().filter(j -> j.getNumero() == numJornada).findFirst().orElseThrow();

        Optional<Actuacion> actaOpt = actuacionRepository.findByJugadorAndJornada(jugador, jornada);
        if (actaOpt.isEmpty()) return "❌ Este jugador no tiene puntos registrados en la jornada " + numJornada + ".";

        Actuacion acta = actaOpt.get();
        int puntosRestar = acta.getPuntosTotales();
        int valorRestar = puntosRestar * 100_000;

        jugador.setPuntosAcumulados(jugador.getPuntosAcumulados() - puntosRestar);
        jugador.setValor(jugador.getValor() - valorRestar);
        jugador.setClausula(jugador.getClausula() - valorRestar);

        actuacionRepository.delete(acta);
        jugadorRepository.save(jugador);

        return "✅ Puntos de " + jugador.getNombre() + " eliminados de la jornada " + numJornada + ".";
    }

    @PostMapping("/add-puntos-jornada/{idJugador}/{numJornada}/{puntos}/{color}")
    public String addPuntosJornada(@PathVariable Long idJugador, @PathVariable int numJornada, @PathVariable int puntos, @PathVariable String color) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Jornada jornada = jornadaRepository.findAll().stream().filter(j -> j.getNumero() == numJornada).findFirst().orElseThrow();

        Actuacion acta = new Actuacion(jugador, jornada);
        acta.setPuntosTotales(puntos);
        acta.setJugado(true);
        acta.setColorEquipo(color);
        actuacionRepository.save(acta);

        int valorSumar = puntos * 100_000;
        jugador.setPuntosAcumulados(jugador.getPuntosAcumulados() + puntos);
        jugador.setValor(jugador.getValor() + valorSumar);

        if (jugador.getClausula() < jugador.getValor()) {
            jugador.setClausula(jugador.getValor());
        }
        jugadorRepository.save(jugador);
        return "✅ " + puntos + " puntos añadidos a " + jugador.getNombre() + " en la jornada " + numJornada + ".";
    }

    @PostMapping("/modificar-puntos-extra/{idUsuario}/{puntos}")
    public String modificarPuntosExtra(@PathVariable Long idUsuario, @PathVariable int puntos) {
        Usuario u = usuarioRepository.findById(idUsuario).orElseThrow();
        u.setPuntosExtra(u.getPuntosExtra() + puntos);
        usuarioRepository.save(u);

        String accion = puntos >= 0 ? "añadido" : "restado";
        return "✅ Se han " + accion + " " + Math.abs(puntos) + " puntos a " + u.getNombre() + " en la clasificación general.";
    }

    @PostMapping("/cambiar-estado/{idJugador}/{nuevoEstado}")
    public String cambiarEstadoJugador(@PathVariable Long idJugador, @PathVariable String nuevoEstado) {
        Jugador j = jugadorRepository.findById(idJugador).orElseThrow();

        String estadoLimpio = nuevoEstado.replace("-", " ");
        j.setEstado(estadoLimpio);
        jugadorRepository.save(j);

        return "✅ Estado de " + j.getNombre() + " cambiado a " + estadoLimpio;
    }

    @PostMapping("/actualizar-imagen/{idJugador}")
    public String actualizarImagen(@PathVariable Long idJugador, @RequestBody Map<String, String> datos) {
        String nuevaUrl = datos.get("urlImagen");
        if (nuevaUrl == null || nuevaUrl.trim().isEmpty()) {
            return "❌ Error: La ruta de la imagen no puede estar vacía.";
        }

        Jugador j = jugadorRepository.findById(idJugador).orElseThrow();
        j.setUrlImagen(nuevaUrl.trim());
        jugadorRepository.save(j);

        return "✅ Foto de " + j.getNombre() + " (" + j.getPosicion() + ") " + " actualizada correctamente.";
    }

    @PostMapping("/actualizar-avatar/{idUsuario}")
    public String actualizarAvatarUsuario(@PathVariable Long idUsuario, @RequestBody Map<String, String> datos) {
        String nuevaUrl = datos.get("urlImagen");
        if (nuevaUrl == null || nuevaUrl.trim().isEmpty()) {
            return "❌ Error: La ruta de la imagen no puede estar vacía.";
        }
        Usuario u = usuarioRepository.findById(idUsuario).orElseThrow();
        u.setUrlImagen(nuevaUrl.trim());
        usuarioRepository.save(u);

        return "✅ Foto de perfil de " + u.getNombre() + " actualizada correctamente.";
    }

    @PostMapping("/modificar-saldo/{idUsuario}/{cantidad}")
    public String modificarSaldo(@PathVariable Long idUsuario, @PathVariable int cantidad) {
        if (cantidad == 0) return "❌ La cantidad no puede ser cero.";

        String accion = cantidad > 0 ? "ingresado" : "retirado";
        //Si el ID es 0, es para a TODOS los mánagers
        if (idUsuario == 0L) {
            List<Usuario> usuarios = usuarioRepository.findAll().stream().filter(Usuario::isActivo).collect(Collectors.toList());
            for (Usuario u : usuarios) {
                u.setPresupuesto(u.getPresupuesto() + cantidad);
            }
            usuarioRepository.saveAll(usuarios);
            return "✅ Se han " + accion + " " + fantasyService.formatearDinero(Math.abs(cantidad)) + " a todos los mánagers.";
        }
        else {
            Usuario u = usuarioRepository.findById(idUsuario).orElseThrow();
            u.setPresupuesto(u.getPresupuesto() + cantidad);
            usuarioRepository.save(u);
            return "✅ Se han " + accion + " " + fantasyService.formatearDinero(Math.abs(cantidad)) + " a " + u.getNombre() + ".";
        }
    }

    // --- DELETE ---
    @DeleteMapping("/rechazar/{idUsuario}")
    public String rechazarUsuario(@PathVariable Long idUsuario) {
        usuarioRepository.deleteById(idUsuario);
        return "🗑️ Solicitud rechazada.";
    }

    @DeleteMapping("/eliminar-usuario/{idUsuario}")
    public String eliminarUsuario(@PathVariable Long idUsuario) {
        Usuario u = usuarioRepository.findById(idUsuario).orElseThrow();
        if(u.isEsAdmin() && !u.getNombre().equals("Cristian")) return "❌ No se puede borrar al admin.";

        jugadorRepository.findAll().stream().filter(j -> j.getPropietario() != null && j.getPropietario().getId().equals(idUsuario)).forEach(j -> {
            j.setPropietario(null);
            j.setClausula(j.getValor());
            jugadorRepository.save(j);
        });
        equipoRepository.deleteAll(equipoRepository.findByUsuario(u));
        List<Oferta> ofertasRelacionadas = ofertaRepository.findAll().stream().filter(o -> o.getVendedor().getId().equals(idUsuario) || o.getComprador().getId().equals(idUsuario)).collect(Collectors.toList());
        ofertaRepository.deleteAll(ofertasRelacionadas);
        usuarioRepository.delete(u);
        return "✅ Usuario eliminado correctamente.";
    }
}