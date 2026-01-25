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
    private boolean bloqueoAcciones = false;

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

    //BLOQUEO DE ACCIONES

    //Tarea automática: A las 10:00 AM se quita el bloqueo SIEMPRE (haya o no haya).
    @Scheduled(cron = "0 0 10 * * *", zone = "Europe/Madrid")
    public void desbloqueoAutomatico() {
        if (bloqueoAcciones) {
            bloqueoAcciones = false;
        }
    }

    @PostMapping("/admin/toggle-bloqueo")
    public String toggleBloqueo() {
        bloqueoAcciones = !bloqueoAcciones;
        String estado = bloqueoAcciones ? "ACTIVADO 🔒" : "DESACTIVADO 🔓";
        return "Bloqueo de acciones " + estado;
    }

    // - - - - - ENDPOINTS - - - - -

    //*********************************************************************************************************************
    //                                                          GET
    //*********************************************************************************************************************

    //ADMIN - Gestión del bloqueo de todas las acciones
    @GetMapping("/admin/estado-bloqueo")
    public boolean getEstadoBloqueo() {
        return bloqueoAcciones;
    }
    //ADMIN - Gestión de usuarios (con contraseñas)
    @GetMapping("/admin/usuarios-gestion")
    public List<Usuario> getUsuariosGestion() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        return usuarios;
    }

    //ADMIN - Aprobaciones
    @GetMapping("/admin/pendientes")
    public List<Usuario> verUsuariosPendientes() {
        List<Usuario> pendientes = usuarioRepository.findAll().stream().filter(u -> !u.isActivo()).collect(Collectors.toList());
        return pendientes;
    }

    //ADMIN - Recuperar los jugadores que han puntuado (aunque erróneamente) para corregir el error de puntos mediante un reseteo
    @GetMapping("/admin/jugadores-puntuados")
    public List<Jugador> getJugadoresPuntuados() {
        Jornada actual = getJornadaActiva();
        List<Jugador> puntuados = jugadorRepository.findAll().stream().filter(j -> actuacionRepository.findByJugadorAndJornada(j, actual).isPresent()).sorted((j1, j2) -> {
            int p1 = getPesoPosicion(j1.getPosicion());
            int p2 = getPesoPosicion(j2.getPosicion());
            int comparacion;
            if (p1 != p2) {
                comparacion = Integer.compare(p1, p2);
            } else {
                comparacion = j1.getNombre().compareToIgnoreCase(j2.getNombre());
            }
            return comparacion;
        }).collect(Collectors.toList());
        return puntuados;
    }

    //ADMIN - Jugadores que AÚN NO han puntuado en esta jornada
    @GetMapping("/admin/jugadores-pendientes")
    public List<Jugador> getJugadoresPendientes() {
        Jornada actual = getJornadaActiva();

        // 1. Sacamos todos los jugadores
        List<Jugador> todos = jugadorRepository.findAll();

        // 2. Sacamos los IDs de los que YA han jugado esta jornada
        List<Long> idsPuntuados = actuacionRepository.findAll().stream()
                .filter(a -> a.getJornada().getId().equals(actual.getId()))
                .map(a -> a.getJugador().getId())
                .collect(Collectors.toList());

        // 3. Devolvemos la lista de todos MENOS los que ya están puntuados
        return todos.stream()
                .filter(j -> !idsPuntuados.contains(j.getId()))
                .sorted(Comparator.comparing(Jugador::getNombre))
                .collect(Collectors.toList());
    }

    //Jornada actual en el dashboard
    @GetMapping("/jornada/actual")
    public long getNumeroJornadaActualEndpoint() {
        long numero = getNumeroJornadaReal();
        return numero;
    }

    //Lista de mánagers
    @GetMapping("/usuarios")
    public List<Usuario> verRivales() {
        List<Usuario> rivales = usuarioRepository.findAll().stream().filter(Usuario::isActivo).collect(Collectors.toList());
        return rivales;
    }

    //Jugadores de plantilla de mánagers ajenos
    @GetMapping("/jugadores")
    public List<Map<String, Object>> verTodosLosJugadores() {
        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("Europe/Madrid"));
        List<Map<String, Object>> listaJugadores = jugadorRepository.findAll().stream().map(j -> {
            boolean blindado = j.getFechaFinBlindaje() != null && j.getFechaFinBlindaje().isAfter(ahora);
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
        return listaJugadores;
    }

    //MERCADO DIARIO - Se renueva automáticamente cada día a las 0:00h
    @GetMapping("/mercado-diario")
    public List<Jugador> getMercadoDiario() {
        //Búsqueda de TODOS los jugadores libres que queden
        List<Jugador> todosLibres = jugadorRepository.findAll().stream().filter(j -> j.getPropietario() == null).sorted(Comparator.comparing(Jugador::getId)).collect(Collectors.toList());

        //Se calcula una semilla y al pasar de las 23:59, este número cambia solo.
        LocalDate hoy = LocalDate.now(ZoneId.of("Europe/Madrid"));
        long seed = hoy.toEpochDay() + desplazamiento; //Por si el admin le da a "Generar Nuevo Mercado"

        //Se baraja la lista completa
        Collections.shuffle(todosLibres, new Random(seed));

        //14 primeros jugadores tras barajar
        return todosLibres.stream().limit(14).collect(Collectors.toList());
    }

    //El resumen en el apartado "PARTIDOS"
    @GetMapping("/jornada/resumen")
    public List<Map<String, Object>> verResumenJornadaAnterior() {
        Jornada actual = getJornadaActiva();
        int numAnterior = actual.getNumero() - 1;

        if (numAnterior < 1) {
            List<Map<String, Object>> vacio = new ArrayList<>();
            return vacio;
        }

        Optional<Jornada> jOpt = jornadaRepository.findAll().stream().filter(j -> j.getNumero() == numAnterior).findFirst();

        if (jOpt.isEmpty()) {
            List<Map<String, Object>> vacio = new ArrayList<>();
            return vacio;
        }

        Jornada anterior = jOpt.get();
        List<Equipo> equipos = equipoRepository.findByJornada(anterior);
        List<Map<String, Object>> resumen = equipos.stream().map(e -> {
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

            //Ordenamos los jugadores por posición
            jugadores.sort((a,b) -> Integer.compare(getPesoPosicion((String)a.get("posicion")), getPesoPosicion((String)b.get("posicion"))));

            return Map.<String, Object>of(
                    "manager", e.getUsuario().getNombre(),
                    "puntosTotal", e.getPuntosTotalesJornada(),
                    "jugadores", jugadores
            );
        }).sorted((a,b) -> Integer.compare((int)b.get("puntosTotal"), (int)a.get("puntosTotal"))).collect(Collectors.toList());
        return resumen;
    }

    //Resumen para una jornada específica
    @GetMapping("/jornada/{numero}/resumen-partido")
    public Map<String, Object> getResumenPartido(@PathVariable int numero) {
        Optional<Jornada> jOpt = jornadaRepository.findAll().stream().filter(j -> j.getNumero() == numero).findFirst();

        if (jOpt.isEmpty()) {
            Map<String, Object> error = Map.of("error", "Jornada no encontrada");
            return error;
        }

        Jornada jornada = jOpt.get();
        List<Actuacion> actuaciones = actuacionRepository.findAll().stream().filter(a -> a.getJornada().getId().equals(jornada.getId())).collect(Collectors.toList());

        if (actuaciones.isEmpty()) {
            Map<String, Object> error = Map.of("error", "Sin datos en esta jornada");
            return error;
        }

        //MVP
        int maxPuntos = actuaciones.stream().mapToInt(Actuacion::getPuntosTotales).max().orElse(0);
        Map<String, List<Actuacion>> grupos = actuaciones.stream().filter(a -> a.getEquipoColor() != null).collect(Collectors.groupingBy(Actuacion::getEquipoColor));
        List<String> colores = new ArrayList<>(grupos.keySet());

        //Equipos
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

        Map<String, Object> respuesta = Map.of(
                "colorA", colorA,
                "colorB", colorB,
                "equipoA", equipoA,
                "equipoB", equipoB
        );
        return respuesta;
    }

    //Resumen de las alineaciones de cada mánager para una jornada en concreto
    @GetMapping("/jornada/{numero}/resumen-managers")
    public List<Map<String, Object>> verResumenManagers(@PathVariable int numero) {
        Optional<Jornada> jOpt = jornadaRepository.findAll().stream().filter(j -> j.getNumero() == numero).findFirst();

        if (jOpt.isEmpty()) {
            List<Map<String, Object>> vacio = new ArrayList<>();
            return vacio;
        }

        Jornada jornada = jOpt.get();
        List<Equipo> equipos = equipoRepository.findByJornada(jornada);
        List<Map<String, Object>> resumen = equipos.stream().map(e -> {
            List<Map<String, Object>> jugadores = new ArrayList<>();
            for (Jugador j : e.getJugadoresAlineados()) {
                int pts = actuacionRepository.findByJugadorAndJornada(j, jornada)
                        .map(Actuacion::getPuntosTotales).orElse(0);

                jugadores.add(Map.of(
                        "nombre", j.getNombre(),
                        "posicion", j.getPosicion(),
                        "puntos", pts
                ));
            }
            jugadores.sort((a,b) -> Integer.compare(getPesoPosicion((String)a.get("posicion")), getPesoPosicion((String)b.get("posicion"))));

            return Map.<String, Object>of(
                    "manager", e.getUsuario().getNombre(),
                    "puntosTotal", e.getPuntosTotalesJornada(),
                    "jugadores", jugadores
            );
        }).sorted((a,b) -> Integer.compare((int)b.get("puntosTotal"), (int)a.get("puntosTotal"))).collect(Collectors.toList());
        return resumen;
    }

    //Ofertas
    @GetMapping("/ofertas/mis-ofertas/{idUsuario}")
    public Map<String, List<Map<String, Object>>> verMisOfertas(@PathVariable Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow();
        List<Map<String, Object>> recibidas = ofertaRepository.findByVendedorAndEstado(usuario, "PENDIENTE").stream()
                .map(o -> Map.<String, Object>of(
                        "id", o.getId(),
                        "jugador", o.getJugador().getNombre(),
                        "comprador", o.getComprador().getNombre(),
                        "cantidad", o.getCantidad(),
                        "cantidadFmt", fmtDinero(o.getCantidad())
                )).collect(Collectors.toList());

        List<Map<String, Object>> enviadas = ofertaRepository.findByCompradorAndEstado(usuario, "PENDIENTE").stream()
                .map(o -> Map.<String, Object>of(
                        "id", o.getId(),
                        "jugador", o.getJugador().getNombre(),
                        "vendedor", o.getVendedor().getNombre(),
                        "cantidad", o.getCantidad(),
                        "cantidadFmt", fmtDinero(o.getCantidad())
                )).collect(Collectors.toList());

        Map<String, List<Map<String, Object>>> todas = Map.of("recibidas", recibidas, "enviadas", enviadas);
        return todas;
    }

    //Mi alineación
    @GetMapping("/alineacion/{usuarioId}")
    public List<Jugador> getAlineacion(@PathVariable Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        Jornada jornadaActual = getJornadaActiva();
        Optional<Equipo> equipo = equipoRepository.findByUsuario(usuario).stream().filter(e -> e.getJornada().getId().equals(jornadaActual.getId())).findFirst();
        List<Jugador> alineacion = equipo.map(Equipo::getJugadoresAlineados).orElse(List.of());
        return alineacion;
    }

    //Noticias
    @GetMapping("/noticias")
    public List<Noticia> verNoticias() {
        LocalDateTime limite = LocalDateTime.now(ZoneId.of("Europe/Madrid")).minusDays(7); //Día de hoy menos 7 días
        List<Noticia> todas = noticiaRepository.findAll();
        List<Noticia> paraBorrar = todas.stream().filter(n -> n.getFecha().isBefore(limite)).collect(Collectors.toList());

        if(!paraBorrar.isEmpty()) {
            noticiaRepository.deleteAll(paraBorrar);
        }

        List<Noticia> noticiasActuales = noticiaRepository.findAllByOrderByFechaDesc();
        return noticiasActuales;
    }

    //Historial de mis jornadas (apartado "MIS JORNADAS")
    @GetMapping("/historial/{usuarioId}")
    public List<Map<String, Object>> getHistorialUsuario(@PathVariable Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        List<Map<String, Object>> historial = equipoRepository.findByUsuario(usuario).stream()
                .sorted((e1, e2) -> Integer.compare(e2.getJornada().getNumero(), e1.getJornada().getNumero()))
                .map(e -> {
                    List<Map<String, Object>> detalles = new ArrayList<>();
                    for (Jugador j : e.getJugadoresAlineados()) {
                        int p = actuacionRepository.findByJugadorAndJornada(j, e.getJornada()).map(Actuacion::getPuntosTotales).orElse(0);
                        detalles.add(Map.of("nombre", j.getNombre(), "posicion", j.getPosicion(), "puntos", p));
                    }
                    return Map.<String, Object>of("jornadaNumero", e.getJornada().getNumero(), "puntosTotal", e.getPuntosTotalesJornada(), "jugadores", detalles);
                }).collect(Collectors.toList());
        return historial;
    }

    //Historial de puntos de cada jugador al hacer clic
    @GetMapping("/jugador/{id}/historial-puntos")
    public List<Map<String, Object>> getHistorialPuntosJugador(@PathVariable Long id) {
        Jugador jugador = jugadorRepository.findById(id).orElseThrow();
        List<Map<String, Object>> historial = actuacionRepository.findByJugador(jugador).stream().map(a -> Map.<String, Object>of("jornada", a.getJornada().getNumero(), "puntos", a.getPuntosTotales())).sorted((m1, m2) -> Integer.compare((Integer)m1.get("jornada"), (Integer)m2.get("jornada"))).collect(Collectors.toList());
        return historial;
    }

    //Dinero para reclamar tras jornada
    @GetMapping("/premios-pendientes/{idUsuario}")
    public List<Map<String, Object>> verPremiosPendientes(@PathVariable Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow();
        Jornada jornadaActual = getJornadaActiva();

        List<Map<String, Object>> premios = equipoRepository.findByUsuario(usuario).stream().filter(e -> !e.getJornada().getId().equals(jornadaActual.getId()) && !e.isReclamado()).map(e -> {
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

            int bonus = 0;
            if (mvp) {
                bonus = 1_000_000;
            }

            return Map.<String, Object>of("idEquipo", e.getId(), "jornada", e.getJornada().getNumero(), "puntos", p, "dineroFmt", fmtDinero(dinero + bonus), "tieneMvp", mvp, "nombreMvp", nombreMvp, "bonusFmt", fmtDinero(bonus));
        }).collect(Collectors.toList());
        return premios;
    }

    //Clasificación global de los mánagers con los puntos y el valor que tiene asociado su equipo
    @GetMapping("/clasificacion")
    public List<Map<String, Object>> verClasificacion() {
        List<Usuario> usuarios = usuarioRepository.findAll().stream().filter(Usuario::isActivo).collect(Collectors.toList());
        List<Jugador> todosJugadores = jugadorRepository.findAll();
        List<Equipo> todosEquipos = equipoRepository.findAll();

        List<Map<String, Object>> ranking = usuarios.stream().map(u -> {
            int p = todosEquipos.stream().filter(e -> e.getUsuario().getId().equals(u.getId())).mapToInt(Equipo::getPuntosTotalesJornada).sum();
            int v = todosJugadores.stream().filter(j -> j.getPropietario() != null && j.getPropietario().getId().equals(u.getId())).mapToInt(Jugador::getValor).sum();
            return Map.<String, Object>of("nombre", u.getNombre(), "puntos", p, "valorPlantilla", v);
        }).sorted((m1, m2) -> {
            int cmp = Integer.compare((int)m2.get("puntos"), (int)m1.get("puntos"));
            int resultadoComparacion;
            if (cmp != 0) {
                resultadoComparacion = cmp;
            } else {
                resultadoComparacion = Integer.compare((int)m2.get("valorPlantilla"), (int)m1.get("valorPlantilla"));
            }
            return resultadoComparacion;
        }).collect(Collectors.toList());
        return ranking;
    }

    //*********************************************************************************************************************
    //                                                          POST
    //*********************************************************************************************************************

    //ADMIN - Aprobación de usuarios
    @PostMapping("/admin/aprobar/{idUsuario}")
    public String aprobarUsuario(@PathVariable Long idUsuario) {
        Usuario u = usuarioRepository.findById(idUsuario).orElseThrow();
        u.setActivo(true);
        usuarioRepository.save(u);
        noticiaRepository.save(new Noticia("👋 BIENVENIDA: " + u.getNombre() + " ha entrado a la liga."));
        String mensaje = "✅ Usuario aprobado.";
        return mensaje;
    }

    //ADMIN - Editar nombre de usuario
    @PostMapping("/admin/editar-usuario/{idUsuario}")
    public String editarUsuario(@PathVariable Long idUsuario, @RequestBody Map<String, String> datos) {
        String nuevoNombre = datos.get("nombre");
        if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) {
            String error = "❌ El nombre no puede estar vacío.";
            return error;
        }

        Usuario existente = usuarioRepository.findByNombre(nuevoNombre);
        if (existente != null && !existente.getId().equals(idUsuario)) {
            String error = "❌ Ese nombre ya está en uso por otro jugador.";
            return error;
        }

        Usuario u = usuarioRepository.findById(idUsuario).orElseThrow();
        String antiguo = u.getNombre();
        u.setNombre(nuevoNombre);
        usuarioRepository.save(u);
        String mensaje = "✅ Se ha cambiado el nombre '" + antiguo + "' a '" + nuevoNombre + "'.";
        return mensaje;
    }

    //ADMIN - Registrar a un usuario
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
        int cambioValor = puntos * 100_000; //Cada punto equivale a 100.000€
        int nuevoValor = jugador.getValor() + cambioValor;
        if (nuevoValor < 150_000) {
            nuevoValor = 150_000;
        }

        //Aumento o disminución del jugador según su actuación en los partidos
        jugador.setValor(nuevoValor);

        //Si por lo que sea la cláusula se queda por debajo del valor de mercado, se establece al valor de mercado
        if (jugador.getPropietario() != null) {
            if (jugador.getClausula() < jugador.getValor()) {
                jugador.setClausula(jugador.getValor());
            }
        }

        jugadorRepository.save(jugador);
        String mensaje = "✅ Puntos de " + jugador.getNombre() + " en esta jornada: " + puntos;
        return mensaje;
    }

    //ADMIN - Cerrar las jornadas
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
        jornadaRepository.save(nueva);
        noticiaRepository.save(new Noticia("🏁 JORNADA " + actual.getNumero() + " FINALIZADA.\n" + res));

        String mensaje = "✅ Jornada terminada.";
        return mensaje;
    }

    //ADMIN - Resetear mercado
    @PostMapping("/admin/reset-mercado")
    public String resetMercado() {
        desplazamiento++; //Cambio de semilla para que el azar sea distinto
        String mensaje = "✅ Mercado renovado con éxito.";
        return mensaje;
    }

    //ADMIN - Resetear la liga entera
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
        jornadaRepository.save(j1);
        noticiaRepository.save(new Noticia("☢️ LIGA RESETEADA."));
        String mensaje = "✅ Liga reseteada.";
        return mensaje;
    }

    //ADMIN - Resetear los puntos de un jugador
    @PostMapping("/admin/reset-puntos/{idJugador}")
    public String resetearPuntosJugador(@PathVariable Long idJugador) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Jornada jornada = getJornadaActiva();
        Optional<Actuacion> actaOpt = actuacionRepository.findByJugadorAndJornada(jugador, jornada);

        if (actaOpt.isEmpty()) {
            String error = "❌ Este jugador no tiene puntos registrados en esta jornada.";
            return error;
        }

        Actuacion acta = actaOpt.get();
        int puntosRestar = acta.getPuntosTotales();
        int valorRestar = puntosRestar * 100_000;
        jugador.setPuntosAcumulados(jugador.getPuntosAcumulados() - puntosRestar);
        jugador.setValor(jugador.getValor() - valorRestar);
        jugador.setClausula(jugador.getClausula() - valorRestar);
        actuacionRepository.delete(acta);
        jugadorRepository.save(jugador);
        String mensaje = "✅ CORREGIDO: Puntos de " + jugador.getNombre() + " reseteados. (Restados " + puntosRestar + " pts y " + fmtDinero(valorRestar) + " valor)";
        return mensaje;
    }

    //ADMIN - Borrar un jugador específico
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

    //Autorización para entrar
    @PostMapping("/auth/registro")
    public String registrarUsuario(@RequestBody Usuario datos) {
        if (usuarioRepository.findByNombre(datos.getNombre()) != null) {
            String error = "❌ El nombre ya existe.";
            return error;
        }

        boolean esPrimero = usuarioRepository.count() == 0;
        Usuario nuevo = new Usuario(datos.getNombre(), datos.getPassword(), 100_000_000, esPrimero);
        nuevo.setActivo(esPrimero);
        usuarioRepository.save(nuevo);

        String mensaje;
        if (esPrimero) {
            noticiaRepository.save(new Noticia("👑 FUNDADOR: " + datos.getNombre() + " ha inaugurado la liga como Admin."));
            mensaje = "✅ ¡Liga inaugurada! Eres el Admin.";
        } else {
            mensaje = "✅ Solicitud enviada. Contacta con el creador de la app por Whatsapp para que te acepte y luego pulsa el botón 'Entrar'.";
        }
        return mensaje;
    }

    //Login
    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Usuario datos) {
        Usuario user = usuarioRepository.findByNombre(datos.getNombre());
        if (user == null || !user.getPassword().equals(datos.getPassword())) {
            Map<String, Object> error = Map.of("error", "Credenciales incorrectas.");
            return error;
        }
        if (!user.isActivo()) {
            Map<String, Object> error = Map.of("error", "⛔ Tu cuenta aún no ha sido aprobada por el Admin.");
            return error;
        }
        Map<String, Object> respuesta = Map.of("id", user.getId(), "nombre", user.getNombre(), "esAdmin", user.isEsAdmin(), "presupuesto", user.getPresupuesto());
        return respuesta;
    }

    //Usuarios en línea
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

    //Mercado
    @PostMapping("/mercado/comprar/{idJugador}/{idUsuario}")
    public String comprarJugadorLibre(@PathVariable Long idJugador, @PathVariable Long idUsuario) {

        if (bloqueoAcciones) return "⛔ No puedes hacer compras, ventas o clausulazos desde que se conoce la alineación hasta el día siguiente.";
        if (isMercadoCerrado()) {
            String error = "⛔ MERCADO CERRADO EN ESTOS MOMENTOS ⛔";
            return error;
        }

        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario comprador = usuarioRepository.findById(idUsuario).orElseThrow();
        if (jugador.getPropietario() != null) {
            String error = "❌ Error: Este jugador ya ha sido comprado.";
            return error;
        }
        comprador.setPresupuesto(comprador.getPresupuesto() - jugador.getValor());
        jugador.setPropietario(comprador);
        jugador.setClausula(jugador.getValor());
        jugador.setJornadaFichaje(getJornadaActiva().getId());
        jugador.setFechaFichaje(LocalDate.now(ZoneId.of("Europe/Madrid")));

        //Blindaje de 7 días
        jugador.setFechaFinBlindaje(LocalDateTime.now(ZoneId.of("Europe/Madrid")).plusDays(7));
        cancelarOfertasPendientes(jugador);
        usuarioRepository.save(comprador);
        jugadorRepository.save(jugador);
        noticiaRepository.save(new Noticia("💰 MERCADO: " + comprador.getNombre() + " ha fichado a " + jugador.getNombre() + " (" + jugador.getPosicion() + ") por " + fmtDinero(jugador.getValor())));
        String mensaje = "✅ Fichaje realizado.";
        return mensaje;
    }

    //Clausulazos
    @PostMapping("/mercado/robar/{idJugador}/{idLadron}")
    public String robarJugador(@PathVariable Long idJugador, @PathVariable Long idLadron) {
        if (bloqueoAcciones)  {
            return "⛔ No puedes hacer compras, ventas o clausulazos desde que se conoce la alineación hasta el día siguiente.";
        }

        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario ladron = usuarioRepository.findById(idLadron).orElseThrow();
        Usuario victima = jugador.getPropietario();

        if (victima == null) {
            String error = "❌ Es libre, fíchalo normal.";
            return error;
        }
        if (victima.getId().equals(ladron.getId())) {
            String error = "❌ No te puedes robar a ti mismo.";
            return error;
        }
        if (jugador.getFechaFinBlindaje() != null && jugador.getFechaFinBlindaje().isAfter(LocalDateTime.now(ZoneId.of("Europe/Madrid")))) {
            String error = "🛡️ JUGADOR BLINDADO. No se puede robar todavía.";
            return error;
        }

        int precioRobo = jugador.getClausula();
        if (ladron.getPresupuesto() < precioRobo) {
            String error = "❌ No tienes suficiente dinero. No puedes quedarte en negativo robando jugadores.";
            return error;
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
        noticiaRepository.save(new Noticia("🔥 CLAUSULAZO: " + ladron.getNombre() + " robó a " + jugador.getNombre() + " al mánager " + victima.getNombre() + " por " + fmtDinero(precioRobo)));

        String mensaje = "✅ ¡Has hecho un clausulazo!";
        return mensaje;
    }

    //Ventas
    @PostMapping("/mercado/vender/{idJugador}/{idUsuario}")
    public String venderJugador(@PathVariable Long idJugador, @PathVariable Long idUsuario) {
        if (bloqueoAcciones)  {
            return "⛔ No puedes hacer compras, ventas o clausulazos desde que se conoce la alineación hasta el día siguiente.";
        }

        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario vendedor = usuarioRepository.findById(idUsuario).orElseThrow();
        if (jugador.getPropietario() == null || !jugador.getPropietario().getId().equals(idUsuario)) {
            String error = "❌ No es tuyo.";
            return error;
        }

        int ingreso = jugador.getValor() + (jugador.getClausula() - jugador.getValor()) / 2;
        vendedor.setPresupuesto(vendedor.getPresupuesto() + ingreso);
        jugador.setPropietario(null);
        jugador.setClausula(jugador.getValor());
        jugador.setFechaFinBlindaje(null);

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
        noticiaRepository.save(new Noticia("👋 VENTA: " + vendedor.getNombre() + " vende a " + jugador.getNombre() + " por " + fmtDinero(ingreso)));

        String mensaje = "✅ Jugador vendido. Recibes " + fmtDinero(ingreso);
        return mensaje;
    }

    //Subir cláusulas
    @PostMapping("/jugador/subir-clausula/{idJugador}/{cantidad}")
    public String subirClausula(@PathVariable Long idJugador, @PathVariable int cantidad) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario propietario = jugador.getPropietario();
        if (cantidad <= 0) {
            String error = "❌ Cantidad no válida.";
            return error;
        }

        if (propietario.getPresupuesto() - cantidad < 0) {
            String error = "❌ No tienes saldo suficiente. No puedes quedarte en negativo al subir cláusula.";
            return error;
        }

        propietario.setPresupuesto(propietario.getPresupuesto() - cantidad);
        jugador.setClausula(jugador.getClausula() + (cantidad * 2));
        usuarioRepository.save(propietario);
        jugadorRepository.save(jugador);

        String mensaje = "✅ Cláusula subida a " + fmtDinero(jugador.getClausula());
        return mensaje;
    }

    //Hacer ofertas
    @PostMapping("/ofertas/crear")
    public String crearOferta(@RequestBody Map<String, Object> datos) {
        Long idJugador = Long.valueOf(datos.get("idJugador").toString());
        Long idComprador = Long.valueOf(datos.get("idComprador").toString());
        int cantidad = Integer.parseInt(datos.get("cantidad").toString());

        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario comprador = usuarioRepository.findById(idComprador).orElseThrow();
        Usuario vendedor = jugador.getPropietario();

        if (vendedor == null) {
            String error = "❌ El jugador es libre, fíchalo en el mercado.";
            return error;
        }
        if (vendedor.getId().equals(comprador.getId())) {
            String error = "❌ No puedes ofertarte a ti mismo.";
            return error;
        }
        if (comprador.getPresupuesto() < cantidad) {
            String error = "❌ No tienes saldo suficiente.";
            return error;
        }

        Oferta oferta = new Oferta(jugador, comprador, vendedor, cantidad);
        ofertaRepository.save(oferta);

        String mensaje = "✅ Oferta enviada a " + vendedor.getNombre();
        return mensaje;
    }

    //Respuesta a las ofertas
    @PostMapping("/ofertas/responder/{idOferta}/{accion}")
    public String responderOferta(@PathVariable Long idOferta, @PathVariable String accion) {
        Oferta oferta = ofertaRepository.findById(idOferta).orElseThrow();
        if (!oferta.getEstado().equals("PENDIENTE")) {
            String error = "❌ Esta oferta ya no está activa.";
            return error;
        }

        if (accion.equals("rechazar")) {
            oferta.setEstado("RECHAZADA");
            ofertaRepository.save(oferta);
            String mensaje = "❌ Oferta rechazada.";
            return mensaje;
        } else if (accion.equals("aceptar")) {
            Usuario comprador = oferta.getComprador();
            Usuario vendedor = oferta.getVendedor();
            Jugador jugador = oferta.getJugador();

            if (comprador.getPresupuesto() < oferta.getCantidad()) {
                String error = "❌ El comprador ya no tiene saldo suficiente.";
                return error;
            }
            if (jugador.getPropietario() == null || !jugador.getPropietario().getId().equals(vendedor.getId())) {
                oferta.setEstado("CANCELADA");
                ofertaRepository.save(oferta);
                String error = "❌ El jugador ya no pertenece a este usuario.";
                return error;
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
            String mensaje = "✅ Oferta aceptada. El jugador ha sido transferido.";
            return mensaje;
        }

        String errorGeneral = "❌ Error.";
        return errorGeneral;
    }

    //Crear alineación
    @PostMapping("/alinear/{usuarioId}")
    public String guardarAlineacion(@RequestBody List<Long> idsJugadores, @PathVariable Long usuarioId) {
        if (idsJugadores == null) {
            String error = "❌ Error: Lista vacía.";
            return error;
        }
        if (idsJugadores.size() > 7) {
            String error = "❌ Máximo 7 jugadores.";
            return error;
        }
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        Jornada jornada = getJornadaActiva();
        List<Jugador> seleccionados = jugadorRepository.findAllById(idsJugadores);
        for (Jugador j : seleccionados) {
            if (j.getPropietario() == null || !j.getPropietario().getId().equals(usuarioId)) {
                String error = "❌ " + j.getNombre() + " no te pertenece.";
                return error;
            }
        }
        Equipo equipo = equipoRepository.findByUsuario(usuario).stream().filter(e -> e.getJornada().getId().equals(jornada.getId())).findFirst().orElse(new Equipo(usuario, jornada));
        equipo.setJugadoresAlineados(seleccionados);
        equipoRepository.save(equipo);

        String mensaje = "✅ Alineación guardada para la jornada " + getNumeroJornadaReal();
        return mensaje;
    }

    //Reclamar premio
    @PostMapping("/reclamar-premio/{idEquipo}")
    public String reclamarPremio(@PathVariable Long idEquipo) {
        Equipo equipo = equipoRepository.findById(idEquipo).orElseThrow();
        if (equipo.isReclamado()) {
            String error = "❌ Ya cobrado.";
            return error;
        }
        int p = equipo.getPuntosTotalesJornada();

        int base = 0;
        if (p > 0) {
            base = p * 100_000;
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

        int bonus = 0;
        if (mvp) {
            bonus = 1_000_000;
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

        String mensaje = "💰 Reclamado: " + fmtDinero(base) + mensajeExtra;
        return mensaje;
    }

    //**************************************************************************************************************+***
    //                                                      DELETE
    //**************************************************************************************************************+***

    //ADMIN - Rechazar solicitudes
    @DeleteMapping("/admin/rechazar/{idUsuario}")
    public String rechazarUsuario(@PathVariable Long idUsuario) {
        usuarioRepository.deleteById(idUsuario);
        String mensaje = "🗑️ Solicitud rechazada.";
        return mensaje;
    }

    //ADMIN - Eliminar usuarios
    @DeleteMapping("/admin/eliminar-usuario/{idUsuario}")
    public String eliminarUsuario(@PathVariable Long idUsuario) {
        Usuario u = usuarioRepository.findById(idUsuario).orElseThrow();
        if(u.isEsAdmin()) {
            String error = "❌ No se puede borrar al admin.";
            return error;
        }

        jugadorRepository.findAll().stream().filter(j -> j.getPropietario() != null && j.getPropietario().getId().equals(idUsuario)).forEach(j -> {
            j.setPropietario(null);
            j.setClausula(j.getValor());
            jugadorRepository.save(j);
        });

        equipoRepository.deleteAll(equipoRepository.findByUsuario(u));
        List<Oferta> ofertasRelacionadas = ofertaRepository.findAll().stream()
                .filter(o -> o.getVendedor().getId().equals(idUsuario) || o.getComprador().getId().equals(idUsuario))
                .collect(Collectors.toList());
        ofertaRepository.deleteAll(ofertasRelacionadas);
        usuarioRepository.delete(u);
        String mensaje = "✅ Usuario eliminado correctamente.";
        return mensaje;
    }

    //**************************************************************************************************************+***
    //                                              MÉTODOS AUXILIARES
    //**************************************************************************************************************+***

    private List<Map<String, Object>> mapJugadoresCampo(List<Actuacion> acts, int maxPuntos) {
        List<Map<String, Object>> mapeados = acts.stream().map(a -> {
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
                    "nombre", j.getNombre(),
                    "posicion", j.getPosicion(),
                    "puntos", a.getPuntosTotales(),
                    "imagen", imagen,
                    "mvp", esMvp
            );
        }).collect(Collectors.toList());
        return mapeados;
    }

    private String fmtDinero(int cantidad) {
        String dineroFormateado = NumberFormat.getCurrencyInstance(Locale.of("es", "ES")).format(cantidad);
        return dineroFormateado;
    }

    private Jornada getJornadaActiva() {
        List<Jornada> jornadas = jornadaRepository.findAll();
        Jornada jornadaResultado;

        //Si no hay, se crea la jornada 1
        if (jornadas.isEmpty()) {
            Jornada j1 = new Jornada();
            j1.setNumero(1);
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
        Jornada activa = getJornadaActiva();
        long numero = activa.getNumero();
        return numero;
    }

    //HORARIO MERCADO (21:30 - 10:00 CERRADO)
    private boolean isMercadoCerrado() {
        LocalTime ahora = LocalTime.now(ZoneId.of("Europe/Madrid"));

        //Tramo 1: Desde las 21:30 hasta el final del día
        boolean cerradoNoche = ahora.isAfter(LocalTime.of(21, 30)) || ahora.equals(LocalTime.of(21, 30));

        //Tramo 2: Desde el inicio del día hasta las 10:00
        boolean cerradoManana = ahora.isBefore(LocalTime.of(10, 0));

        //Si estamos en cualquiera de los dos tramos, está cerrado.
        boolean estaCerrado = cerradoNoche || cerradoManana;
        return estaCerrado;
    }

    private int getPesoPosicion(String pos) {
        if (pos == null) {
            return 5;
        }
        int peso;
        switch(pos.toUpperCase()) {
            case "PORTERO":
                peso = 1;
                break;
            case "DEFENSA":
                peso = 2;
                break;
            case "MEDIO":
                peso = 3;
                break;
            case "DELANTERO":
                peso = 4;
                break;
            default:
                peso = 5;
                break;
        }
        return peso;
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
        public Long idJugador; public boolean jugado; public boolean victoria; public boolean derrota;
        public int goles; public int golesEncajados; public int autogoles; public String equipoColor;
    }
}