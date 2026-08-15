package com.fantasy.ligabarrio.controller;

import com.fantasy.ligabarrio.model.*;
import com.fantasy.ligabarrio.repository.*;
import com.fantasy.ligabarrio.service.FantasyService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
public class FantasyController {

    private final EquipoRepository equipoRepository;
    private final JugadorRepository jugadorRepository;
    private final UsuarioRepository usuarioRepository;
    private final JornadaRepository jornadaRepository;
    private final ActuacionRepository actuacionRepository;
    private final NoticiaRepository noticiaRepository;
    private final FantasyService fantasyService;
    private final Map<Long, LocalDateTime> usuariosOnline = new ConcurrentHashMap<>();

    public FantasyController(EquipoRepository equipoRepository, JugadorRepository jugadorRepository,
                             UsuarioRepository usuarioRepository, JornadaRepository jornadaRepository,
                             ActuacionRepository actuacionRepository, NoticiaRepository noticiaRepository,
                             FantasyService fantasyService) {
        this.equipoRepository = equipoRepository;
        this.jugadorRepository = jugadorRepository;
        this.usuarioRepository = usuarioRepository;
        this.jornadaRepository = jornadaRepository;
        this.actuacionRepository = actuacionRepository;
        this.noticiaRepository = noticiaRepository;
        this.fantasyService = fantasyService;
    }

    @GetMapping("/jornada/actual")
    public long getNumeroJornadaActualEndpoint() {
        return fantasyService.getNumeroJornadaReal();
    }

    @GetMapping("/estado-mantenimiento")
    public boolean getEstadoMantenimiento() {
        boolean resultado= fantasyService.isMantenimientoActivo();
        return resultado;
    }

    @GetMapping("/usuarios")
    public List<Usuario> verRivales() {
        return usuarioRepository.findAll().stream().filter(Usuario::isActivo).collect(Collectors.toList());
    }

    @GetMapping("/jugadores")
    public List<Map<String, Object>> verTodosLosJugadores() {
        List<Map<String, Object>> resultado = new ArrayList<>();
        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("Europe/Madrid"));
        List<Jugador> todosLosJugadores = jugadorRepository.findAll();
        long segundosRestantes = 0;
        boolean blindado = false;

        String urlImagen;
        Object propietarioObj;
        String estado;

