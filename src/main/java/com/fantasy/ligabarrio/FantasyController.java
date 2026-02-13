package com.fantasy.ligabarrio;

import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.EnableScheduling;

@RestController
@CrossOrigin(origins = "*")
@EnableScheduling
public class FantasyController {

    private final EquipoRepository equipoRepository;
    private final JugadorRepository jugadorRepository;
    private final UsuarioRepository usuarioRepository;
    private final JornadaRepository jornadaRepository;
    private final ActuacionRepository actuacionRepository;
    private final NoticiaRepository noticiaRepository;
    private final OfertaRepository ofertaRepository;
    private final CalculadoraPuntosService calculadora;
    private final Map<Long, LocalDateTime> usuariosOnline = new ConcurrentHashMap<>();

    private static long desplazamiento = 0;

    public FantasyController(EquipoRepository equipoRepository, JugadorRepository jugadorRepository,
                             UsuarioRepository usuarioRepository, JornadaRepository jornadaRepository,
                             ActuacionRepository actuacionRepository, NoticiaRepository noticiaRepository,
                             OfertaRepository ofertaRepository, CalculadoraPuntosService calculadora) {
        this.equipoRepository = equipoRepository;
        this.jugadorRepository = jugadorRepository;
        this.usuarioRepository = usuarioRepository;
        this.jornadaRepository = jornadaRepository;
        this.actuacionRepository = actuacionRepository;
        this.noticiaRepository = noticiaRepository;
        this.ofertaRepository = ofertaRepository;
        this.calculadora = calculadora;
    }

    // BLOQUEO DE ACCIONES (SOLUCIÓN PERSISTENTE)

    @Scheduled(cron = "0 0 10 * * *", zone = "Europe/Madrid")
    public void desbloqueoAutomatico() {
        Jornada actual = getJornadaActiva();
        if (actual.isBloqueada()) {
            actual.setBloqueada(false);
            jornadaRepository.save(actual);
            System.out.println("⏰ Bloqueo desactivado automáticamente en BBDD a las 10:00");
        }
    }

    @PostMapping("/admin/toggle-bloqueo")
    public String toggleBloqueo() {
        Jornada actual = getJornadaActiva();

        // Invertir estado
        if (actual.isBloqueada()) {
            actual.setBloqueada(false);
        } else {
            actual.setBloqueada(true);
        }

        jornadaRepository.save(actual);

        String estado;
        if (actual.isBloqueada()) {
            estado = "ACTIVADO 🔒";
        } else {
            estado = "DESACTIVADO 🔓";
        }

        return "Bloqueo de acciones " + estado;
    }

    // - - - - - ENDPOINTS - - - - -

    //*********************************************************************************************************************
    //                                                          GET
    //*********************************************************************************************************************

    @GetMapping("/admin/estado-bloqueo")
    public boolean getEstadoBloqueo() {
        return getJornadaActiva().isBloqueada();
    }

    @GetMapping("/admin/usuarios-gestion")
    public List<Usuario> getUsuariosGestion() {
        return usuarioRepository.findAll();
    }

    @GetMapping("/admin/pendientes")
    public List<Usuario> verUsuariosPendientes() {
        return usuarioRepository.findAll().stream().filter(u -> !u.isActivo()).collect(Collectors.toList());
    }

    @GetMapping("/admin/jugadores-puntuados")
    public List<Jugador> getJugadoresPuntuados() {
        Jornada actual = getJornadaActiva();
        return jugadorRepository.findAll().stream()
                .filter(j -> actuacionRepository.findByJugadorAndJornada(j, actual).isPresent())
                .sorted((j1, j2) -> {
                    int p1 = getPesoPosicion(j1.getPosicion());
                    int p2 = getPesoPosicion(j2.getPosicion());

                    if (p1 != p2) {
                        return Integer.compare(p1, p2);
                    } else {
                        return j1.getNombre().compareToIgnoreCase(j2.getNombre());
                    }
                }).collect(Collectors.toList());
    }

    @GetMapping("/admin/jugadores-pendientes")
    public List<Jugador> getJugadoresPendientes() {
        Jornada actual = getJornadaActiva();
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

    @GetMapping("/jornada/actual")
    public long getNumeroJornadaActualEndpoint() {
        return getNumeroJornadaReal();
    }

    @GetMapping("/usuarios")
    public List<Usuario> verRivales() {
        return usuarioRepository.findAll().stream().filter(Usuario::isActivo).collect(Collectors.toList());
    }

    @GetMapping("/jugadores")
    public List<Map<String, Object>> verTodosLosJugadores() {
        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("Europe/Madrid"));
        return jugadorRepository.findAll().stream().map(j -> {
            boolean blindado = false;
            if (j.getFechaFinBlindaje() != null && j.getFechaFinBlindaje().isAfter(ahora)) {
                blindado = true;
            }

            long segundosRestantes;
            if (blindado) {
                segundosRestantes = ChronoUnit.SECONDS.between(ahora, j.getFechaFinBlindaje());
            } else {
                segundosRestantes = 0;
            }

            String urlImagen;
            if (j.getUrlImagen() != null) {
                urlImagen = j.getUrlImagen();
            } else {
                urlImagen = "";
            }

            Object propietarioObj;
            if (j.getPropietario() != null) {
                propietarioObj = j.getPropietario();
            } else {
                propietarioObj = Map.of();
            }

            return Map.<String, Object>of(
                    "id", j.getId(),
                    "nombre", j.getNombre(),
                    "posicion", j.getPosicion(),
                    "valor", j.getValor(),
                    "clausula", j.getClausula(),
                    "puntosAcumulados", j.getPuntosAcumulados(),
                    "urlImagen", urlImagen,
                    "propietario", propietarioObj,
                    "blindado", blindado,
                    "segundosBlindaje", segundosRestantes
            );
        }).collect(Collectors.toList());
    }

