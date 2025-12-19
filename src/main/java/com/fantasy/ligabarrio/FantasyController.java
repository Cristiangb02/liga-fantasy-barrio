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

    // --- INFO ---
    @GetMapping("/jornada/actual")
    public int getJornadaActual() { return (int) jornadaRepository.count(); }

    @GetMapping("/usuarios")
    public List<Usuario> verRivales() { return usuarioRepository.findAll(); }

    @GetMapping("/jugadores")
    public List<Jugador> verTodosLosJugadores() { return jugadorRepository.findAll(); }

    @GetMapping("/noticias")
    public List<Noticia> verNoticias() { return noticiaRepository.findAllByOrderByFechaDesc(); }

    @GetMapping("/alineacion/{usuarioId}")
    public List<Jugador> getAlineacion(@PathVariable Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        List<Equipo> equipos = equipoRepository.findByUsuario(usuario);
        return equipos.isEmpty() ? List.of() : equipos.get(0).getJugadoresAlineados();
    }

    @GetMapping("/clasificacion")
    public List<String> verClasificacion() {
        return equipoRepository.findAll().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getPuntosTotalesJornada(), e1.getPuntosTotalesJornada()))
                .map(equipo -> "Pos: " + equipo.getUsuario().getNombre() + " (" + equipo.getPuntosTotalesJornada() + " pts)")
                .collect(Collectors.toList());
    }

    // --- OPERACIONES DE MERCADO ---

    // 1. FICHAR DEL MERCADO
    @PostMapping("/mercado/comprar/{idJugador}/{idUsuario}")
    public String comprarJugadorLibre(@PathVariable Long idJugador, @PathVariable Long idUsuario) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario comprador = usuarioRepository.findById(idUsuario).orElseThrow();

        if (jugador.getPropietario() != null) return "❌ Error: Jugador ya tiene dueño.";
        if (comprador.getPresupuesto() < jugador.getValor()) return "❌ Error: Sin dinero.";

        comprador.setPresupuesto(comprador.getPresupuesto() - jugador.getValor());
        jugador.setPropietario(comprador);
        jugador.setClausula(jugador.getValor() * 2); // Cláusula inicial

        usuarioRepository.save(comprador);
        jugadorRepository.save(jugador);
        
        noticiaRepository.save(new Noticia("💰 MERCADO: " + comprador.getNombre() + " ficha a " + jugador.getNombre() + " por " + jugador.getValor() + "€"));
        return "✅ Fichaje realizado.";
    }

    // 2. ROBAR (CLÁUSULAZO)
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
        jugador.setClausula((int)(jugador.getClausula() * 1.5)); // Sube al cambiar de manos

        usuarioRepository.save(ladron);
        usuarioRepository.save(victima);
        jugadorRepository.save(jugador);

        noticiaRepository.save(new Noticia("🔥 CLÁUSULAZO: " + ladron.getNombre() + " roba a " + jugador.getNombre() + " de " + victima.getNombre()));
        return "✅ ¡Robo completado!";
    }

    // 3. VENDER AL MERCADO (NUEVO)
    @PostMapping("/mercado/vender/{idJugador}/{idUsuario}")
    public String venderJugador(@PathVariable Long idJugador, @PathVariable Long idUsuario) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario vendedor = usuarioRepository.findById(idUsuario).orElseThrow();

        if (jugador.getPropietario() == null || !jugador.getPropietario().getId().equals(idUsuario)) {
            return "❌ Ese jugador no es tuyo.";
        }

        // Recuperas el valor de mercado
        vendedor.setPresupuesto(vendedor.getPresupuesto() + jugador.getValor());
        jugador.setPropietario(null); // Queda libre
        jugador.setClausula(jugador.getValor() * 2); // Reseteamos cláusula

        // Si estaba alineado, lo quitamos de la alineación
        List<Equipo> equipos = equipoRepository.findByUsuario(vendedor);
        if (!equipos.isEmpty()) {
            Equipo equipo = equipos.get(0);
            equipo.getJugadoresAlineados().remove(jugador);
            equipoRepository.save(equipo);
        }

        usuarioRepository.save(vendedor);
        jugadorRepository.save(jugador);
        
        noticiaRepository.save(new Noticia("👋 VENTA: " + vendedor.getNombre() + " vende a " + jugador.getNombre() + " al mercado."));
        return "✅ Jugador vendido por " + jugador.getValor() + "€";
    }

    // 4. SUBIR CLÁUSULA (NUEVO)
    @PostMapping("/jugador/subir-clausula/{idJugador}/{cantidad}")
    public String subirClausula(@PathVariable Long idJugador, @PathVariable int cantidad) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario propietario = jugador.getPropietario();

        if (cantidad <= 0) return "❌ Cantidad inválida.";
        if (propietario.getPresupuesto() < cantidad) return "❌ No tienes suficiente dinero.";

        // Pagas la inversión
        propietario.setPresupuesto(propietario.getPresupuesto() - cantidad);
        // La cláusula sube el DOBLE de lo invertido
        jugador.setClausula(jugador.getClausula() + (cantidad * 2));

        usuarioRepository.save(propietario);
        jugadorRepository.save(jugador);

        return "✅ Blindaje exitoso. Nueva cláusula: " + jugador.getClausula() + "€";
    }

    // --- ALINEAR Y ADMIN ---

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

    // --- CERRAR JORNADA (LÓGICA ECONÓMICA MEJORADA) ---
    @PostMapping("/admin/cerrar-jornada")
    public String cerrarJornada() {
        Jornada jornada = jornadaRepository.findById(1L).orElseThrow();
        List<Equipo> equipos = equipoRepository.findByJornada(jornada);
        
        StringBuilder resumenPremios = new StringBuilder();

        for (Equipo equipo : equipos) {
            Usuario manager = equipo.getUsuario();
            
            // 1. REGLA DE ORO: Si saldo < 0, no puntúa
            if (manager.getPresupuesto() < 0) {
                equipo.setPuntosTotalesJornada(0);
                equipoRepository.save(equipo);
                resumenPremios.append("🚫 ").append(manager.getNombre()).append(" (Saldo negativo)\n");
                continue; 
            }

            // 2. Calcular Puntos
            int puntos = calculadora.calcularTotalEquipo(equipo);
            equipo.setPuntosTotalesJornada(puntos);
            
            // 3. Pagar Premios (1 punto = 100.000 €) -> 32 puntos = 3.200.000 €
            int premioEconomico = puntos * 100_000;
            manager.setPresupuesto(manager.getPresupuesto() + premioEconomico);
            
            usuarioRepository.save(manager);
            equipoRepository.save(equipo);
            
            if (puntos > 0) {
                resumenPremios.append("💰 ").append(manager.getNombre()).append(": ").append(puntos).append(" pts -> ").append(premioEconomico/1_000_000).append("M €\n");
            }
        }
        
        noticiaRepository.save(new Noticia("🏁 JORNADA FINALIZADA. Premios repartidos:\n" + resumenPremios));
        return "✅ Jornada cerrada y primas repartidas.";
    }

    // Eliminar usuario
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
}