        for (Jugador j : todosLosJugadores) {

            if (j.getFechaFinBlindaje() != null && j.getFechaFinBlindaje().isAfter(ahora)) {
                blindado = true;
            }
            if (blindado) {
                segundosRestantes = ChronoUnit.SECONDS.between(ahora, j.getFechaFinBlindaje());
            }

            if (j.getUrlImagen() != null) {
                urlImagen = j.getUrlImagen();
            } else {
                urlImagen = "";
            }

            if (j.getPropietario() != null) {
                propietarioObj = j.getPropietario();
            } else {
                propietarioObj = Map.of();
            }

            if (j.getEstado() != null) {
                estado = j.getEstado();
            } else {
                estado = "DISPONIBLE";
            }

            Map<String, Object> mapJugador = new HashMap<>();
            mapJugador.put("id", j.getId());
            mapJugador.put("nombre", j.getNombre());
            mapJugador.put("posicion", j.getPosicion());
            mapJugador.put("valor", j.getValor());
            mapJugador.put("clausula", j.getClausula());
            mapJugador.put("puntosAcumulados", j.getPuntosAcumulados());
            mapJugador.put("urlImagen", urlImagen);
            mapJugador.put("propietario", propietarioObj);
            mapJugador.put("blindado", blindado);
            mapJugador.put("segundosBlindaje", segundosRestantes);
            mapJugador.put("estado", estado);

            resultado.add(mapJugador);
        }
        return resultado;
    }

    @GetMapping("/mercado-diario")
    public List<Jugador> getMercadoDiario() {
        List<Jugador> resultado = new ArrayList<>();

        //Fecha actual, la semilla y todos los jugadores
        LocalDate hoy = LocalDate.now(ZoneId.of("Europe/Madrid"));
        long seed = hoy.toEpochDay() + fantasyService.getDesplazamiento();
        List<Jugador> todos = jugadorRepository.findAll();
        List<Jugador> candidatosHoy = new ArrayList<>();

        for (Jugador j : todos) {
            boolean esCandidato = false;

            if (j.getPropietario() == null) { //Si es libre, entra al sorteo si NO ha sido vendido hoy al mercado
                if (j.getFechaVenta() == null || !j.getFechaVenta().isEqual(hoy)) {
                    esCandidato = true;
                }
            } else { //Si no es libre, entra al sorteo SOLO si ha sido fichado hoy (para mantener el orden)
                if (j.getFechaFichaje() != null && j.getFechaFichaje().isEqual(hoy)) {
                    esCandidato = true;
                }
            }

            if (esCandidato) {
                candidatosHoy.add(j);
            }
        }

        //Ordenamos por ID para que el orden inicial siempre sea exacto antes de barajar
        candidatosHoy.sort(Comparator.comparing(Jugador::getId));

        //Barajamos la lista usando la semilla
        Collections.shuffle(candidatosHoy, new Random(seed));

        for (int i = 0; (i < candidatosHoy.size() && i < 14); i++) {
            Jugador j = candidatosHoy.get(i);

            //Solo lo añadimos al resultado final si sigue libre
            if (j.getPropietario() == null) {
                resultado.add(j);
            }
        }
        return resultado;
    }

    @GetMapping("/jornada/resumen")
    public List<Map<String, Object>> verResumenJornadaAnterior() {
        List<Map<String, Object>> resultado = new ArrayList<>();
        Jornada actual = fantasyService.getJornadaActiva();
        int numAnterior = actual.getNumero() - 1;

        if (numAnterior >= 1) { //Solamente si hay una jornada anterior
            Jornada anterior = null; //Para buscarla
            List<Jornada> todas = jornadaRepository.findAll();

            for (int i = 0; i < todas.size() && anterior == null; i++) {
                if (todas.get(i).getNumero() == numAnterior) {
                    anterior = todas.get(i);
                }
            }

            if (anterior != null) { //Si hemos encontrado la jornada
                List<Equipo> equipos = equipoRepository.findByJornada(anterior);

                for (Equipo e : equipos) {
                    List<Map<String, Object>> jugadores = new ArrayList<>();

                    for (Jugador j : e.getJugadoresAlineados()) {

                        //Obtenemos los puntos de los jugadores
                        int pts = 0;
                        Optional<Actuacion> actuacionOpt = actuacionRepository.findByJugadorAndJornada(j, anterior);
                        if (actuacionOpt.isPresent()) {
                            pts = actuacionOpt.get().getPuntosTotales();
                        }

                        String urlImagen;
                        if (j.getUrlImagen() != null) {
                            urlImagen = j.getUrlImagen();
                        } else {
                            urlImagen = "";
                        }

                        Map<String, Object> mapJugador = new HashMap<>();
                        mapJugador.put("nombre", j.getNombre());
                        mapJugador.put("posicion", j.getPosicion());
                        mapJugador.put("puntos", pts);
                        mapJugador.put("urlImagen", urlImagen);
                        jugadores.add(mapJugador);
                    }

                    //Ordenamos a los jugadores por posición
                    jugadores.sort((a, b) -> Integer.compare(
                            fantasyService.getPesoPosicion((String) a.get("posicion")),
                            fantasyService.getPesoPosicion((String) b.get("posicion"))
                    ));

                    //Empaquetamos el resumen del equipo completo
                    Map<String, Object> mapEquipo = new HashMap<>();
                    mapEquipo.put("manager", e.getUsuario().getNombre());
                    mapEquipo.put("puntosTotal", e.getPuntosTotalesJornada());
                    mapEquipo.put("jugadores", jugadores);
                    resultado.add(mapEquipo);
                }

                //Finalmente, ordenamos los equipos por puntos de mayor a menor
                resultado.sort((a, b) -> Integer.compare(
                        (int) b.get("puntosTotal"),
                        (int) a.get("puntosTotal")
                ));
            }
        }
        return resultado;
    }

    @GetMapping("/jornada/{numero}/resumen-partido")
    public Map<String, Object> getResumenPartido(@PathVariable int numero) {
        Map<String, Object> resultado = new HashMap<>();

        Jornada jornada = null;
        List<Jornada> todasLasJornadas = jornadaRepository.findAll();

        for (int i = 0; i < todasLasJornadas.size() && jornada == null; i++) {
            if (todasLasJornadas.get(i).getNumero() == numero) {
                jornada = todasLasJornadas.get(i);
            }
        }

        if (jornada == null) {
            resultado.put("Error", "Jornada no encontrada");
        } else {
            List<Actuacion> actuaciones = new ArrayList<>();
            List<Actuacion> todasLasActuaciones = actuacionRepository.findAll();

            for (Actuacion a : todasLasActuaciones) {
                if (a.getJornada().getId().equals(jornada.getId())) {
                    actuaciones.add(a);
                }
            }

            if (actuaciones.isEmpty()) {
                resultado.put("error", "Sin datos en esta jornada");

            } else { //Calculamos el MVP
                int maxPuntos = 0;
                for (Actuacion a : actuaciones) {
                    if (a.getPuntosTotales() > maxPuntos) {
                        maxPuntos = a.getPuntosTotales();
                    }
                }

                //Agrupamos por colores
                Map<String, List<Actuacion>> grupos = new HashMap<>();

                for (Actuacion a : actuaciones) {

                    String color;
                    if (a.getColorEquipo() != null) {
                        color = a.getColorEquipo();
                    } else {
                        color = "SIN COLOR";
                    }

                    if (!grupos.containsKey(color)) {
                        grupos.put(color, new ArrayList<>());
                    }
                    grupos.get(color).add(a);
                }

                List<String> colores = new ArrayList<>(grupos.keySet());
                List<Actuacion> listaA = new ArrayList<>();
                List<Actuacion> listaB = new ArrayList<>();
                String colorA = "EQUIPO 1";
                String colorB = "EQUIPO 2";

                if (colores.size() >= 2) {
                    colorA = colores.get(0);
                    colorB = colores.get(1);
                    listaA = grupos.get(colorA);
                    listaB = grupos.get(colorB);

                } else if (colores.size() == 1) {
                    //Si  todos tienen el mismo color o son null, se parten exactamente por la mitad para dibujar 1 arriba y otro abajo
                    colorA = colores.get(0) + " 1";
                    colorB = colores.get(0) + " 2";

                    List<Actuacion> todas = grupos.get(colores.get(0));
                    int mitad = todas.size() / 2;

                    //.subList() para coger un trozo de la lista original
                    listaA = todas.subList(0, mitad);
                    listaB = todas.subList(mitad, todas.size());
                }

                resultado.put("colorA", colorA);
                resultado.put("colorB", colorB);
                resultado.put("equipoA", mapJugadoresCampo(listaA, maxPuntos));
                resultado.put("equipoB", mapJugadoresCampo(listaB, maxPuntos));
            }
        }
        return resultado;
    }

    @GetMapping("/jornada/{numero}/resumen-managers")
    public List<Map<String, Object>> verResumenManagers(@PathVariable int numero) {
        List<Map<String, Object>> resultado = new ArrayList<>();

        Jornada jornadaAVer = null;
        List<Jornada> todasLasJornadas = jornadaRepository.findAll();

        for (int i = 0; i < todasLasJornadas.size() && jornadaAVer == null; i++) {
            if (todasLasJornadas.get(i).getNumero() == numero) {
                jornadaAVer = todasLasJornadas.get(i);
            }
        }

        if (jornadaAVer != null) {
            List<Equipo> equipos = equipoRepository.findByJornada(jornadaAVer);

            for (Equipo e : equipos) {
                List<Map<String, Object>> jugadoresMapeados = new ArrayList<>();

                for (Jugador j : e.getJugadoresAlineados()) {
                    int pts = 0;
                    Optional<Actuacion> actuacionOpt = actuacionRepository.findByJugadorAndJornada(j, jornadaAVer);

                    if (actuacionOpt.isPresent()) {
                        pts = actuacionOpt.get().getPuntosTotales();
                    }

                    Map<String, Object> mapJugador = new HashMap<>();
                    mapJugador.put("nombre", j.getNombre());
                    mapJugador.put("posicion", j.getPosicion());
                    mapJugador.put("puntos", pts);
                    jugadoresMapeados.add(mapJugador);
                }

                jugadoresMapeados.sort((a, b) -> Integer.compare(
                        fantasyService.getPesoPosicion((String) a.get("posicion")),
                        fantasyService.getPesoPosicion((String) b.get("posicion"))
                ));

                Map<String, Object> mapEquipo = new HashMap<>();
                mapEquipo.put("manager", e.getUsuario().getNombre());
                mapEquipo.put("puntosTotal", e.getPuntosTotalesJornada());
                mapEquipo.put("jugadores", jugadoresMapeados);
                resultado.add(mapEquipo);
            }

            //Ordenamos de mayor a menor puntuación
            resultado.sort((a, b) -> Integer.compare(
                    (int) b.get("puntosTotal"),
                    (int) a.get("puntosTotal")
            ));
        }
        return resultado;
    }

    @GetMapping("/alineacion/{usuarioId}")
    public List<Jugador> getAlineacion(@PathVariable Long usuarioId) {
        List<Jugador> resultado = new ArrayList<>();
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        Jornada jornadaActual = fantasyService.getJornadaActiva();

        //Obtenemos el historial completo de equipos de este usuario
        List<Equipo> todosLosEquipos = equipoRepository.findByUsuario(usuario);
        boolean equipoEncontrado = false;

        for (int i = 0; i < todosLosEquipos.size() && !equipoEncontrado; i++) {
            Equipo equipoIteracion = todosLosEquipos.get(i);

            //Si el equipo coincide con la jornada que estamos jugando, guardamos sus jugadores
            if (equipoIteracion.getJornada().getId().equals(jornadaActual.getId())) {
                resultado = equipoIteracion.getJugadoresAlineados();
                equipoEncontrado = true;
            }
        }
        return resultado;
    }

    @GetMapping("/noticias")
    public List<Noticia> verNoticias() {
        List<Noticia> resultado;

        //Calculamos la fecha límite (hace 7 días)
        LocalDateTime limite = LocalDateTime.now(ZoneId.of("Europe/Madrid")).minusDays(7);
        List<Noticia> todas = noticiaRepository.findAll();
        List<Noticia> borrar = new ArrayList<>();

        for (Noticia n : todas) {
            if (n.getFecha().isBefore(limite)) {
                borrar.add(n);
            }
        }

        if (!borrar.isEmpty()) {
            noticiaRepository.deleteAll(borrar);
        }
        resultado = noticiaRepository.findAllByOrderByFechaDesc();
        return resultado;
    }

    @GetMapping("/historial/{usuarioId}")
    public List<Map<String, Object>> getHistorialUsuario(@PathVariable Long usuarioId) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();

        //Obtenemos el historial completo de equipos y lo ordenamos de más reciente a más antiguo
        List<Equipo> historialEquipos = equipoRepository.findByUsuario(usuario);
        historialEquipos.sort((e1, e2) -> Integer.compare(
                e2.getJornada().getNumero(),
                e1.getJornada().getNumero()
        ));

        for (Equipo e : historialEquipos) {
            List<Map<String, Object>> detallesJugadores = new ArrayList<>();

            //Comprobamos que el equipo tenga una lista de alineados
            if (e.getJugadoresAlineados() != null) {
                for (Jugador j : e.getJugadoresAlineados()) {
                    if (j != null) {
                        int puntos = 0;
                        Optional<Actuacion> actuacionOpt = actuacionRepository.findByJugadorAndJornada(j, e.getJornada());
                        if (actuacionOpt.isPresent()) {
                            puntos = actuacionOpt.get().getPuntosTotales();
                        }

                        String nombre;
                        if (j.getNombre() != null) {
                            nombre = j.getNombre();
                        } else {
                            nombre = "Jugador eliminado";
                        }

                        String posicion;
                        if (j.getPosicion() != null) {
                            posicion = j.getPosicion();
                        } else {
                            posicion = "MED";
                        }

                        Map<String, Object> mapJugador = new HashMap<>();
                        mapJugador.put("nombre", nombre);
                        mapJugador.put("posicion", posicion);
                        mapJugador.put("puntos", puntos);
                        detallesJugadores.add(mapJugador);
                    }
                }
            }

            int jornadaNumero = 0;
            if (e.getJornada() != null) {
                jornadaNumero = e.getJornada().getNumero();
            }

            Map<String, Object> mapEquipo = new HashMap<>();
            mapEquipo.put("jornadaNumero", jornadaNumero);
            mapEquipo.put("puntosTotal", e.getPuntosTotalesJornada());
            mapEquipo.put("jugadores", detallesJugadores);
            resultado.add(mapEquipo);
        }
        return resultado;
    }

    @GetMapping("/jugador/{id}/historial-puntos")
    public List<Map<String, Object>> getHistorialPuntosJugador(@PathVariable Long id) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        Jugador jugador = jugadorRepository.findById(id).orElseThrow();
        List<Actuacion> historialActuaciones = actuacionRepository.findByJugador(jugador);

        for (Actuacion a : historialActuaciones) {
            Map<String, Object> datos = new HashMap<>();
            datos.put("jornada", a.getJornada().getNumero());
            datos.put("puntos", a.getPuntosTotales());
            resultado.add(datos);
        }

        //Ordenamos la lista cronológicamente (de la jornada 1 en adelante)
        resultado.sort((m1, m2) -> Integer.compare(
                (Integer) m1.get("jornada"),
                (Integer) m2.get("jornada")
        ));
        return resultado;
    }

    @GetMapping("/premios-pendientes/{idUsuario}")
    public List<Map<String, Object>> verPremiosPendientes(@PathVariable Long idUsuario) {
        List<Map<String, Object>> resultado = new ArrayList<>();
        Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow();
        Jornada jornadaActual = fantasyService.getJornadaActiva();
        List<Equipo> equiposUsuario = equipoRepository.findByUsuario(usuario);

        for (Equipo e : equiposUsuario) {
            //Solo evaluamos jornadas pasadas que NO hayan sido reclamadas
            if (!e.getJornada().getId().equals(jornadaActual.getId()) && !e.isReclamado()) {

                int p = e.getPuntosTotalesJornada();
                int dinero;
                if (p > 0) {
                    dinero = p * 100_000;
                } else {
                    dinero = 0;
                }

                //Calculamos el MVP
                int maxPuntos = 0;
                List<Actuacion> todasLasActuaciones = actuacionRepository.findAll();
                for (Actuacion a : todasLasActuaciones) {
                    if (a.getJornada().getId().equals(e.getJornada().getId())) {
                        if (a.getPuntosTotales() > maxPuntos) {
                            maxPuntos = a.getPuntosTotales();
                        }
                    }
                }

                boolean mvp = false;
                String nombreMvp = "";

                if (maxPuntos > 0) {
                    List<Jugador> alineacion = e.getJugadoresAlineados();

                    for (int i = 0; i < alineacion.size() && !mvp; i++) {
                        Jugador j = alineacion.get(i);
                        Optional<Actuacion> actOpt = actuacionRepository.findByJugadorAndJornada(j, e.getJornada());

                        if (actOpt.isPresent() && actOpt.get().getPuntosTotales() == maxPuntos) {
                            mvp = true;
                            nombreMvp = j.getNombre();
                        }
                    }
                }

                int bonus;
                if (mvp) {
                    bonus = 1_000_000;
                } else {
                    bonus = 0;
                }

                Map<String, Object> mapPremio = new HashMap<>();
                mapPremio.put("idEquipo", e.getId());
                mapPremio.put("jornada", e.getJornada().getNumero());
                mapPremio.put("puntos", p);
                mapPremio.put("dineroFmt", fantasyService.formatearDinero(dinero + bonus));
                mapPremio.put("tieneMvp", mvp);
                mapPremio.put("nombreMvp", nombreMvp);
                mapPremio.put("bonusFmt", fantasyService.formatearDinero(bonus));
                resultado.add(mapPremio);
            }
        }
        return resultado;
    }

    @GetMapping("/clasificacion")
    public List<Map<String, Object>> verClasificacion() {
        List<Map<String, Object>> resultado = new ArrayList<>();
        List<Usuario> todosLosUsuarios = usuarioRepository.findAll();
        List<Equipo> todosEquipos = equipoRepository.findAll();
        List<Jugador> todosJugadores = jugadorRepository.findAll();

        for (Usuario u : todosLosUsuarios) {

            if (u.isActivo()) {
                int puntos = u.getPuntosExtra();
                for (Equipo e : todosEquipos) {
                    if (e.getUsuario().getId().equals(u.getId())) {
                        puntos = puntos + e.getPuntosTotalesJornada();
                    }
                }

                int valorPlantilla = 0;
                for (Jugador j : todosJugadores) {
                    if (j.getPropietario() != null && j.getPropietario().getId().equals(u.getId())) {
                        valorPlantilla = valorPlantilla + j.getValor();
                    }
                }

                String urlImagen;
                if (u.getUrlImagen() != null) {
                    urlImagen = u.getUrlImagen();
                } else {
                    urlImagen = "";
                }

                Map<String, Object> mapUsuario = new HashMap<>();
                mapUsuario.put("nombre", u.getNombre());
                mapUsuario.put("puntos", puntos);
                mapUsuario.put("valorPlantilla", valorPlantilla);
                mapUsuario.put("urlImagen", urlImagen);
                resultado.add(mapUsuario);
            }
        }

        //Ordenamos la clasificación global
        resultado.sort((m1, m2) -> {
            int cmp = Integer.compare((int) m2.get("puntos"), (int) m1.get("puntos"));
            if (cmp != 0) {
                return cmp;
            } else { //En caso de empate, desempata el valor de plantilla
                return Integer.compare((int) m2.get("valorPlantilla"), (int) m1.get("valorPlantilla"));
            }
        });
        return resultado;
    }

    @PostMapping("/usuarios/ping/{idUsuario}")
    public List<String> pingUsuario(@PathVariable Long idUsuario) {
        List<String> resultado = new ArrayList<>();
        usuariosOnline.put(idUsuario, LocalDateTime.now());
        LocalDateTime limite = LocalDateTime.now().minusMinutes(5);

        for (Map.Entry<Long, LocalDateTime> entrada : usuariosOnline.entrySet()) {
            Long id = entrada.getKey();
            LocalDateTime fechaUltimaConexion = entrada.getValue();

            if (fechaUltimaConexion.isAfter(limite)) {
                Optional<Usuario> usuarioOpt = usuarioRepository.findById(id);

                if (usuarioOpt.isPresent()) {
                    resultado.add(usuarioOpt.get().getNombre());
                }
            }
        }
        return resultado;
    }

    @PostMapping("/alinear/{usuarioId}")
    public String guardarAlineacion(@RequestBody List<Long> idsJugadores, @PathVariable Long usuarioId) {
        String resultado;

        if (idsJugadores == null) {
            resultado = "❌ Error: Lista vacía.";

        } else if (idsJugadores.size() > 7) {
            resultado = "❌ Máximo 7 jugadores.";

        } else {

            Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
            Jornada jornada = fantasyService.getJornadaActiva();
            List<Jugador> seleccionados = jugadorRepository.findAllById(idsJugadores);
            boolean propiedadValida = true;
            String nombreInvalido = "";

            for (int i = 0; i < seleccionados.size() && propiedadValida; i++) {
                Jugador j = seleccionados.get(i);

                if (j.getPropietario() == null || !j.getPropietario().getId().equals(usuarioId)) {
                    propiedadValida = false;
                    nombreInvalido = j.getNombre();
                }
            }

            if (!propiedadValida) {
                resultado = "❌ " + nombreInvalido + " no te pertenece.";

            } else {
                List<Equipo> historialEquipos = equipoRepository.findByUsuario(usuario);
                Equipo equipoActual = null;

                for (int i = 0; i < historialEquipos.size() && equipoActual == null; i++) {
                    if (historialEquipos.get(i).getJornada().getId().equals(jornada.getId())) {
                        equipoActual = historialEquipos.get(i);
                    }
                }

                if (equipoActual == null) {
                    equipoActual = new Equipo(usuario, jornada);
                }

                equipoActual.setJugadoresAlineados(seleccionados);
                equipoRepository.save(equipoActual);
                resultado = "✅ Alineación guardada para la jornada " + fantasyService.getNumeroJornadaReal();
            }
        }
        return resultado;
    }

    @PostMapping("/reclamar-premio/{idEquipo}")
    public String reclamarPremio(@PathVariable Long idEquipo) {
        String resultado;
        Equipo equipo = equipoRepository.findById(idEquipo).orElseThrow();

        if (equipo.isReclamado()) {
            resultado = "❌ Ya cobrado.";
        } else {
            int p = equipo.getPuntosTotalesJornada();
            int base;
            if (p > 0) {
                base = p * 100_000;
            } else {
                base = 0;
            }

            int maxPuntos = 0;
            List<Actuacion> todasLasActuaciones = actuacionRepository.findAll();

            for (Actuacion a : todasLasActuaciones) {
                if (a.getJornada().getId().equals(equipo.getJornada().getId())) {
                    if (a.getPuntosTotales() > maxPuntos) {
                        maxPuntos = a.getPuntosTotales();
                    }
                }
            }

            //Comprobamos si el mánager tiene al MVP en su alineación
            boolean mvp = false;
            if (maxPuntos > 0) {
                List<Jugador> alineacion = equipo.getJugadoresAlineados();

                for (int i = 0; i < alineacion.size() && !mvp; i++) {
                    Jugador j = alineacion.get(i);
                    Optional<Actuacion> actOpt = actuacionRepository.findByJugadorAndJornada(j, equipo.getJornada());

                    if (actOpt.isPresent() && actOpt.get().getPuntosTotales() == maxPuntos) {
                        mvp = true;
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

            String mensajeExito = "💰 Reclamado: " + fantasyService.formatearDinero(base);
            if (mvp) {
                mensajeExito = mensajeExito + " + 🏆 " + fantasyService.formatearDinero(bonus);
            }
            resultado = mensajeExito;
        }
        return resultado;
    }

    //MÉTODO AUXILIAR
    private List<Map<String, Object>> mapJugadoresCampo(List<Actuacion> acts, int maxPuntos) {
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Actuacion a : acts) {
            Jugador j = a.getJugador();
            String nombre;
            if (j.getNombre() != null) {
                nombre = j.getNombre();
            } else {
                nombre = "Sin Nombre";
            }

            String posicion;
            if (j.getPosicion() != null) {
                posicion = j.getPosicion();
            } else {
                posicion = "MED";
            }

            String imagen;
            if (j.getUrlImagen() != null) {
                imagen = j.getUrlImagen();
            } else {
                imagen = "";
            }

            boolean esMvp = false;
            if (a.getPuntosTotales() == maxPuntos && maxPuntos > 0) {
                esMvp = true;
            }

            Map<String, Object> mapJugador = new HashMap<>();
            mapJugador.put("nombre", nombre);
            mapJugador.put("posicion", posicion);
            mapJugador.put("puntos", a.getPuntosTotales());
            mapJugador.put("imagen", imagen);
            mapJugador.put("mvp", esMvp);
            resultado.add(mapJugador);
        }
        return resultado;
    }
}