    @GetMapping("/mercado-diario")
    public List<Jugador> getMercadoDiario() {
        LocalDate hoy = LocalDate.now(ZoneId.of("Europe/Madrid"));
        long seed = hoy.toEpochDay() + desplazamiento;
        List<Jugador> todos = jugadorRepository.findAll();
        List<Jugador> libresAnoche = new ArrayList<>();

        for (Jugador j : todos) {
            if (j.getPropietario() == null) {
                if (j.getFechaVenta() == null || !j.getFechaVenta().isEqual(hoy)) {
                    libresAnoche.add(j);
                }
            } else {
                if (j.getFechaFichaje() != null && j.getFechaFichaje().isEqual(hoy)) {
                    libresAnoche.add(j);
                }
            }
        }
        libresAnoche.sort(Comparator.comparing(Jugador::getId));
        Collections.shuffle(libresAnoche, new Random(seed));
        List<Jugador> mercadoBase = libresAnoche.stream().limit(14).collect(Collectors.toList());

        List<Jugador> mercadoReal = new ArrayList<>();

        for (Jugador j : mercadoBase) {
            if (j.getPropietario() == null) {
                mercadoReal.add(j);
            }
        }

        for (Jugador j : todos) {
            if (j.getPropietario() == null && j.getFechaVenta() != null && j.getFechaVenta().isEqual(hoy)) {
                if (!mercadoReal.contains(j)) {
                    mercadoReal.add(j);
                }
            }
        }
        return mercadoReal;
    }

    @GetMapping("/jornada/resumen")
    public List<Map<String, Object>> verResumenJornadaAnterior() {
        Jornada actual = getJornadaActiva();
        int numAnterior = actual.getNumero() - 1;
        if (numAnterior < 1) {
            return new ArrayList<>();
        }

        Optional<Jornada> jOpt = jornadaRepository.findAll().stream().filter(j -> j.getNumero() == numAnterior).findFirst();
        if (jOpt.isEmpty()) {
            return new ArrayList<>();
        }

        Jornada anterior = jOpt.get();
        List<Equipo> equipos = equipoRepository.findByJornada(anterior);
        return equipos.stream().map(e -> {
            List<Map<String, Object>> jugadores = new ArrayList<>();
            for (Jugador j : e.getJugadoresAlineados()) {
                int pts = actuacionRepository.findByJugadorAndJornada(j, anterior).map(Actuacion::getPuntosTotales).orElse(0);

                String urlImagen;
                if (j.getUrlImagen() != null) {
                    urlImagen = j.getUrlImagen();
                } else {
                    urlImagen = "";
                }

                jugadores.add(Map.of(
                        "nombre", j.getNombre(),
                        "posicion", j.getPosicion(),
                        "puntos", pts,
                        "urlImagen", urlImagen
                ));
            }
            jugadores.sort((a,b) -> Integer.compare(getPesoPosicion((String)a.get("posicion")), getPesoPosicion((String)b.get("posicion"))));
            return Map.<String, Object>of(
                    "manager", e.getUsuario().getNombre(),
                    "puntosTotal", e.getPuntosTotalesJornada(),
                    "jugadores", jugadores
            );
        }).sorted((a,b) -> Integer.compare((int)b.get("puntosTotal"), (int)a.get("puntosTotal"))).collect(Collectors.toList());
    }

    @GetMapping("/jornada/{numero}/resumen-partido")
    public Map<String, Object> getResumenPartido(@PathVariable int numero) {
        Optional<Jornada> jOpt = jornadaRepository.findAll().stream().filter(j -> j.getNumero() == numero).findFirst();
        if (jOpt.isEmpty()) {
            return Map.of("error", "Jornada no encontrada");
        }

        Jornada jornada = jOpt.get();
        List<Actuacion> actuaciones = actuacionRepository.findAll().stream().filter(a -> a.getJornada().getId().equals(jornada.getId())).collect(Collectors.toList());
        if (actuaciones.isEmpty()) {
            return Map.of("error", "Sin datos en esta jornada");
        }

        int maxPuntos = actuaciones.stream().mapToInt(Actuacion::getPuntosTotales).max().orElse(0);
        Map<String, List<Actuacion>> grupos = actuaciones.stream().filter(a -> a.getEquipoColor() != null).collect(Collectors.groupingBy(Actuacion::getEquipoColor));
        List<String> colores = new ArrayList<>(grupos.keySet());

        String colorA;
        if (grupos.isEmpty() || colores.isEmpty()) {
            colorA = "BLANCO";
        } else {
            colorA = colores.get(0);
        }

        String colorB;
        if (colores.size() > 1) {
            colorB = colores.get(1);
        } else {
            colorB = "RIVAL";
        }

        List<Map<String, Object>> equipoA = mapJugadoresCampo(grupos.getOrDefault(colorA, List.of()), maxPuntos);
        List<Map<String, Object>> equipoB = mapJugadoresCampo(grupos.getOrDefault(colorB, List.of()), maxPuntos);

        return Map.of("colorA", colorA, "colorB", colorB, "equipoA", equipoA, "equipoB", equipoB);
    }

