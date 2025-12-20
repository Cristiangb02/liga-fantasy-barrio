package com.fantasy.ligabarrio;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
public class FantasyController {

    private final EquipoRepository equipoRepository;
    private final JugadorRepository jugadorRepository;
    private final UsuarioRepository usuarioRepository;
    private final JornadaRepository jornadaRepository;
    private final ActuacionRepository actuacionRepository;
    private final NoticiaRepository noticiaRepository;
    private final CalculadoraPuntosService calculadora;

    public FantasyController(EquipoRepository equipoRepository, JugadorRepository jugadorRepository, UsuarioRepository usuarioRepository, JornadaRepository jornadaRepository, ActuacionRepository actuacionRepository, NoticiaRepository noticiaRepository, CalculadoraPuntosService calculadora) {
        this.equipoRepository = equipoRepository;
        this.jugadorRepository = jugadorRepository;
        this.usuarioRepository = usuarioRepository;
        this.jornadaRepository = jornadaRepository;
        this.actuacionRepository = actuacionRepository;
        this.noticiaRepository = noticiaRepository;
        this.calculadora = calculadora;
    }

    private Jornada getJornadaActiva() {
        List<Jornada> jornadas = jornadaRepository.findAll();
        if (jornadas.isEmpty()) return jornadaRepository.save(new Jornada());
        return jornadas.get(jornadas.size() - 1);
    }

    @GetMapping("/jornada/actual")
    public long getNumeroJornadaActual() { return getJornadaActiva().getId(); }

    @GetMapping("/usuarios")
    public List<Usuario> verRivales() { return usuarioRepository.findAll(); }

    @GetMapping("/jugadores")
    public List<Jugador> verTodosLosJugadores() { return jugadorRepository.findAll(); }

    @GetMapping("/noticias")
    public List<Noticia> verNoticias() { return noticiaRepository.findAllByOrderByFechaDesc(); }

    @GetMapping("/alineacion/{usuarioId}")
    public List<Jugador> getAlineacion(@PathVariable Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        Jornada jornadaActual = getJornadaActiva();
        Optional<Equipo> equipo = equipoRepository.findByUsuario(usuario).stream()
                .filter(e -> e.getJornada().getId().equals(jornadaActual.getId()))
                .findFirst();
        return equipo.map(Equipo::getJugadoresAlineados).orElse(List.of());
    }

    // --- 🔴 PUNTO 4: CLASIFICACIÓN REAL (PUNTOS Y VALOR DE PLANTILLA) ---
    @GetMapping("/clasificacion")
    public List<Map<String, Object>> verClasificacion() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        List<Jugador> todosJugadores = jugadorRepository.findAll();
        List<Equipo> todosEquipos = equipoRepository.findAll();

