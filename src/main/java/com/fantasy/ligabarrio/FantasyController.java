package com.fantasy.ligabarrio;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
public class FantasyController {

    private final EquipoRepository equipoRepository;
    private final JugadorRepository jugadorRepository;
    private final UsuarioRepository usuarioRepository;
    private final JornadaRepository jornadaRepository;
    private final ActuacionRepository actuacionRepository;
    private final NoticiaRepository noticiaRepository; // <--- NUEVO
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

    // --- NOTICIAS (NUEVO) ---
    @GetMapping("/noticias")
    public List<Noticia> verNoticias() {
        return noticiaRepository.findAllByOrderByFechaDesc();
    }

    // --- INFO BASICA ---
    @GetMapping("/jornada/actual")
    public int getJornadaActual() { return (int) jornadaRepository.count(); }

    @GetMapping("/usuarios")
    public List<Usuario> verRivales() { return usuarioRepository.findAll(); }

    @GetMapping("/jugadores")
    public List<Jugador> verTodosLosJugadores() { return jugadorRepository.findAll(); }

    @GetMapping("/clasificacion")
    public List<String> verClasificacion() {
        return equipoRepository.findAll().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getPuntosTotalesJornada(), e1.getPuntosTotalesJornada()))
                .map(equipo -> "Pos: " + equipo.getUsuario().getNombre() + " (" + equipo.getPuntosTotalesJornada() + " pts)")
                .collect(Collectors.toList());
    }

    // --- NUEVO: OBTENER ALINEACIÓN ACTUAL ---
    @GetMapping("/alineacion/{usuarioId}")
    public List<Jugador> getAlineacion(@PathVariable Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        List<Equipo> equipos = equipoRepository.findByUsuario(usuario);

        if (equipos.isEmpty()) {
            return List.of(); // Devuelve lista vacía si no hay equipo
        }

        // Devolvemos solo los jugadores titulares (alineados)
        return equipos.get(0).getJugadoresAlineados();
    }

    // --- COMPRAR MERCADO ---
    @PostMapping("/mercado/comprar/{idJugador}/{idUsuario}")
    public String comprarJugadorLibre(@PathVariable Long idJugador, @PathVariable Long idUsuario) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario comprador = usuarioRepository.findById(idUsuario).orElseThrow();

        if (jugador.getPropietario() != null) return "❌ Error: Jugador ocupado.";
        if (comprador.getPresupuesto() < jugador.getValor()) return "❌ Error: Sin dinero.";

        comprador.setPresupuesto(comprador.getPresupuesto() - jugador.getValor());
        jugador.setPropietario(comprador);
        jugador.setClausula(jugador.getValor() * 2);

        usuarioRepository.save(comprador);
        jugadorRepository.save(jugador);

        // REGISTRAR NOTICIA
        noticiaRepository.save(new Noticia("💰 MERCADO: " + comprador.getNombre() + " ha fichado a " + jugador.getNombre() + " por " + jugador.getValor() + "€"));

        return "✅ ¡Fichado!";
    }

    // --- ROBAR (CLAUSULAZO) ---
    @PostMapping("/mercado/robar/{idJugador}/{idLadron}")
    public String robarJugador(@PathVariable Long idJugador, @PathVariable Long idLadron) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario ladron = usuarioRepository.findById(idLadron).orElseThrow();
        Usuario victima = jugador.getPropietario();

        if (victima == null) return "❌ Es libre, fíchalo normal.";
        if (victima.getId().equals(ladron.getId())) return "❌ Auto-robo no permitido.";
        if (ladron.getPresupuesto() < jugador.getClausula()) return "❌ Falta dinero.";

        ladron.setPresupuesto(ladron.getPresupuesto() - jugador.getClausula());
        victima.setPresupuesto(victima.getPresupuesto() + jugador.getClausula());
        jugador.setPropietario(ladron);
        jugador.setClausula((int)(jugador.getClausula() * 1.5)); // Sube cláusula

        usuarioRepository.save(ladron);
        usuarioRepository.save(victima);
        jugadorRepository.save(jugador);

        // REGISTRAR NOTICIA
        noticiaRepository.save(new Noticia("🔥 CLÁUSULAZO: " + ladron.getNombre() + " le ha robado a " + jugador.getNombre() + " a " + victima.getNombre() + " por " + (jugador.getClausula()/1.5) + "€"));

        return "✅ ¡Robo completado!";
    }

    @PostMapping("/alinear/{usuarioId}")
    public String guardarAlineacion(@RequestBody List<Long> idsJugadores, @PathVariable Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        Jornada jornada = jornadaRepository.findById(1L).orElseThrow();
        List<Jugador> seleccionados = jugadorRepository.findAllById(idsJugadores);

        for (Jugador j : seleccionados) {
            if (j.getPropietario() == null || !j.getPropietario().getId().equals(usuarioId)) return "❌ " + j.getNombre() + " no es tuyo.";
        }

        List<Equipo> equipos = equipoRepository.findByUsuario(usuario);
        Equipo equipo = equipos.isEmpty() ? new Equipo(usuario, jornada) : equipos.get(0);
        equipo.setJugadoresAlineados(seleccionados);
        equipoRepository.save(equipo);
        return "✅ Alineación guardada.";
    }

    @PostMapping("/admin/registrar")
    public String registrarPartido(@RequestBody DatosPartido datos) {
        Jugador jugador = jugadorRepository.findById(datos.idJugador).orElseThrow();
        Jornada jornada = jornadaRepository.findById(datos.idJornada).orElseThrow();
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
        Jornada jornada = jornadaRepository.findById(1L).orElseThrow();
        List<Equipo> equipos = equipoRepository.findByJornada(jornada);
        for (Equipo equipo : equipos) {
            int total = calculadora.calcularTotalEquipo(equipo);
            equipo.setPuntosTotalesJornada(total);
            equipoRepository.save(equipo);
        }

        noticiaRepository.save(new Noticia("🏁 JORNADA CERRADA: La clasificación ha sido actualizada."));
        return "✅ Jornada cerrada.";
    }

    // --- GESTIÓN DE USUARIOS (ADMIN) ---

    // 1. Obtener lista de usuarios (Ya tenías /usuarios, usaremos ese)

    // 2. Eliminar usuario y liberar fichas
    @DeleteMapping("/admin/eliminar-usuario/{idUsuario}")
    public String eliminarUsuario(@PathVariable Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (usuario.isEsAdmin()) {
            return "❌ No puedes eliminar al Administrador.";
        }

        // 1. Liberar a sus jugadores (Vuelven al mercado)
        List<Jugador> susJugadores = jugadorRepository.findAll().stream()
                .filter(j -> j.getPropietario() != null && j.getPropietario().getId().equals(idUsuario))
                .collect(Collectors.toList());

        for (Jugador j : susJugadores) {
            j.setPropietario(null); // Se queda libre
            j.setClausula(j.getValor()); // Reseteamos cláusula a valor original (opcional)
            jugadorRepository.save(j);
        }

        // 2. Borrar sus equipos (Alineaciones guardadas)
        List<Equipo> susEquipos = equipoRepository.findByUsuario(usuario);
        equipoRepository.deleteAll(susEquipos);

        // 3. Borrar al usuario
        usuarioRepository.delete(usuario);

        // 4. Noticia pública
        noticiaRepository.save(new Noticia("👮 ADMIN: El manager " + usuario.getNombre() + " ha sido expulsado de la liga. Sus jugadores están libres."));

        return "✅ Usuario " + usuario.getNombre() + " eliminado y jugadores liberados.";
    }
}