    @GetMapping("/jornada/{numero}/resumen-managers")
    public List<Map<String, Object>> verResumenManagers(@PathVariable int numero) {
        Optional<Jornada> jOpt = jornadaRepository.findAll().stream().filter(j -> j.getNumero() == numero).findFirst();
        if (jOpt.isEmpty()) {
            return new ArrayList<>();
        }

        Jornada jornada = jOpt.get();
        List<Equipo> equipos = equipoRepository.findByJornada(jornada);
        return equipos.stream().map(e -> {
            List<Map<String, Object>> jugadores = new ArrayList<>();
            for (Jugador j : e.getJugadoresAlineados()) {
                int pts = actuacionRepository.findByJugadorAndJornada(j, jornada).map(Actuacion::getPuntosTotales).orElse(0);
                jugadores.add(Map.of("nombre", j.getNombre(), "posicion", j.getPosicion(), "puntos", pts));
            }
            jugadores.sort((a,b) -> Integer.compare(getPesoPosicion((String)a.get("posicion")), getPesoPosicion((String)b.get("posicion"))));
            return Map.<String, Object>of("manager", e.getUsuario().getNombre(), "puntosTotal", e.getPuntosTotalesJornada(), "jugadores", jugadores);
        }).sorted((a,b) -> Integer.compare((int)b.get("puntosTotal"), (int)a.get("puntosTotal"))).collect(Collectors.toList());
    }

    @GetMapping("/ofertas/mis-ofertas/{idUsuario}")
    public Map<String, List<Map<String, Object>>> verMisOfertas(@PathVariable Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow();
        List<Map<String, Object>> recibidas = ofertaRepository.findByVendedorAndEstado(usuario, "PENDIENTE").stream()
                .map(o -> Map.<String, Object>of(
                        "id", o.getId(), "jugador", o.getJugador().getNombre(), "comprador", o.getComprador().getNombre(),
                        "cantidad", o.getCantidad(), "cantidadFmt", fmtDinero(o.getCantidad())
                )).collect(Collectors.toList());
        List<Map<String, Object>> enviadas = ofertaRepository.findByCompradorAndEstado(usuario, "PENDIENTE").stream()
                .map(o -> Map.<String, Object>of(
                        "id", o.getId(), "jugador", o.getJugador().getNombre(), "vendedor", o.getVendedor().getNombre(),
                        "cantidad", o.getCantidad(), "cantidadFmt", fmtDinero(o.getCantidad())
                )).collect(Collectors.toList());
        return Map.of("recibidas", recibidas, "enviadas", enviadas);
    }

    @GetMapping("/alineacion/{usuarioId}")
    public List<Jugador> getAlineacion(@PathVariable Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        Jornada jornadaActual = getJornadaActiva();
        Optional<Equipo> equipo = equipoRepository.findByUsuario(usuario).stream().filter(e -> e.getJornada().getId().equals(jornadaActual.getId())).findFirst();

        if (equipo.isPresent()) {
            return equipo.get().getJugadoresAlineados();
        } else {
            return List.of();
        }
    }

    @GetMapping("/noticias")
    public List<Noticia> verNoticias() {
        LocalDateTime limite = LocalDateTime.now(ZoneId.of("Europe/Madrid")).minusDays(7);
        List<Noticia> todas = noticiaRepository.findAll();
        List<Noticia> paraBorrar = todas.stream().filter(n -> n.getFecha().isBefore(limite)).collect(Collectors.toList());
        if(!paraBorrar.isEmpty()) {
            noticiaRepository.deleteAll(paraBorrar);
        }
        return noticiaRepository.findAllByOrderByFechaDesc();
    }

    @GetMapping("/historial/{usuarioId}")
    public List<Map<String, Object>> getHistorialUsuario(@PathVariable Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        return equipoRepository.findByUsuario(usuario).stream()
                .sorted((e1, e2) -> Integer.compare(e2.getJornada().getNumero(), e1.getJornada().getNumero()))
                .map(e -> {
                    List<Map<String, Object>> detalles = new ArrayList<>();
                    for (Jugador j : e.getJugadoresAlineados()) {
                        int p = actuacionRepository.findByJugadorAndJornada(j, e.getJornada()).map(Actuacion::getPuntosTotales).orElse(0);
                        detalles.add(Map.of("nombre", j.getNombre(), "posicion", j.getPosicion(), "puntos", p));
                    }
                    return Map.<String, Object>of("jornadaNumero", e.getJornada().getNumero(), "puntosTotal", e.getPuntosTotalesJornada(), "jugadores", detalles);
                }).collect(Collectors.toList());
    }

    @GetMapping("/jugador/{id}/historial-puntos")
    public List<Map<String, Object>> getHistorialPuntosJugador(@PathVariable Long id) {
        Jugador jugador = jugadorRepository.findById(id).orElseThrow();
        return actuacionRepository.findByJugador(jugador).stream().map(a -> Map.<String, Object>of("jornada", a.getJornada().getNumero(), "puntos", a.getPuntosTotales())).sorted((m1, m2) -> Integer.compare((Integer)m1.get("jornada"), (Integer)m2.get("jornada"))).collect(Collectors.toList());
    }

