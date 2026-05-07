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

    private final EquipoRepository er;
    private final JugadorRepository jR;
    private final UsuarioRepository uR;
    private final JornadaRepository joR;
    private final ActuacionRepository aR;
    private final NoticiaRepository nR;
    private final OfertaRepository oR;
    private final CalculadoraPuntosService calculadora;
    private final FantasyService fS;

    public AdminController(EquipoRepository er, JugadorRepository jR, UsuarioRepository uR, JornadaRepository joR,
                           ActuacionRepository aR, NoticiaRepository nR, OfertaRepository oR, CalculadoraPuntosService calculadora,
                           FantasyService fS) {
        this.er = er;
        this.jR = jR;
        this.uR = uR;
        this.joR = joR;
        this.aR = aR;
        this.nR = nR;
        this.oR = oR;
        this.calculadora = calculadora;
        this.fS = fS;
    }

    //GET-MAPPPING
    @GetMapping("/estado-bloqueo")
    public boolean getEstadoBloqueo() {
        return fS.getJornadaActiva().isBloqueada();
    }

    @GetMapping("/usuarios-gestion")
    public List<Usuario> getUsuariosGestion() {
        return uR.findAll();
    }

    @GetMapping("/pendientes")
    public List<Usuario> verUsuariosPendientes() {
        return uR.findAll().stream().filter(u -> !u.isActivo()).collect(Collectors.toList());
    }

    @GetMapping("/jugadores-puntuados")
    public List<Jugador> getJugadoresPuntuados() {
        Jornada actual = fS.getJornadaActiva();
        return jR.findAll().stream().filter(j -> aR.findByJugadorAndJornada(j, actual).isPresent())
                .sorted((j1, j2) -> {
                    int p1 = fS.getPesoPosicion(j1.getPosicion());
                    int p2 = fS.getPesoPosicion(j2.getPosicion());
                    if (p1 != p2) return Integer.compare(p1, p2);
                    else return j1.getNombre().compareToIgnoreCase(j2.getNombre());
                }).collect(Collectors.toList());
    }

    @GetMapping("/jugadores-pendientes")
    public List<Jugador> getJugadoresPendientes() {
        Jornada actual = fS.getJornadaActiva();
        List<Jugador> todos = jR.findAll();
        List<Long> idsPuntuados = aR.findAll().stream().filter(a -> a.getJornada().getId().equals(actual.getId()))
                .map(a -> a.getJugador().getId()).collect(Collectors.toList());

        return todos.stream().filter(j -> !idsPuntuados.contains(j.getId())).sorted(Comparator.comparing(Jugador::getNombre))
                .collect(Collectors.toList());
    }

    //BOTÓN PARA BORRAR CLONES
    @GetMapping("/limpiar-clones/{numJornada}")
    public String limpiarClonesJornada(@PathVariable int numJornada) {
        String msj = "";
        Jornada jornada = joR.findAll().stream().filter(j -> j.getNumero() == numJornada).findFirst().orElseThrow();

        List<Actuacion> todas = aR.findAll().stream().filter(a -> a.getJornada().getId().equals(jornada.getId()))
                .collect(Collectors.toList());

        Map<Long, List<Actuacion>> porJugador = todas.stream().collect(Collectors.groupingBy(a -> a.getJugador().getId()));
        int borrados = 0;

        for (List<Actuacion> lista : porJugador.values()) {
            //Si hay más de 1 actuación para el mismo jugador, es un clon!
            if (lista.size() > 1) {
                //Ordenamos para mantener el que tenga color guardado
                lista.sort((a, b) -> {
                    if (a.getColorEquipo() != null && b.getColorEquipo() == null) return -1;
                    if (b.getColorEquipo() != null && a.getColorEquipo() == null) return 1;
                    return b.getId().compareTo(a.getId());
                });

                for (int i = 1; i < lista.size(); i++) {
                    Actuacion clon = lista.get(i);
                    Jugador j = clon.getJugador();

                    int puntosSobrantes = clon.getPuntosTotales();
                    int valorSobrante = puntosSobrantes * 100_000;

                    j.setPuntosAcumulados(j.getPuntosAcumulados() - puntosSobrantes);
                    j.setValor(j.getValor() - valorSobrante);
                    j.setClausula(j.getClausula() - valorSobrante);

                    jR.save(j);
                    aR.delete(clon);
                    borrados++;
                }
            }
        }
        msj = "Se han eliminado  " + borrados + " clones y se ajustaron sus puntos generales.";
        return msj;
    }

    //POST-MAPPING
    @PostMapping("/toggle-bloqueo")
    public String toggleBloqueo() {
        Jornada actual = fS.getJornadaActiva();
        if (actual.isBloqueada()) {
            actual.setBloqueada(false);
            actual.setDiaBloqueo(null);
        } else {
            actual.setBloqueada(true);
            actual.setDiaBloqueo(java.time.LocalDate.now(java.time.ZoneId.of("Europe/Madrid")));
        }
        joR.save(actual);
        return "Bloqueo de acciones " + (actual.isBloqueada() ? "ACTIVADO 🔒" : "DESACTIVADO 🔓");
    }

    @PostMapping("/aprobar/{idUsuario}")
    public String aprobarUsuario(@PathVariable Long idUsuario) {
        Usuario u = uR.findById(idUsuario).orElseThrow();
        u.setActivo(true);
        uR.save(u);
        nR.save(new Noticia("👋 BIENVENIDA: " + u.getNombre() + " ha entrado a la liga."));
        return "✅ Usuario aprobado.";
    }

    @PostMapping("/editar-usuario/{idUsuario}")
    public String editarUsuario(@PathVariable Long idUsuario, @RequestBody Map<String, String> datos) {
        String nuevoNombre = datos.get("nombre");
        String msj = "";
        Usuario existente = uR.findByNombre(nuevoNombre);

        if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) {
            msj = "❌ El nombre no puede estar vacío.";
        } else if (existente != null && !existente.getId().equals(idUsuario)) {
            msj = "❌ Ese nombre ya está en uso por otro jugador.";
        } else {
            Usuario u = uR.findById(idUsuario).orElseThrow();
            String antiguo = u.getNombre();
            u.setNombre(nuevoNombre);
            uR.save(u);
            msj = "✅ Se ha cambiado el nombre '" + antiguo + "' a '" + nuevoNombre + "'.";
        }
        return msj;
    }

    @PostMapping("/registrar")
    public String registrarPartido(@RequestBody DatosPartido datos) {
        String msj = "";
        Jugador jugador = jR.findById(datos.idJugador).orElseThrow();
        Jornada jornada = fS.getJornadaActiva();
        Actuacion actuacion = aR.findByJugadorAndJornada(jugador, jornada).orElse(new Actuacion(jugador, jornada));

        actuacion.setJugado(datos.jugado);
        actuacion.setVictoria(datos.victoria);
        actuacion.setDerrota(datos.derrota);
        actuacion.setGolesMarcados(datos.goles);
        actuacion.setGolesEncajados(datos.golesEncajados);
        actuacion.setAutogoles(datos.autogoles);
        actuacion.setColorEquipo(datos.colorEquipo);

        int puntos = calculadora.calcularPuntos(actuacion);
        actuacion.setPuntosTotales(puntos);
        aR.save(actuacion);

        jugador.setPuntosAcumulados(jugador.getPuntosAcumulados() + puntos);
        int nuevoValor = Math.max(150_000, jugador.getValor() + (puntos * 100_000)); //Un jugador no valdrá menos de 150.000€
        jugador.setValor(nuevoValor);

        //Si lo tiene alguien y la clausula es menor que su valor, se pone la cláusula al valor de mercado
        if ((jugador.getPropietario() != null) && (jugador.getClausula() < jugador.getValor())) {
            jugador.setClausula(jugador.getValor());
        }
        jR.save(jugador);
        msj = "✅ Puntos de " + jugador.getNombre() + " en esta jornada: " + puntos;
        return msj;
    }

    @PostMapping("/cerrar-jornada")
    public String cerrarJornada() {
        String msj = "";
        Jornada actual = fS.getJornadaActiva();
        List<Equipo> equipos = er.findByJornada(actual);
        StringBuilder res = new StringBuilder();

        for (Equipo e : equipos) {
            Usuario u = e.getUsuario();
            if (u.getPresupuesto() < 0) {
                e.setPuntosTotalesJornada(0);
                er.save(e);
                res.append("🚫 ").append(u.getNombre()).append(" (Saldo Negativo)\n");
                continue;
            }
            int total = 0;
            for(Jugador j : e.getJugadoresAlineados()) {
                total += aR.findByJugadorAndJornada(j, actual).map(Actuacion::getPuntosTotales).orElse(0);
            }
            e.setPuntosTotalesJornada(total);
            er.save(e);
            res.append("✅ ").append(u.getNombre()).append(": ").append(total).append("p\n");
        }

        Jornada nueva = new Jornada();
        nueva.setNumero(actual.getNumero() + 1);
        nueva.setBloqueada(actual.isBloqueada());

        joR.save(nueva);
        nR.save(new Noticia("🏁 JORNADA " + actual.getNumero() + " FINALIZADA.\n" + res));

        msj = "✅ Jornada terminada.";
        return msj;
    }

    @PostMapping("/reset-mercado")
    public String resetMercado() {
        String msj = "";
        fS.incrementarDesplazamiento();
        msj =  "✅ Mercado renovado con éxito.";
        return msj;
    }

    @PostMapping("/reset-liga")
    public String resetearLiga() {
        String msj = "";
        List<Jugador> jugadores = jR.findAll();
        for (Jugador j : jugadores) {
            j.setPropietario(null);
            j.setPuntosAcumulados(0);
            j.setClausula(j.getValor());
            j.setJornadaFichaje(0L);
            j.setFechaFichaje(null);
            j.setFechaFinBlindaje(null);
            j.setFechaVenta(null);
        }

        jR.saveAll(jugadores);
        List<Usuario> usuarios = uR.findAll();
        for (Usuario u : usuarios) {
            u.setPresupuesto(100_000_000);
            u.setActivo(true);
            u.setPuntosExtra(0);
        }
        uR.saveAll(usuarios);
        er.deleteAll();
        aR.deleteAll();
        nR.deleteAll();
        oR.deleteAll();
        joR.deleteAll();

        Jornada j1 = new Jornada();
        j1.setNumero(1);
        j1.setBloqueada(false);
        joR.save(j1);

        nR.save(new Noticia("LIGA RESETEADA."));
        msj = "✅ Liga reseteada.";
        return msj;
    }

    @PostMapping("/reset-puntos/{idJugador}")
    public String resetearPuntosJugador(@PathVariable Long idJugador) {
        String msj = "";
        Jugador jugador = jR.findById(idJugador).orElseThrow();
        Jornada jornada = fS.getJornadaActiva();
        Optional<Actuacion> actaOpt = aR.findByJugadorAndJornada(jugador, jornada);

        if (actaOpt.isEmpty()) {
            msj = "❌ Este jugador no tiene puntos registrados en esta jornada.";
        } else {
            Actuacion acta = actaOpt.get();
            int puntosRestar = acta.getPuntosTotales();
            int valorRestar = puntosRestar * 100_000;

            jugador.setPuntosAcumulados(jugador.getPuntosAcumulados() - puntosRestar);
            jugador.setValor(jugador.getValor() - valorRestar);
            jugador.setClausula(jugador.getClausula() - valorRestar);

            aR.delete(acta);
            jR.save(jugador);
            msj = "✅ CORREGIDO: Puntos de " + jugador.getNombre() + "(" + jugador.getPosicion() + ") "
                    + puntosRestar + " pts y " + fS.formatearDinero(valorRestar) + " valor)";
        }
        return msj;
    }

    @PostMapping("/eliminar-jugador/{id}")
    public String eliminarJugador(@PathVariable Long id) {
        String msj = "";
        Optional<Jugador> jOpt = jR.findById(id);
        if (jOpt.isEmpty()) {
            msj = "❌ Error: El jugador no existe.";
        } else {
            Jugador j = jOpt.get();
            if(j.getPropietario() != null) {
                j.setPropietario(null);
                jR.save(j);
            }
            aR.deleteAll(aR.findByJugador(j));
            oR.deleteAll(oR.findByJugador(j));
            jR.delete(j);

            msj = "✅ El jugador " + j.getNombre() + "(" + j.getPosicion() + ") ha sido eliminado.";
        }
        return msj;
    }

    @PostMapping("/reset-puntos-jornada/{idJugador}/{numJornada}")
    public String resetPuntosJornada(@PathVariable Long idJugador, @PathVariable int numJornada) {
        String msj = "";
        Jugador jug = jR.findById(idJugador).orElseThrow();
        Jornada jornada = joR.findAll().stream().filter(j -> j.getNumero() == numJornada).findFirst().orElseThrow();

        Optional<Actuacion> actaOpt = aR.findByJugadorAndJornada(jug, jornada);
        if (actaOpt.isEmpty())  {
            msj = "❌ Este jugador no tiene puntos registrados en la jornada " + numJornada + ".";
        } else {
            Actuacion acta = actaOpt.get();
            int puntosRestar = acta.getPuntosTotales();
            int valorRestar = puntosRestar * 100_000;

            jug.setPuntosAcumulados(jug.getPuntosAcumulados() - puntosRestar);
            jug.setValor(jug.getValor() - valorRestar);
            jug.setClausula(jug.getClausula() - valorRestar);

            aR.delete(acta);
            jR.save(jug);
            msj = "✅ Los puntos de " + jug.getNombre() + "(" + jug.getPosicion() + ") han sido eliminados de la jornada " + numJornada + ".";
        }
        return msj;
    }

    @PostMapping("/add-puntos-jornada/{idJugador}/{numJornada}/{puntos}/{color}")
    public String addPuntosJornada(@PathVariable Long idJugador, @PathVariable int numJornada, @PathVariable int puntos, @PathVariable String color) {
        String msj = "";
        Jugador jugador = jR.findById(idJugador).orElseThrow();
        Jornada jornada = joR.findAll().stream().filter(j -> j.getNumero() == numJornada).findFirst().orElseThrow();

        List<Actuacion> actas = aR.findAll().stream().filter(a -> a.getJugador().getId().
                        equals(idJugador) && a.getJornada().getId().equals(jornada.getId())).collect(Collectors.toList());

        Actuacion acta;
        if (!actas.isEmpty()) {
            acta = actas.get(0); //Si ya existe, la actualizamos
        } else {
            acta = new Actuacion(jugador, jornada); //Si no existe, la creamos
        }

        acta.setPuntosTotales(puntos);
        acta.setJugado(true);
        acta.setColorEquipo(color);
        aR.save(acta);

        int valorSumar = puntos * 100_000;
        jugador.setPuntosAcumulados(jugador.getPuntosAcumulados() + puntos);
        jugador.setValor(jugador.getValor() + valorSumar);

        if (jugador.getClausula() < jugador.getValor()) {
            jugador.setClausula(jugador.getValor());
        }
        jR.save(jugador);

        msj = "✅ El jugador " + jugador.getNombre() + "("+ jugador.getPosicion() + ") ha hecho " + puntos
                + " puntos en la jornada " + numJornada + ".";
        return msj;
    }

    @PostMapping("/modificar-puntos-extra/{idUsuario}/{puntos}")
    public String modificarPuntosExtra(@PathVariable Long idUsuario, @PathVariable int puntos) {
        String msj = "";
        Usuario u = uR.findById(idUsuario).orElseThrow();
        u.setPuntosExtra(u.getPuntosExtra() + puntos);
        uR.save(u);

        String accion = "";
        if (puntos >=0 ) {
            accion = "añadido";
        } else {
            accion = "restado";
        }
        msj = "✅ Se han " + accion + " " + Math.abs(puntos) + " puntos a " + u.getNombre() + " en la clasificación general.";
        return msj;
    }

    @PostMapping("/cambiar-estado/{idJugador}/{nuevoEstado}")
    public String cambiarEstadoJugador(@PathVariable Long idJugador, @PathVariable String nuevoEstado) {
        String msj = "";
        Jugador j = jR.findById(idJugador).orElseThrow();

        String estadoLimpio = nuevoEstado.replace("-", " ");
        j.setEstado(estadoLimpio);
        jR.save(j);

        msj = "✅ Estado de " + j.getNombre() + " cambiado a " + estadoLimpio;
        return msj;
    }

    @PostMapping("/actualizar-imagen/{idJugador}")
    public String actualizarImagen(@PathVariable Long idJugador, @RequestBody Map<String, String> datos) {
        String msj = "";
        String nuevaUrl = datos.get("urlImagen");
        if (nuevaUrl == null || nuevaUrl.trim().isEmpty()) {
            msj = "❌ Error: La ruta de la imagen no puede estar vacía.";
        } else {
            Jugador j = jR.findById(idJugador).orElseThrow();
            j.setUrlImagen(nuevaUrl.trim());
            jR.save(j);
            msj = " ✅ La foto de " + j.getNombre() + " (" + j.getPosicion() + ") " + " ha sido actualizada.";
        }
        return msj;
    }

    @PostMapping("/actualizar-avatar/{idUsuario}")
    public String actualizarAvatarUsuario(@PathVariable Long idUsuario, @RequestBody Map<String, String> datos) {
        String msj = "";
        String nuevaUrl = datos.get("urlImagen");
        if (nuevaUrl == null || nuevaUrl.trim().isEmpty()) {
            msj = "❌ Error. La ruta de la imagen no puede estar vacía.";
        } else {
            Usuario u = uR.findById(idUsuario).orElseThrow();
            u.setUrlImagen(nuevaUrl.trim());
            uR.save(u);

            msj = "✅ La foto de perfil de " + u.getNombre() + " ha sido actualizada.";
        }
        return msj;
    }

    @PostMapping("/modificar-saldo/{idUsuario}/{cantidad}")
    public String modificarSaldo(@PathVariable Long idUsuario, @PathVariable int cantidad) {
        String msj = "";
        if (cantidad == 0) return "❌ La cantidad no puede ser cero.";

        String accion = cantidad > 0 ? "ingresado" : "retirado";
        //Si el ID es 0, es para a TODOS los mánagers
        if (idUsuario == 0L) {
            List<Usuario> usuarios = uR.findAll().stream().filter(Usuario::isActivo).collect(Collectors.toList());
            for (Usuario u : usuarios) {
                u.setPresupuesto(u.getPresupuesto() + cantidad);
            }
            uR.saveAll(usuarios);
            return "✅ Se han " + accion + " " + fS.formatearDinero(Math.abs(cantidad)) + " a todos los mánagers.";
        }
        else {
            Usuario u = uR.findById(idUsuario).orElseThrow();
            u.setPresupuesto(u.getPresupuesto() + cantidad);
            uR.save(u);
            return "✅ Se han " + accion + " " + fS.formatearDinero(Math.abs(cantidad)) + " a " + u.getNombre() + ".";
        }
    }

    //DELETE-MAPPING
    @DeleteMapping("/rechazar/{idUsuario}")
    public String rechazarUsuario(@PathVariable Long idUsuario) {
        String msj = "";
        uR.deleteById(idUsuario);
        msj = "Solicitud rechazada.";
        return msj;
    }

    @DeleteMapping("/eliminar-usuario/{idUsuario}")
    public String eliminarUsuario(@PathVariable Long idUsuario) {
        String msj = "";
        Usuario u = uR.findById(idUsuario).orElseThrow();
        if (u.isEsAdmin() && !u.getNombre().equals("Cristian")) {
            msj = "❌ No se puede borrar al admin.";
        } else {
            jR.findAll().stream().filter(j -> j.getPropietario() != null && j.getPropietario().getId().equals(idUsuario)).forEach(j -> {
                j.setPropietario(null);
                j.setClausula(j.getValor());
                jR.save(j);
            });

            er.deleteAll(er.findByUsuario(u));

            List<Oferta> ofertasRelacionadas = oR.findAll().stream().filter(o -> o.getVendedor().getId().equals(idUsuario) || o.getComprador().getId().equals(idUsuario)).collect(Collectors.toList());
            oR.deleteAll(ofertasRelacionadas);
            uR.delete(u);

            msj = "✅ Usuario eliminado correctamente.";
        }
        return msj;
    }
}