        return usuarios.stream().map(u -> {
            // 1. Calcular Puntos Totales (Suma de todas las jornadas)
            int puntosTotales = todosEquipos.stream()
                    .filter(e -> e.getUsuario().getId().equals(u.getId()))
                    .mapToInt(Equipo::getPuntosTotalesJornada)
                    .sum();
            
            // 2. Calcular Valor de Plantilla (Suma del valor de sus jugadores)
            int valorPlantilla = todosJugadores.stream()
                    .filter(j -> j.getPropietario() != null && j.getPropietario().getId().equals(u.getId()))
                    .mapToInt(Jugador::getValor)
                    .sum();

            // Devolvemos un mapa estructurado (JSON)
            return Map.<String, Object>of(
                    "nombre", u.getNombre(),
                    "puntos", puntosTotales,
                    "valorPlantilla", valorPlantilla
            );
        })
        // 3. ORDENAR POR PUNTOS (De mayor a menor)
        .sorted((m1, m2) -> Integer.compare((int)m2.get("puntos"), (int)m1.get("puntos")))
        .collect(Collectors.toList());
    }

    // --- MERCADO ---

    @PostMapping("/mercado/comprar/{idJugador}/{idUsuario}")
    public String comprarJugadorLibre(@PathVariable Long idJugador, @PathVariable Long idUsuario) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario comprador = usuarioRepository.findById(idUsuario).orElseThrow();

        if (jugador.getPropietario() != null) return "❌ Error: Jugador ya tiene dueño.";
        if (comprador.getPresupuesto() < jugador.getValor()) return "❌ Error: Sin dinero.";

        comprador.setPresupuesto(comprador.getPresupuesto() - jugador.getValor());
        jugador.setPropietario(comprador);
        jugador.setClausula(jugador.getValor() * 2);

        usuarioRepository.save(comprador);
        jugadorRepository.save(jugador);
        
        noticiaRepository.save(new Noticia("💰 MERCADO: " + comprador.getNombre() + " ficha a " + jugador.getNombre()));
        return "✅ Fichaje realizado.";
    }

    @PostMapping("/mercado/robar/{idJugador}/{idLadron}")
    public String robarJugador(@PathVariable Long idJugador, @PathVariable Long idLadron) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario ladron = usuarioRepository.findById(idLadron).orElseThrow();
        Usuario victima = jugador.getPropietario();

        if (victima == null) return "❌ Es libre, fíchalo normal.";
        if (victima.getId().equals(ladron.getId())) return "❌ No te puedes robar a ti mismo.";
        if (ladron.getPresupuesto() < jugador.getClausula()) return "❌ Falta dinero.";

        ladron.setPresupuesto(ladron.getPresupuesto() - jugador.getClausula());
        victima.setPresupuesto(victima.getPresupuesto() + jugador.getClausula());
        jugador.setPropietario(ladron);
        jugador.setClausula((int)(jugador.getClausula() * 1.5));

        usuarioRepository.save(ladron);
        usuarioRepository.save(victima);
        jugadorRepository.save(jugador);

        noticiaRepository.save(new Noticia("🔥 CLÁUSULAZO: " + ladron.getNombre() + " roba a " + jugador.getNombre()));
        return "✅ ¡Robo completado!";
    }

    @PostMapping("/mercado/vender/{idJugador}/{idUsuario}")
    public String venderJugador(@PathVariable Long idJugador, @PathVariable Long idUsuario) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario vendedor = usuarioRepository.findById(idUsuario).orElseThrow();

        if (jugador.getPropietario() == null || !jugador.getPropietario().getId().equals(idUsuario)) return "❌ No es tuyo.";

        vendedor.setPresupuesto(vendedor.getPresupuesto() + jugador.getValor());
        jugador.setPropietario(null);
        jugador.setClausula(jugador.getValor() * 2);

        Jornada jornadaActual = getJornadaActiva();
        List<Equipo> equipos = equipoRepository.findByUsuario(vendedor);
        for(Equipo e : equipos) {
            if(e.getJornada().getId().equals(jornadaActual.getId())) {
                e.getJugadoresAlineados().remove(jugador);
                equipoRepository.save(e);
            }
        }

        usuarioRepository.save(vendedor);
        jugadorRepository.save(jugador);
        
        noticiaRepository.save(new Noticia("👋 VENTA: " + vendedor.getNombre() + " vende a " + jugador.getNombre()));
        return "✅ Jugador vendido por " + jugador.getValor() + "€";
    }

    @PostMapping("/jugador/subir-clausula/{idJugador}/{cantidad}")
    public String subirClausula(@PathVariable Long idJugador, @PathVariable int cantidad) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario propietario = jugador.getPropietario();

        if (cantidad <= 0) return "❌ Cantidad inválida.";
        if (propietario.getPresupuesto() < cantidad) return "❌ No tienes suficiente dinero.";

        propietario.setPresupuesto(propietario.getPresupuesto() - cantidad);
        jugador.setClausula(jugador.getClausula() + (cantidad * 2));

        usuarioRepository.save(propietario);
        jugadorRepository.save(jugador);

        return "✅ Blindado. Nueva cláusula: " + jugador.getClausula() + "€";
    }

    // --- ALINEAR Y ADMIN ---

    @PostMapping("/alinear/{usuarioId}")
    public String guardarAlineacion(@RequestBody List<Long> idsJugadores, @PathVariable Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        Jornada jornada = getJornadaActiva(); 
        List<Jugador> seleccionados = jugadorRepository.findAllById(idsJugadores);
        
        for (Jugador j : seleccionados) {
            if (j.getPropietario() == null || !j.getPropietario().getId().equals(usuarioId)) return "❌ " + j.getNombre() + " no es tuyo.";
        }
        
        Equipo equipo = equipoRepository.findByUsuario(usuario).stream()
                .filter(e -> e.getJornada().getId().equals(jornada.getId()))
                .findFirst()
                .orElse(new Equipo(usuario, jornada));
        
        equipo.setJugadoresAlineados(seleccionados);
        equipoRepository.save(equipo);
        return "✅ Alineación guardada para Jornada " + jornada.getId();
    }

    @PostMapping("/admin/registrar")
    public String registrarPartido(@RequestBody DatosPartido datos) {
        Jugador jugador = jugadorRepository.findById(datos.idJugador).orElseThrow();
        Jornada jornada = getJornadaActiva(); 
        Actuacion actuacion = actuacionRepository.findByJugadorAndJornada(jugador, jornada).orElse(new Actuacion(jugador, jornada));

        actuacion.setVictoria(datos.victoria);
        actuacion.setDerrota(datos.derrota);
        actuacion.setGolesMarcados(datos.goles);
        actuacion.setGolesEncajados(datos.golesEncajados);
        
        int puntos = calculadora.calcularPuntos(actuacion);
        actuacion.setPuntosTotales(puntos);
        actuacionRepository.save(actuacion);
        
        jugador.setPuntosAcumulados(jugador.getPuntosAcumulados() + puntos);
        jugadorRepository.save(jugador);
        return "✅ Puntos registrados: " + puntos;
    }

    @PostMapping("/admin/cerrar-jornada")
    public String cerrarJornada() {
        Jornada jornadaActual = getJornadaActiva();
        List<Equipo> equipos = equipoRepository.findByJornada(jornadaActual);
        StringBuilder resumenPremios = new StringBuilder();

        for (Equipo equipo : equipos) {
            Usuario manager = equipo.getUsuario();
            if (manager.getPresupuesto() < 0) {
                equipo.setPuntosTotalesJornada(0);
                equipoRepository.save(equipo);
                resumenPremios.append("🚫 ").append(manager.getNombre()).append(" (Saldo -)\n");
                continue; 
            }
            int puntos = calculadora.calcularTotalEquipo(equipo);
            equipo.setPuntosTotalesJornada(puntos);
            int premioEconomico = puntos * 100_000;
            manager.setPresupuesto(manager.getPresupuesto() + premioEconomico);
            
            usuarioRepository.save(manager);
            equipoRepository.save(equipo);
            if (puntos > 0) {
                resumenPremios.append("💰 ").append(manager.getNombre()).append(": ").append(puntos).append("p -> ").append(premioEconomico/1000).append("k\n");
            }
        }
        
        Jornada nuevaJornada = new Jornada();
        jornadaRepository.save(nuevaJornada);
        noticiaRepository.save(new Noticia("🏁 JORNADA " + jornadaActual.getId() + " FINALIZADA.\n" + resumenPremios));
        return "✅ Jornada " + jornadaActual.getId() + " cerrada. ¡Arranca la Jornada " + nuevaJornada.getId() + "!";
    }

    @DeleteMapping("/admin/eliminar-usuario/{idUsuario}")
    public String eliminarUsuario(@PathVariable Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow();
        if (usuario.isEsAdmin()) return "❌ No se puede eliminar al admin.";

        jugadorRepository.findAll().stream().filter(j -> j.getPropietario() != null && j.getPropietario().getId().equals(idUsuario))
                .forEach(j -> { j.setPropietario(null); j.setClausula(j.getValor()); jugadorRepository.save(j); });

        equipoRepository.deleteAll(equipoRepository.findByUsuario(usuario));
        usuarioRepository.delete(usuario);
        noticiaRepository.save(new Noticia("👮 ADMIN: " + usuario.getNombre() + " ha sido expulsado."));
        return "✅ Usuario eliminado.";
    }

    @PostMapping("/admin/reset-liga")
    public String resetearLiga() {
        List<Jugador> jugadores = jugadorRepository.findAll();
        for (Jugador j : jugadores) {
            j.setPropietario(null);
            j.setPuntosAcumulados(0);
            j.setClausula(j.getValor() * 2); 
        }
        jugadorRepository.saveAll(jugadores);

        List<Usuario> usuarios = usuarioRepository.findAll();
        for (Usuario u : usuarios) {
            u.setPresupuesto(100_000_000); 
        }
        usuarioRepository.saveAll(usuarios);

        equipoRepository.deleteAll();
        actuacionRepository.deleteAll();
        noticiaRepository.deleteAll();
        jornadaRepository.deleteAll();
        jornadaRepository.save(new Jornada()); 

        noticiaRepository.save(new Noticia("☢️ LIGA RESETEADA: ¡Todos empiezan de cero con 100M! ¡A fichar!"));

        return "✅ Liga reseteada a 100M. Jornada 1 lista.";
    }
}