    @GetMapping("/premios-pendientes/{idUsuario}")
    public List<Map<String, Object>> verPremiosPendientes(@PathVariable Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow();
        Jornada jornadaActual = getJornadaActiva();

        return equipoRepository.findByUsuario(usuario).stream().filter(e -> !e.getJornada().getId().equals(jornadaActual.getId()) && !e.isReclamado()).map(e -> {
            int p = e.getPuntosTotalesJornada();

            int dinero;
            if (p > 0) {
                dinero = p * 100_000;
            } else {
                dinero = 0;
            }

            int maxPuntos = actuacionRepository.findAll().stream().filter(a -> a.getJornada().getId().equals(e.getJornada().getId())).mapToInt(Actuacion::getPuntosTotales).max().orElse(0);
            boolean mvp = false;
            String nombreMvp = "";
            if (maxPuntos > 0) {
                for (Jugador j : e.getJugadoresAlineados()) {
                    Optional<Actuacion> act = actuacionRepository.findByJugadorAndJornada(j, e.getJornada());
                    if (act.isPresent() && act.get().getPuntosTotales() == maxPuntos) {
                        mvp = true;
                        nombreMvp = j.getNombre();
                        break;
                    }
                }
            }

            int bonus;
            if (mvp) {
                bonus = 1_000_000;
            } else {
                bonus = 0;
            }

            return Map.<String, Object>of("idEquipo", e.getId(), "jornada", e.getJornada().getNumero(), "puntos", p, "dineroFmt", fmtDinero(dinero + bonus), "tieneMvp", mvp, "nombreMvp", nombreMvp, "bonusFmt", fmtDinero(bonus));
        }).collect(Collectors.toList());
    }

    @GetMapping("/clasificacion")
    public List<Map<String, Object>> verClasificacion() {
        List<Usuario> usuarios = usuarioRepository.findAll().stream().filter(Usuario::isActivo).collect(Collectors.toList());
        List<Jugador> todosJugadores = jugadorRepository.findAll();
        List<Equipo> todosEquipos = equipoRepository.findAll();

        return usuarios.stream().map(u -> {
            int p = todosEquipos.stream().filter(e -> e.getUsuario().getId().equals(u.getId())).mapToInt(Equipo::getPuntosTotalesJornada).sum();
            int v = todosJugadores.stream().filter(j -> j.getPropietario() != null && j.getPropietario().getId().equals(u.getId())).mapToInt(Jugador::getValor).sum();
            return Map.<String, Object>of("nombre", u.getNombre(), "puntos", p, "valorPlantilla", v);
        }).sorted((m1, m2) -> {
            int cmp = Integer.compare((int)m2.get("puntos"), (int)m1.get("puntos"));
            if (cmp != 0) {
                return cmp;
            } else {
                return Integer.compare((int)m2.get("valorPlantilla"), (int)m1.get("valorPlantilla"));
            }
        }).collect(Collectors.toList());
    }

    //*********************************************************************************************************************
    //                                                          POST
    //*********************************************************************************************************************

    @PostMapping("/admin/aprobar/{idUsuario}")
    public String aprobarUsuario(@PathVariable Long idUsuario) {
        Usuario u = usuarioRepository.findById(idUsuario).orElseThrow();
        u.setActivo(true);
        usuarioRepository.save(u);
        noticiaRepository.save(new Noticia("👋 BIENVENIDA: " + u.getNombre() + " ha entrado a la liga."));
        return "✅ Usuario aprobado.";
    }

    @PostMapping("/admin/editar-usuario/{idUsuario}")
    public String editarUsuario(@PathVariable Long idUsuario, @RequestBody Map<String, String> datos) {
        String nuevoNombre = datos.get("nombre");
        if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) {
            return "❌ El nombre no puede estar vacío.";
        }

        Usuario existente = usuarioRepository.findByNombre(nuevoNombre);
        if (existente != null && !existente.getId().equals(idUsuario)) {
            return "❌ Ese nombre ya está en uso por otro jugador.";
        }

