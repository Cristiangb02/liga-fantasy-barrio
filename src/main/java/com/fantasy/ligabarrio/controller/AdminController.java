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
        boolean resultado = fS.getJornadaActiva().isBloqueada();
        return resultado;
    }

    @GetMapping("/usuarios-gestion")
    public List<Usuario> getUsuariosGestion() {
        List<Usuario> resultado = uR.findAll();
        return resultado;
    }

    @GetMapping("/pendientes")
    public List<Usuario> verUsuariosPendientes() {
        List<Usuario> resultado = new ArrayList<>();
        List<Usuario> todos = uR.findAll();

        for (Usuario u : todos) {
            if (!u.isActivo()) {
                resultado.add(u);
            }
        }

        return resultado;
    }

    @GetMapping("/jugadores-puntuados")
    public List<Jugador> getJugadoresPuntuados() {
        List<Jugador> resultado = new ArrayList<>();
        Jornada actual = fS.getJornadaActiva();

        List<Actuacion> todasActuaciones = aR.findAll();

        for (Actuacion a : todasActuaciones) {
            if (a.getJornada().getId().equals(actual.getId())) {
                resultado.add(a.getJugador());
            }
        }

        // Ordenamos
        resultado.sort((j1, j2) -> {
            int p1 = fS.getPesoPosicion(j1.getPosicion());
            int p2 = fS.getPesoPosicion(j2.getPosicion());

            if (p1 != p2) {
                return Integer.compare(p1, p2);
            } else {
                return j1.getNombre().compareToIgnoreCase(j2.getNombre());
            }
        });

        return resultado;
    }

    @GetMapping("/jugadores-pendientes")
    public List<Jugador> getJugadoresPendientes() {
        List<Jugador> resultado = new ArrayList<>();
        Jornada actual = fS.getJornadaActiva();
        List<Jugador> todos = jR.findAll();
        List<Actuacion> todasActuaciones = aR.findAll();
        List<Long> idsPuntuados = new ArrayList<>();

        for (Actuacion a : todasActuaciones) {
            if (a.getJornada().getId().equals(actual.getId())) {
                idsPuntuados.add(a.getJugador().getId());
            }
        }

        for (Jugador j : todos) {
            if (!idsPuntuados.contains(j.getId())) {
                resultado.add(j);
            }
        }

        resultado.sort(Comparator.comparing(Jugador::getNombre));

        return resultado;
    }

    @GetMapping("/limpiar-clones/{numJornada}")
    public String limpiarClonesJornada(@PathVariable int numJornada) {
        String msj;

        Jornada jornada = null;
        List<Jornada> todasLasJornadas = joR.findAll();
        for (int i = 0; i < todasLasJornadas.size() && jornada == null; i++) {
            if (todasLasJornadas.get(i).getNumero() == numJornada) {
                jornada = todasLasJornadas.get(i);
            }
        }

        if (jornada == null) {
            msj = "❌ Error: Jornada no encontrada.";
        } else {
            List<Actuacion> todas = new ArrayList<>();
            List<Actuacion> actuacionesBD = aR.findAll();

            for (Actuacion a : actuacionesBD) {
                if (a.getJornada().getId().equals(jornada.getId())) {
                    todas.add(a);
                }
            }

            // 5. Agrupamos por el ID de jugador
            Map<Long, List<Actuacion>> porJugador = new HashMap<>();
            for (Actuacion a : todas) {
                Long idJugador = a.getJugador().getId();

                if (!porJugador.containsKey(idJugador)) {
                    porJugador.put(idJugador, new ArrayList<>());
                }
                porJugador.get(idJugador).add(a);
            }

            int borrados = 0;

            for (Map.Entry<Long, List<Actuacion>> entrada : porJugador.entrySet()) {
                List<Actuacion> lista = entrada.getValue();

                if (lista.size() > 1) {

                    lista.sort((a, b) -> {
                        if (a.getColorEquipo() != null && b.getColorEquipo() == null) {
                            return -1;
                        } else if (b.getColorEquipo() != null && a.getColorEquipo() == null) {
                            return 1;
                        } else {
                            return b.getId().compareTo(a.getId());
                        }
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
            msj = "Se han eliminado " + borrados + " clones.";
        }
        return msj;
    }

    //POST-MAPPING
    @PostMapping("/toggle-bloqueo")
    public String toggleBloqueo() {
        String resultado;
        Jornada actual = fS.getJornadaActiva();

        if (actual.isBloqueada()) {
            actual.setBloqueada(false);
            actual.setDiaBloqueo(null);
            resultado = "Bloqueo de acciones DESACTIVADO 🔓";
        } else {
            actual.setBloqueada(true);
            actual.setDiaBloqueo(java.time.LocalDate.now(java.time.ZoneId.of("Europe/Madrid")));
            resultado = "Bloqueo de acciones ACTIVADO 🔒";
        }
        joR.save(actual);
        return resultado;
    }

    @PostMapping("/toggle-mantenimiento")
    public String toggleMantenimiento() {
        String resultado;
        boolean estadoActual = fS.isMantenimientoActivo();
        fS.setMantenimientoActivo(!estadoActual);

        if (!estadoActual) {
            resultado = "Mantenimiento ACTIVO";
        } else {
            resultado = "Mantenimiento DESACTIVADO";
        }
        return resultado;
    }

    @PostMapping("/cerrar-jornada")
    public String cerrarJornada() {
        String msj;
        Jornada actual = fS.getJornadaActiva();
        List<Equipo> equipos = er.findByJornada(actual);
        StringBuilder res = new StringBuilder();

        for (Equipo e : equipos) {
            Usuario u = e.getUsuario();

            if (u.getPresupuesto() < 0) {
                e.setPuntosTotalesJornada(0);
                er.save(e);
                res.append("🚫 ").append(u.getNombre()).append(" (Saldo Negativo)\n");

            } else {
                int total = 0;

                for (Jugador j : e.getJugadoresAlineados()) {
                    Optional<Actuacion> actuacionOpt = aR.findByJugadorAndJornada(j, actual);
                    if (actuacionOpt.isPresent()) {
                        total = total + actuacionOpt.get().getPuntosTotales();
                    }
                }

                e.setPuntosTotalesJornada(total);
                er.save(e);
                res.append("✅ ").append(u.getNombre()).append(": ").append(total).append("p\n");
            }
        }

        Jornada nueva = new Jornada();
        nueva.setNumero(actual.getNumero() + 1);
        nueva.setBloqueada(actual.isBloqueada());
        nueva.setDiaBloqueo(actual.getDiaBloqueo());
        joR.save(nueva);

        nR.save(new Noticia("🏁 JORNADA " + actual.getNumero() + " FINALIZADA con éxito."));
        msj = "✅ Jornada terminada.";
        return msj;
    }

    @PostMapping("/aprobar/{idUsuario}")
    public String aprobarUsuario(@PathVariable Long idUsuario) {
        String msj;
        Usuario u = uR.findById(idUsuario).orElseThrow();
        u.setActivo(true);
        uR.save(u);
        nR.save(new Noticia("👋 BIENVENIDA: " + u.getNombre() + " ha entrado a la liga."));
        msj = "✅ Usuario aprobado.";
        return msj;
    }

    @PostMapping("/editar-usuario/{idUsuario}")
    public String editarUsuario(@PathVariable Long idUsuario, @RequestBody Map<String, String> datos) {
        String nuevoNombre = datos.get("nombre");
        String msj;
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

    @PostMapping("/modificar-saldo/{idUsuario}/{cantidad}")
    public String modificarSaldo(@PathVariable Long idUsuario, @PathVariable int cantidad) {
        String msj;

        if (cantidad == 0) {
            msj = "❌ La cantidad no puede ser cero.";
        } else {
            String accion;
            if (cantidad > 0) {
                accion = "ingresado";
            } else {
                accion = "retirado";
            }

            if (idUsuario == 0L) {
                List<Usuario> todosLosUsuarios = uR.findAll();
                List<Usuario> usuariosModificados = new ArrayList<>();

                for (Usuario u : todosLosUsuarios) {
                    if (u.isActivo()) {
                        u.setPresupuesto(u.getPresupuesto() + cantidad);
                        usuariosModificados.add(u);
                    }
                }
                uR.saveAll(usuariosModificados);
                msj = "✅ Se han " + accion + " " + fS.formatearDinero(Math.abs(cantidad)) + " a todos los mánagers.";

            } else {
                Usuario u = uR.findById(idUsuario).orElseThrow();
                u.setPresupuesto(u.getPresupuesto() + cantidad);
                uR.save(u);
                msj = "✅ Se han " + accion + " " + fS.formatearDinero(Math.abs(cantidad)) + " a " + u.getNombre() + ".";
            }
        }
        return msj;
    }

    @PostMapping("/modificar-puntos-extra/{idUsuario}/{puntos}")
    public String modificarPuntosExtra(@PathVariable Long idUsuario, @PathVariable int puntos) {
        String msj;
        Usuario u = uR.findById(idUsuario).orElseThrow();
        u.setPuntosExtra(u.getPuntosExtra() + puntos);
        uR.save(u);

        String accion;
        if (puntos >= 0) {
            accion = "añadido";
        } else {
            accion = "restado";
        }
        msj = "✅ Se han " + accion + " " + Math.abs(puntos) + " puntos a " + u.getNombre() + " en la clasificación general.";
        return msj;
    }

    @PostMapping("/actualizar-avatar/{idUsuario}")
    public String actualizarAvatarUsuario(@PathVariable Long idUsuario, @RequestBody Map<String, String> datos) {
        String msj;
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

    @PostMapping("/reset-liga")
    public String resetearLiga() {
        String msj;
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
        String msj;
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
        String msj;
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

            List<Equipo> todosEquipos = er.findAll();
            for (Equipo equipo : todosEquipos) {
                if (equipo.getJugadoresAlineados().contains(j)) {
                    equipo.getJugadoresAlineados().remove(j);
                    er.save(equipo);
                }
            }

            jR.delete(j);
            msj = "✅ El jugador " + j.getNombre() + "(" + j.getPosicion() + ") ha sido eliminado.";
        }
        return msj;
    }

    @PostMapping("/reset-puntos-jornada/{idJugador}/{numJornada}")
    public String resetPuntosJornada(@PathVariable Long idJugador, @PathVariable int numJornada) {
        String msj;
        Jugador jug = jR.findById(idJugador).orElseThrow();
        Jornada jornada = joR.findAll().stream().filter(j -> j.getNumero() == numJornada).findFirst().orElseThrow();

        List<Actuacion> actas = aR.findAll().stream()
                .filter(a -> a.getJugador().getId().equals(idJugador) && a.getJornada().getId().equals(jornada.getId()))
                .collect(Collectors.toList());

        if (actas.isEmpty())  {
            msj = "❌ Este jugador no tiene puntos registrados en la jornada " + numJornada + ".";
        } else {
            int clonesBorrados = 0;
            for (Actuacion acta : actas) {
                int puntosRestar = acta.getPuntosTotales();
                int valorRestar = puntosRestar * 100_000;

                jug.setPuntosAcumulados(jug.getPuntosAcumulados() - puntosRestar);
                jug.setValor(jug.getValor() - valorRestar);
                jug.setClausula(jug.getClausula() - valorRestar);

                aR.delete(acta);
                clonesBorrados++;
            }
            jR.save(jug);
            msj = "✅ Se eliminaron " + clonesBorrados + " registros de " + jug.getNombre() + "(" + jug.getPosicion() + ") en la jornada " + numJornada + ".";
        }
        return msj;
    }

    @PostMapping("/add-puntos-jornada/{idJugador}/{numJornada}/{puntos}/{color}")
    public String addPuntosJornada(@PathVariable Long idJugador, @PathVariable int numJornada, @PathVariable int puntos, @PathVariable String color) {
        String msj;
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


    @PostMapping("/cambiar-estado/{idJugador}/{nuevoEstado}")
    public String cambiarEstadoJugador(@PathVariable Long idJugador, @PathVariable String nuevoEstado) {
        String msj;
        Jugador j = jR.findById(idJugador).orElseThrow();

        String estadoLimpio = nuevoEstado.replace("-", " ");
        j.setEstado(estadoLimpio);
        jR.save(j);

        msj = "✅ Estado de " + j.getNombre() + " cambiado a " + estadoLimpio;
        return msj;
    }

    @PostMapping("/actualizar-imagen/{idJugador}")
    public String actualizarImagen(@PathVariable Long idJugador, @RequestBody Map<String, String> datos) {
        String msj;
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

    //DELETE-MAPPING
    @DeleteMapping("/rechazar/{idUsuario}")
    public String rechazarUsuario(@PathVariable Long idUsuario) {
        String msj;
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