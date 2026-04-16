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

    @GetMapping("/usuarios")
    public List<Usuario> verRivales() {
        return usuarioRepository.findAll().stream().filter(Usuario::isActivo).collect(Collectors.toList());
    }

    @GetMapping("/jugadores")
    public List<Map<String, Object>> verTodosLosJugadores() {
        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("Europe/Madrid"));
        return jugadorRepository.findAll().stream().map(j -> {
            boolean blindado = j.getFechaFinBlindaje() != null && j.getFechaFinBlindaje().isAfter(ahora);
            long segundosRestantes = blindado ? ChronoUnit.SECONDS.between(ahora, j.getFechaFinBlindaje()) : 0;
            String urlImagen = j.getUrlImagen() != null ? j.getUrlImagen() : "";
            Object propietarioObj = j.getPropietario() != null ? j.getPropietario() : Map.of();

            Map<String, Object> map = new HashMap<>();
            map.put("id", j.getId());
            map.put("nombre", j.getNombre());
            map.put("posicion", j.getPosicion());
            map.put("valor", j.getValor());
            map.put("clausula", j.getClausula());
            map.put("puntosAcumulados", j.getPuntosAcumulados());
            map.put("urlImagen", urlImagen);
            map.put("propietario", propietarioObj);
            map.put("blindado", blindado);
            map.put("segundosBlindaje", segundosRestantes);
            map.put("estado", j.getEstado() != null ? j.getEstado() : "DISPONIBLE");

            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/mercado-diario")
    public List<Jugador> getMercadoDiario() {
        LocalDate hoy = LocalDate.now(ZoneId.of("Europe/Madrid"));
        long seed = hoy.toEpochDay() + fantasyService.getDesplazamiento();
        List<Jugador> todos = jugadorRepository.findAll();

        List<Jugador> candidatosHoy = todos.stream()
                .filter(j -> {
                    if (j.getPropietario() == null) {
                        return j.getFechaVenta() == null || !j.getFechaVenta().isEqual(hoy);
                    }
                    return j.getFechaFichaje() != null && j.getFechaFichaje().isEqual(hoy);
                })
                .sorted(Comparator.comparing(Jugador::getId))
                .collect(Collectors.toList());

        Collections.shuffle(candidatosHoy, new Random(seed));

        return candidatosHoy.stream()
                .limit(14)
                .filter(j -> j.getPropietario() == null)
                .collect(Collectors.toList());
    }

    @GetMapping("/jornada/resumen")
    public List<Map<String, Object>> verResumenJornadaAnterior() {
        Jornada actual = fantasyService.getJornadaActiva();
        int numAnterior = actual.getNumero() - 1;
        if (numAnterior < 1) return new ArrayList<>();

        Optional<Jornada> jOpt = jornadaRepository.findAll().stream().filter(j -> j.getNumero() == numAnterior).findFirst();
        if (jOpt.isEmpty()) return new ArrayList<>();

        Jornada anterior = jOpt.get();
        return equipoRepository.findByJornada(anterior).stream().map(e -> {
            List<Map<String, Object>> jugadores = new ArrayList<>();
            for (Jugador j : e.getJugadoresAlineados()) {
                int pts = actuacionRepository.findByJugadorAndJornada(j, anterior).map(Actuacion::getPuntosTotales).orElse(0);
                jugadores.add(Map.of("nombre", j.getNombre(), "posicion", j.getPosicion(), "puntos", pts, "urlImagen", j.getUrlImagen() != null ? j.getUrlImagen() : ""));
            }
            jugadores.sort((a,b) -> Integer.compare(fantasyService.getPesoPosicion((String)a.get("posicion")), fantasyService.getPesoPosicion((String)b.get("posicion"))));
            return Map.<String, Object>of("manager", e.getUsuario().getNombre(), "puntosTotal", e.getPuntosTotalesJornada(), "jugadores", jugadores);
        }).sorted((a,b) -> Integer.compare((int)b.get("puntosTotal"), (int)a.get("puntosTotal"))).collect(Collectors.toList());
    }

    @GetMapping("/jornada/{numero}/resumen-partido")
    public Map<String, Object> getResumenPartido(@PathVariable int numero) {
        Optional<Jornada> jOpt = jornadaRepository.findAll().stream().filter(j -> j.getNumero() == numero).findFirst();
        if (jOpt.isEmpty()) return Map.of("error", "Jornada no encontrada");

        Jornada jornada = jOpt.get();
        List<Actuacion> actuaciones = actuacionRepository.findAll().stream()
                .filter(a -> a.getJornada().getId().equals(jornada.getId()))
                .collect(Collectors.toList());

        if (actuaciones.isEmpty()) return Map.of("error", "Sin datos en esta jornada");

        int maxPuntos = actuaciones.stream().mapToInt(Actuacion::getPuntosTotales).max().orElse(0);

        // Agrupamos puramente por el color de la camiseta de la tabla Actuacion
        Map<String, List<Actuacion>> grupos = actuaciones.stream()
                .collect(Collectors.groupingBy(a -> a.getColorEquipo() != null ? a.getColorEquipo() : "SIN COLOR"));

        List<String> colores = new ArrayList<>(grupos.keySet());

        List<Actuacion> listaA = new ArrayList<>();
        List<Actuacion> listaB = new ArrayList<>();
        String colorA = "EQUIPO 1";
        String colorB = "EQUIPO 2";

        if (colores.size() >= 2) {
            // CASO IDEAL: Hay dos colores distintos registrados en la base de datos
            colorA = colores.get(0);
            colorB = colores.get(1);
            listaA = grupos.get(colorA);
            listaB = grupos.get(colorB);
        } else if (colores.size() == 1) {
            // SALVAVIDAS: Todos los jugadores tienen el mismo color (o son null).
            // Cortamos la lista por la mitad para obligar al frontend a dibujar uno arriba y otro abajo.
            colorA = colores.get(0) + " 1";
            colorB = colores.get(0) + " 2";
            List<Actuacion> todas = grupos.get(colores.get(0));

            int mitad = todas.size() / 2;
            listaA = todas.subList(0, mitad);
            listaB = todas.subList(mitad, todas.size());
        }

        return Map.of(
                "colorA", colorA,
                "colorB", colorB,
                "equipoA", mapJugadoresCampo(listaA, maxPuntos),
                "equipoB", mapJugadoresCampo(listaB, maxPuntos)
        );
    }

    @GetMapping("/jornada/{numero}/resumen-managers")
    public List<Map<String, Object>> verResumenManagers(@PathVariable int numero) {
        Optional<Jornada> jOpt = jornadaRepository.findAll().stream().filter(j -> j.getNumero() == numero).findFirst();
        if (jOpt.isEmpty()) return new ArrayList<>();

        return equipoRepository.findByJornada(jOpt.get()).stream().map(e -> {
            List<Map<String, Object>> jugadores = new ArrayList<>();
            for (Jugador j : e.getJugadoresAlineados()) {
                int pts = actuacionRepository.findByJugadorAndJornada(j, jOpt.get()).map(Actuacion::getPuntosTotales).orElse(0);
                jugadores.add(Map.of("nombre", j.getNombre(), "posicion", j.getPosicion(), "puntos", pts));
            }
            jugadores.sort((a,b) -> Integer.compare(fantasyService.getPesoPosicion((String)a.get("posicion")), fantasyService.getPesoPosicion((String)b.get("posicion"))));
            return Map.<String, Object>of("manager", e.getUsuario().getNombre(), "puntosTotal", e.getPuntosTotalesJornada(), "jugadores", jugadores);
        }).sorted((a,b) -> Integer.compare((int)b.get("puntosTotal"), (int)a.get("puntosTotal"))).collect(Collectors.toList());
    }

    @GetMapping("/alineacion/{usuarioId}")
    public List<Jugador> getAlineacion(@PathVariable Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        Jornada jornadaActual = fantasyService.getJornadaActiva();
        return equipoRepository.findByUsuario(usuario).stream()
                .filter(e -> e.getJornada().getId().equals(jornadaActual.getId()))
                .findFirst().map(Equipo::getJugadoresAlineados).orElse(List.of());
    }

    @GetMapping("/noticias")
    public List<Noticia> verNoticias() {
        LocalDateTime limite = LocalDateTime.now(ZoneId.of("Europe/Madrid")).minusDays(7);
        List<Noticia> todas = noticiaRepository.findAll();
        List<Noticia> paraBorrar = todas.stream().filter(n -> n.getFecha().isBefore(limite)).collect(Collectors.toList());
        if(!paraBorrar.isEmpty()) noticiaRepository.deleteAll(paraBorrar);
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
        return actuacionRepository.findByJugador(jugador).stream()
                .map(a -> Map.<String, Object>of("jornada", a.getJornada().getNumero(), "puntos", a.getPuntosTotales()))
                .sorted((m1, m2) -> Integer.compare((Integer)m1.get("jornada"), (Integer)m2.get("jornada")))
                .collect(Collectors.toList());
    }

    @GetMapping("/premios-pendientes/{idUsuario}")
    public List<Map<String, Object>> verPremiosPendientes(@PathVariable Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow();
        Jornada jornadaActual = fantasyService.getJornadaActiva();

        return equipoRepository.findByUsuario(usuario).stream()
                .filter(e -> !e.getJornada().getId().equals(jornadaActual.getId()) && !e.isReclamado()).map(e -> {
                    int p = e.getPuntosTotalesJornada();
                    int dinero = p > 0 ? p * 100_000 : 0;
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
                    int bonus = mvp ? 1_000_000 : 0;
                    return Map.<String, Object>of("idEquipo", e.getId(), "jornada", e.getJornada().getNumero(), "puntos", p, "dineroFmt", fantasyService.formatearDinero(dinero + bonus), "tieneMvp", mvp, "nombreMvp", nombreMvp, "bonusFmt", fantasyService.formatearDinero(bonus));
                }).collect(Collectors.toList());
    }

    @GetMapping("/clasificacion")
    public List<Map<String, Object>> verClasificacion() {
        List<Usuario> usuarios = usuarioRepository.findAll().stream().filter(Usuario::isActivo).collect(Collectors.toList());
        List<Jugador> todosJugadores = jugadorRepository.findAll();
        List<Equipo> todosEquipos = equipoRepository.findAll();

        return usuarios.stream().map(u -> {
            int p = todosEquipos.stream().filter(e -> e.getUsuario().getId().equals(u.getId())).mapToInt(Equipo::getPuntosTotalesJornada).sum();
            p += u.getPuntosExtra();

            int v = todosJugadores.stream().filter(j -> j.getPropietario() != null && j.getPropietario().getId().equals(u.getId())).mapToInt(Jugador::getValor).sum();
            return Map.<String, Object>of("nombre", u.getNombre(), "puntos", p, "valorPlantilla", v, "urlImagen", u.getUrlImagen() != null ? u.getUrlImagen() : "");
        }).sorted((m1, m2) -> {
            int cmp = Integer.compare((int)m2.get("puntos"), (int)m1.get("puntos"));
            return cmp != 0 ? cmp : Integer.compare((int)m2.get("valorPlantilla"), (int)m1.get("valorPlantilla"));
        }).collect(Collectors.toList());
    }

    @PostMapping("/usuarios/ping/{idUsuario}")
    public List<String> pingUsuario(@PathVariable Long idUsuario) {
        usuariosOnline.put(idUsuario, LocalDateTime.now());
        LocalDateTime limite = LocalDateTime.now().minusMinutes(5);
        List<String> nombresOnline = new ArrayList<>();
        usuariosOnline.forEach((id, fecha) -> {
            if (fecha.isAfter(limite)) usuarioRepository.findById(id).ifPresent(u -> nombresOnline.add(u.getNombre()));
        });
        return nombresOnline;
    }

    @PostMapping("/alinear/{usuarioId}")
    public String guardarAlineacion(@RequestBody List<Long> idsJugadores, @PathVariable Long usuarioId) {
        if (idsJugadores == null) return "❌ Error: Lista vacía.";
        if (idsJugadores.size() > 7) return "❌ Máximo 7 jugadores.";

        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        Jornada jornada = fantasyService.getJornadaActiva();
        List<Jugador> seleccionados = jugadorRepository.findAllById(idsJugadores);
        for (Jugador j : seleccionados) {
            if (j.getPropietario() == null || !j.getPropietario().getId().equals(usuarioId)) {
                return "❌ " + j.getNombre() + " no te pertenece.";
            }
        }
        Equipo equipo = equipoRepository.findByUsuario(usuario).stream().filter(e -> e.getJornada().getId().equals(jornada.getId())).findFirst().orElse(new Equipo(usuario, jornada));
        equipo.setJugadoresAlineados(seleccionados);
        equipoRepository.save(equipo);
        return "✅ Alineación guardada para la jornada " + fantasyService.getNumeroJornadaReal();
    }

    @PostMapping("/reclamar-premio/{idEquipo}")
    public String reclamarPremio(@PathVariable Long idEquipo) {
        Equipo equipo = equipoRepository.findById(idEquipo).orElseThrow();
        if (equipo.isReclamado()) return "❌ Ya cobrado.";

        int p = equipo.getPuntosTotalesJornada();
        int base = p > 0 ? p * 100_000 : 0;

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

        int bonus = mvp ? 1_000_000 : 0;
        Usuario u = equipo.getUsuario();
        u.setPresupuesto(u.getPresupuesto() + base + bonus);
        equipo.setReclamado(true);
        usuarioRepository.save(u);
        equipoRepository.save(equipo);

        return "💰 Reclamado: " + fantasyService.formatearDinero(base) + (mvp ? " + 🏆 " + fantasyService.formatearDinero(bonus) : "");
    }

    private List<Map<String, Object>> mapJugadoresCampo(List<Actuacion> acts, int maxPuntos) {
        return acts.stream().map(a -> {
            Jugador j = a.getJugador();
            Map<String, Object> map = new HashMap<>();
            map.put("nombre", j.getNombre() != null ? j.getNombre() : "Sin Nombre");
            map.put("posicion", j.getPosicion() != null ? j.getPosicion() : "MED");
            map.put("puntos", a.getPuntosTotales());
            map.put("imagen", j.getUrlImagen() != null ? j.getUrlImagen() : "");
            map.put("mvp", a.getPuntosTotales() == maxPuntos && maxPuntos > 0);
            return map;
        }).collect(Collectors.toList());
    }
}