        Usuario u = usuarioRepository.findById(idUsuario).orElseThrow();
        String antiguo = u.getNombre();
        u.setNombre(nuevoNombre);
        usuarioRepository.save(u);
        return "✅ Se ha cambiado el nombre '" + antiguo + "' a '" + nuevoNombre + "'.";
    }

    @PostMapping("/admin/registrar")
    public String registrarPartido(@RequestBody DatosPartido datos) {
        Jugador jugador = jugadorRepository.findById(datos.idJugador).orElseThrow();
        Jornada jornada = getJornadaActiva();
        Actuacion actuacion = actuacionRepository.findByJugadorAndJornada(jugador, jornada).orElse(new Actuacion(jugador, jornada));

        actuacion.setJugado(datos.jugado);
        actuacion.setVictoria(datos.victoria);
        actuacion.setDerrota(datos.derrota);
        actuacion.setGolesMarcados(datos.goles);
        actuacion.setGolesEncajados(datos.golesEncajados);
        actuacion.setAutogoles(datos.autogoles);
        actuacion.setEquipoColor(datos.equipoColor);

        int puntos = calculadora.calcularPuntos(actuacion);
        actuacion.setPuntosTotales(puntos);
        actuacionRepository.save(actuacion);

        jugador.setPuntosAcumulados(jugador.getPuntosAcumulados() + puntos);
        int nuevoValor = jugador.getValor() + (puntos * 100_000);
        if (nuevoValor < 150_000) {
            nuevoValor = 150_000;
        }
        jugador.setValor(nuevoValor);

        if (jugador.getPropietario() != null) {
            if (jugador.getClausula() < jugador.getValor()) {
                jugador.setClausula(jugador.getValor());
            }
        }
        jugadorRepository.save(jugador);
        return "✅ Puntos de " + jugador.getNombre() + " en esta jornada: " + puntos;
    }

    @PostMapping("/admin/cerrar-jornada")
    public String cerrarJornada() {
        Jornada actual = getJornadaActiva();
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

    @PostMapping("/admin/reset-mercado")
    public String resetMercado() {
        desplazamiento++;
        return "✅ Mercado renovado con éxito.";
    }

    @PostMapping("/admin/reset-liga")
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

    @PostMapping("/admin/reset-puntos/{idJugador}")
    public String resetearPuntosJugador(@PathVariable Long idJugador) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Jornada jornada = getJornadaActiva();
        Optional<Actuacion> actaOpt = actuacionRepository.findByJugadorAndJornada(jugador, jornada);

        if (actaOpt.isEmpty()) {
            return "❌ Este jugador no tiene puntos registrados en esta jornada.";
        }

        Actuacion acta = actaOpt.get();
        int puntosRestar = acta.getPuntosTotales();
        int valorRestar = puntosRestar * 100_000;

        jugador.setPuntosAcumulados(jugador.getPuntosAcumulados() - puntosRestar);
        jugador.setValor(jugador.getValor() - valorRestar);
        jugador.setClausula(jugador.getClausula() - valorRestar);

        actuacionRepository.delete(acta);
        jugadorRepository.save(jugador);
        return "✅ CORREGIDO: Puntos de " + jugador.getNombre() + " reseteados. (Restados " + puntosRestar + " pts y " + fmtDinero(valorRestar) + " valor)";
    }

    @PostMapping("/admin/eliminar-jugador/{id}")
    public String eliminarJugador(@PathVariable Long id) {
        Optional<Jugador> jOpt = jugadorRepository.findById(id);
        if (jOpt.isEmpty()) {
            return "❌ Error: El jugador no existe.";
        }

        Jugador j = jOpt.get();
        String nombre = j.getNombre();
        if(j.getPropietario() != null) {
            j.setPropietario(null);
            jugadorRepository.save(j);
        }

        List<Actuacion> actuaciones = actuacionRepository.findByJugador(j);
        actuacionRepository.deleteAll(actuaciones);
        List<Oferta> ofertas = ofertaRepository.findByJugador(j);
        ofertaRepository.deleteAll(ofertas);
        jugadorRepository.delete(j);
        return "✅ Jugador eliminado: " + nombre;
    }

    @PostMapping("/auth/registro")
    public String registrarUsuario(@RequestBody Usuario datos) {
        if (usuarioRepository.findByNombre(datos.getNombre()) != null) {
            return "❌ El nombre ya existe.";
        }

        boolean esPrimero = usuarioRepository.count() == 0;
        Usuario nuevo = new Usuario(datos.getNombre(), datos.getPassword(), 100_000_000, esPrimero);
        nuevo.setActivo(esPrimero);
        usuarioRepository.save(nuevo);

        if (esPrimero) {
            noticiaRepository.save(new Noticia("👑 FUNDADOR: " + datos.getNombre() + " ha inaugurado la liga como Admin."));
            return "✅ ¡Liga inaugurada! Eres el Admin.";
        } else {
            return "✅ Solicitud enviada. Contacta con el creador de la app por Whatsapp para que te acepte y luego pulsa el botón 'Entrar'.";
        }
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Usuario datos) {
        Usuario user = usuarioRepository.findByNombre(datos.getNombre());
        if (user == null || !user.getPassword().equals(datos.getPassword())) {
            return Map.of("error", "Credenciales incorrectas.");
        }
        if (!user.isActivo()) {
            return Map.of("error", "⛔ Tu cuenta aún no ha sido aprobada por el Admin.");
        }
        return Map.of("id", user.getId(), "nombre", user.getNombre(), "esAdmin", user.isEsAdmin(), "presupuesto", user.getPresupuesto());
    }

    @PostMapping("/usuarios/ping/{idUsuario}")
    public List<String> pingUsuario(@PathVariable Long idUsuario) {
        usuariosOnline.put(idUsuario, LocalDateTime.now());
        LocalDateTime limite = LocalDateTime.now().minusMinutes(5);
        List<String> nombresOnline = new ArrayList<>();
        usuariosOnline.forEach((id, fecha) -> {
            if (fecha.isAfter(limite)) {
                usuarioRepository.findById(id).ifPresent(u -> nombresOnline.add(u.getNombre()));
            }
        });
        return nombresOnline;
    }

    @PostMapping("/mercado/comprar/{idJugador}/{idUsuario}")
    public String comprarJugadorLibre(@PathVariable Long idJugador, @PathVariable Long idUsuario) {

        // NUEVO BLOQUEO: Consultamos BBDD
        if (getJornadaActiva().isBloqueada()) {
            return "⛔ No puedes hacer compras, ventas o clausulazos desde que se conoce la alineación hasta el día siguiente.";
        }
        if (isMercadoCerrado()) {
            return "⛔ MERCADO CERRADO EN ESTOS MOMENTOS ⛔";
        }

        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario comprador = usuarioRepository.findById(idUsuario).orElseThrow();
        if (jugador.getPropietario() != null) {
            return "❌ Error: Este jugador ya ha sido comprado.";
        }

        comprador.setPresupuesto(comprador.getPresupuesto() - jugador.getValor());
        jugador.setPropietario(comprador);
        jugador.setClausula(jugador.getValor());
        jugador.setJornadaFichaje(getJornadaActiva().getId());
        jugador.setFechaFichaje(LocalDate.now(ZoneId.of("Europe/Madrid")));
        jugador.setFechaFinBlindaje(LocalDateTime.now(ZoneId.of("Europe/Madrid")).plusDays(7));
        cancelarOfertasPendientes(jugador);
        usuarioRepository.save(comprador);
        jugadorRepository.save(jugador);
        noticiaRepository.save(new Noticia("💰 MERCADO: " + comprador.getNombre() + " ha fichado a " + jugador.getNombre() +
                " (" + jugador.getPosicion() + ") por " + fmtDinero(jugador.getValor())));
        return "✅ Fichaje realizado.";
    }

    @PostMapping("/mercado/robar/{idJugador}/{idLadron}")
    public String robarJugador(@PathVariable Long idJugador, @PathVariable Long idLadron) {
        // NUEVO BLOQUEO: Consultamos BBDD
        if (getJornadaActiva().isBloqueada()) {
            return "⛔ No puedes hacer compras, ventas o clausulazos desde que se conoce la alineación hasta el día siguiente.";
        }

        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario ladron = usuarioRepository.findById(idLadron).orElseThrow();
        Usuario victima = jugador.getPropietario();

        if (victima == null) {
            return "❌ Es libre, fíchalo normal.";
        }
        if (victima.getId().equals(ladron.getId())) {
            return "❌ No te puedes robar a ti mismo.";
        }
        if (jugador.getFechaFinBlindaje() != null && jugador.getFechaFinBlindaje().isAfter(LocalDateTime.now(ZoneId.of("Europe/Madrid")))) {
            return "🛡️ JUGADOR BLINDADO. No se puede robar todavía.";
        }

        int precioRobo = jugador.getClausula();
        if (ladron.getPresupuesto() < precioRobo) {
            return "❌ No tienes suficiente dinero. No puedes quedarte en negativo robando jugadores.";
        }

        ladron.setPresupuesto(ladron.getPresupuesto() - precioRobo);
        victima.setPresupuesto(victima.getPresupuesto() + precioRobo);
        jugador.setPropietario(ladron);
        jugador.setClausula(precioRobo);
        jugador.setJornadaFichaje(getJornadaActiva().getId());
        jugador.setFechaFichaje(LocalDate.now(ZoneId.of("Europe/Madrid")));
        jugador.setFechaFinBlindaje(LocalDateTime.now(ZoneId.of("Europe/Madrid")).plusDays(7));

        Jornada jornadaActual = getJornadaActiva();
        Optional<Equipo> equipoVictima = equipoRepository.findByUsuario(victima).stream()
                .filter(e -> e.getJornada().getId().equals(jornadaActual.getId()))
                .findFirst();
        if (equipoVictima.isPresent()) {
            Equipo eq = equipoVictima.get();
            eq.getJugadoresAlineados().remove(jugador);
            equipoRepository.save(eq);
        }

        cancelarOfertasPendientes(jugador);
        usuarioRepository.save(ladron);
        usuarioRepository.save(victima);
        jugadorRepository.save(jugador);
        noticiaRepository.save(new Noticia("🔥 CLAUSULAZO: " + ladron.getNombre() + " ha robado a " + jugador.getNombre() +
                " (" + jugador.getPosicion() + ") " + " al mánager " + victima.getNombre() + " por " + fmtDinero(precioRobo)));
        return "✅ ¡Has hecho un clausulazo!";
    }

    @PostMapping("/mercado/vender/{idJugador}/{idUsuario}")
    public String venderJugador(@PathVariable Long idJugador, @PathVariable Long idUsuario) {
        if (getJornadaActiva().isBloqueada()) {
            return "⛔ No puedes hacer compras, ventas o clausulazos desde que se conoce la alineación hasta el día siguiente.";
        }

        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario vendedor = usuarioRepository.findById(idUsuario).orElseThrow();
        if (jugador.getPropietario() == null || !jugador.getPropietario().getId().equals(idUsuario)) {
            return "❌ No es tuyo.";
        }

        int ingreso = jugador.getValor() + (jugador.getClausula() - jugador.getValor()) / 2;
        vendedor.setPresupuesto(vendedor.getPresupuesto() + ingreso);
        jugador.setPropietario(null);
        jugador.setClausula(jugador.getValor());
        jugador.setFechaFinBlindaje(null);
        jugador.setFechaVenta(LocalDate.now(ZoneId.of("Europe/Madrid")));

        Jornada jornadaActual = getJornadaActiva();
        List<Equipo> equipos = equipoRepository.findByUsuario(vendedor);
        for(Equipo e : equipos) {
            if(e.getJornada().getId().equals(jornadaActual.getId())) {
                e.getJugadoresAlineados().remove(jugador);
                equipoRepository.save(e);
            }
        }

        cancelarOfertasPendientes(jugador);
        usuarioRepository.save(vendedor);
        jugadorRepository.save(jugador);
        noticiaRepository.save(new Noticia("👋 VENTA: " + vendedor.getNombre() + " ha vendido a " + jugador.getNombre() +
                " (" + jugador.getPosicion() + ") " + fmtDinero(ingreso)));
        return "✅ Jugador vendido. Recibes " + fmtDinero(ingreso);
    }

    @PostMapping("/jugador/subir-clausula/{idJugador}/{cantidad}")
    public String subirClausula(@PathVariable Long idJugador, @PathVariable int cantidad) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario propietario = jugador.getPropietario();

        if (getJornadaActiva().isBloqueada()) {
            return "⛔ El mercado está bloqueado.";
        }

        if (cantidad <= 0) {
            return "❌ Cantidad no válida.";
        }
        if (propietario.getPresupuesto() - cantidad < 0) {
            return "❌ No tienes saldo suficiente. No puedes quedarte en negativo al subir cláusula.";
        }

        propietario.setPresupuesto(propietario.getPresupuesto() - cantidad);
        jugador.setClausula(jugador.getClausula() + (cantidad * 2));
        usuarioRepository.save(propietario);
        jugadorRepository.save(jugador);
        return "✅ Cláusula subida a " + fmtDinero(jugador.getClausula());
    }

    @PostMapping("/ofertas/crear")
    public String crearOferta(@RequestBody Map<String, Object> datos) {
        Long idJugador = Long.valueOf(datos.get("idJugador").toString());
        Long idComprador = Long.valueOf(datos.get("idComprador").toString());
        int cantidad = Integer.parseInt(datos.get("cantidad").toString());

        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario comprador = usuarioRepository.findById(idComprador).orElseThrow();
        Usuario vendedor = jugador.getPropietario();

        if (vendedor == null) {
            return "❌ El jugador es libre, fíchalo en el mercado.";
        }
        if (vendedor.getId().equals(comprador.getId())) {
            return "❌ No puedes ofertarte a ti mismo.";
        }
        if (comprador.getPresupuesto() < cantidad) {
            return "❌ No tienes saldo suficiente.";
        }

        Oferta oferta = new Oferta(jugador, comprador, vendedor, cantidad);
        ofertaRepository.save(oferta);
        return "✅ Oferta enviada a " + vendedor.getNombre();
    }

    @PostMapping("/ofertas/responder/{idOferta}/{accion}")
    public String responderOferta(@PathVariable Long idOferta, @PathVariable String accion) {
        Oferta oferta = ofertaRepository.findById(idOferta).orElseThrow();
        if (!oferta.getEstado().equals("PENDIENTE")) {
            return "❌ Esta oferta ya no está activa.";
        }

        if (accion.equals("rechazar")) {
            oferta.setEstado("RECHAZADA");
            ofertaRepository.save(oferta);
            return "❌ Oferta rechazada.";
        } else if (accion.equals("aceptar")) {

            if (getJornadaActiva().isBloqueada()) {
                return "⛔ No se pueden aceptar ofertas durante el bloqueo.";
            }

            Usuario comprador = oferta.getComprador();
            Usuario vendedor = oferta.getVendedor();
            Jugador jugador = oferta.getJugador();

            if (comprador.getPresupuesto() < oferta.getCantidad()) {
                return "❌ El comprador ya no tiene saldo suficiente.";
            }
            if (jugador.getPropietario() == null || !jugador.getPropietario().getId().equals(vendedor.getId())) {
                oferta.setEstado("CANCELADA");
                ofertaRepository.save(oferta);
                return "❌ El jugador ya no pertenece a este usuario.";
            }

            comprador.setPresupuesto(comprador.getPresupuesto() - oferta.getCantidad());
            vendedor.setPresupuesto(vendedor.getPresupuesto() + oferta.getCantidad());
            jugador.setPropietario(comprador);
            jugador.setClausula(Math.max(oferta.getCantidad(), jugador.getValor()));
            jugador.setFechaFinBlindaje(LocalDateTime.now(ZoneId.of("Europe/Madrid")).plusDays(7));
            jugador.setJornadaFichaje(getJornadaActiva().getId());
            jugador.setFechaFichaje(LocalDate.now(ZoneId.of("Europe/Madrid")));

            Jornada jornadaActual = getJornadaActiva();
            equipoRepository.findByUsuario(vendedor).stream()
                    .filter(e -> e.getJornada().getId().equals(jornadaActual.getId()))
                    .findFirst().ifPresent(e -> {
                        e.getJugadoresAlineados().remove(jugador);
                        equipoRepository.save(e);
                    });
            oferta.setEstado("ACEPTADA");
            usuarioRepository.save(comprador);
            usuarioRepository.save(vendedor);
            jugadorRepository.save(jugador);
            ofertaRepository.save(oferta);
            cancelarOfertasPendientes(jugador);
            noticiaRepository.save(new Noticia("🤝 ACUERDO: " + comprador.getNombre() + " ha comprado a " + jugador.getNombre() + "( " + jugador.getPosicion() + ") al mánager " + vendedor.getNombre() + " por " + fmtDinero(oferta.getCantidad())));
            return "✅ Oferta aceptada. El jugador ha sido transferido.";
        }
        return "❌ Error.";
    }

    @PostMapping("/alinear/{usuarioId}")
    public String guardarAlineacion(@RequestBody List<Long> idsJugadores, @PathVariable Long usuarioId) {
        if (idsJugadores == null) {
            return "❌ Error: Lista vacía.";
        }
        if (idsJugadores.size() > 7) {
            return "❌ Máximo 7 jugadores.";
        }

        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        Jornada jornada = getJornadaActiva();
        List<Jugador> seleccionados = jugadorRepository.findAllById(idsJugadores);
        for (Jugador j : seleccionados) {
            if (j.getPropietario() == null || !j.getPropietario().getId().equals(usuarioId)) {
                return "❌ " + j.getNombre() + " no te pertenece.";
            }
        }
        Equipo equipo = equipoRepository.findByUsuario(usuario).stream().filter(e -> e.getJornada().getId().equals(jornada.getId())).findFirst().orElse(new Equipo(usuario, jornada));
        equipo.setJugadoresAlineados(seleccionados);
        equipoRepository.save(equipo);
        return "✅ Alineación guardada para la jornada " + getNumeroJornadaReal();
    }

    @PostMapping("/reclamar-premio/{idEquipo}")
    public String reclamarPremio(@PathVariable Long idEquipo) {
        Equipo equipo = equipoRepository.findById(idEquipo).orElseThrow();
        if (equipo.isReclamado()) {
            return "❌ Ya cobrado.";
        }
        int p = equipo.getPuntosTotalesJornada();

        int base;
        if (p > 0) {
            base = p * 100_000;
        } else {
            base = 0;
        }

        int max = actuacionRepository.findAll().stream().filter(a -> a.getJornada().getId().equals(equipo.getJornada().getId())).mapToInt(Actuacion::getPuntosTotales).max().orElse(0);
        boolean mvp = false;
        if(max > 0) {
            for(Jugador j : equipo.getJugadoresAlineados()){
                Optional<Actuacion> act = actuacionRepository.findByJugadorAndJornada(j, equipo.getJornada());
                if(act.isPresent() && act.get().getPuntosTotales() == max) {
                    mvp = true;
                    break;
                }
            }
        }

        int bonus;
        if (mvp) {
            bonus = 1_000_000;
        } else {
            bonus = 0;
        }

        Usuario u = equipo.getUsuario();
        u.setPresupuesto(u.getPresupuesto() + base + bonus);
        equipo.setReclamado(true);
        usuarioRepository.save(u);
        equipoRepository.save(equipo);

        String mensajeExtra;
        if (mvp) {
            mensajeExtra = " + 🏆 " + fmtDinero(bonus);
        } else {
            mensajeExtra = "";
        }
        return "💰 Reclamado: " + fmtDinero(base) + mensajeExtra;
    }

    //**************************************************************************************************************+***
    //                                                      DELETE
    //**************************************************************************************************************+***

    @DeleteMapping("/admin/rechazar/{idUsuario}")
    public String rechazarUsuario(@PathVariable Long idUsuario) {
        usuarioRepository.deleteById(idUsuario);
        return "🗑️ Solicitud rechazada.";
    }

    @DeleteMapping("/admin/eliminar-usuario/{idUsuario}")
    public String eliminarUsuario(@PathVariable Long idUsuario) {
        Usuario u = usuarioRepository.findById(idUsuario).orElseThrow();
        if(u.isEsAdmin() && !u.getNombre().equals("Cristian")) {
            return "❌ No se puede borrar al admin.";
        }

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

    //**************************************************************************************************************+***
    //                                              MÉTODOS AUXILIARES
    //**************************************************************************************************************+***

    private List<Map<String, Object>> mapJugadoresCampo(List<Actuacion> acts, int maxPuntos) {
        return acts.stream().map(a -> {
            Jugador j = a.getJugador();

            String imagen;
            if (j.getUrlImagen() != null) {
                imagen = j.getUrlImagen();
            } else {
                imagen = "";
            }

            boolean esMvp;
            if (a.getPuntosTotales() == maxPuntos && maxPuntos > 0) {
                esMvp = true;
            } else {
                esMvp = false;
            }

            return Map.<String, Object>of(
                    "nombre", j.getNombre(), "posicion", j.getPosicion(), "puntos", a.getPuntosTotales(), "imagen", imagen, "mvp", esMvp
            );
        }).collect(Collectors.toList());
    }

    private String fmtDinero(int cantidad) {
        return NumberFormat.getCurrencyInstance(Locale.of("es", "ES")).format(cantidad);
    }

    private Jornada getJornadaActiva() {
        List<Jornada> jornadas = jornadaRepository.findAll();
        Jornada jornadaResultado;

        if (jornadas.isEmpty()) {
            Jornada j1 = new Jornada();
            j1.setNumero(1);
            j1.setBloqueada(false);
            jornadaResultado = jornadaRepository.save(j1);
        } else {
            Jornada activa = jornadas.get(jornadas.size() - 1);
            if (activa.getNumero() <= 0) {
                activa.setNumero(1);
                jornadaRepository.save(activa);
            }
            jornadaResultado = activa;
        }
        return jornadaResultado;
    }

    private long getNumeroJornadaReal() {
        return getJornadaActiva().getNumero();
    }

    private boolean isMercadoCerrado() {
        LocalTime ahora = LocalTime.now(ZoneId.of("Europe/Madrid"));
        boolean cerradoNoche = ahora.isAfter(LocalTime.of(21, 30)) || ahora.equals(LocalTime.of(21, 30));
        boolean cerradoManana = ahora.isBefore(LocalTime.of(10, 0));
        return cerradoNoche || cerradoManana;
    }

    private int getPesoPosicion(String pos) {
        if (pos == null) {
            return 5;
        }

        switch(pos.toUpperCase()) {
            case "PORTERO": return 1;
            case "DEFENSA": return 2;
            case "MEDIO": return 3;
            case "DELANTERO": return 4;
            default: return 5;
        }
    }

    private void cancelarOfertasPendientes(Jugador j) {
        List<Oferta> pendientes = ofertaRepository.findByJugador(j);
        for(Oferta o : pendientes) {
            if(o.getEstado().equals("PENDIENTE")) {
                o.setEstado("CANCELADA");
                ofertaRepository.save(o);
            }
        }
    }

    public static class DatosPartido {
        public Long idJugador;
        public boolean jugado; public boolean victoria; public boolean derrota;
        public int goles; public int golesEncajados; public int autogoles;
        public String equipoColor;
    }
}