package com.fantasy.ligabarrio;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Collections;
import java.util.Random;
import java.time.LocalDate;

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

    private String fmtDinero(int cantidad) {
        return NumberFormat.getCurrencyInstance(Locale.of("es", "ES")).format(cantidad);
    }

    private Jornada getJornadaActiva() {
        List<Jornada> jornadas = jornadaRepository.findAll();
        if (jornadas.isEmpty()) return jornadaRepository.save(new Jornada());
        return jornadas.get(jornadas.size() - 1);
    }

    private long getNumeroJornadaReal() {
        long c = jornadaRepository.count();
        return c == 0 ? 1 : c;
    }

    // --- AUTH ---
    @PostMapping("/auth/registro")
    public String registrarUsuario(@RequestBody Usuario datos) {
        if (usuarioRepository.findByNombre(datos.getNombre()) != null) return "❌ El nombre ya existe.";
        Usuario nuevo = new Usuario(datos.getNombre(), datos.getPassword(), 100_000_000, false);
        nuevo.setActivo(false); 
        usuarioRepository.save(nuevo);
        noticiaRepository.save(new Noticia("🔔 SOLICITUD: " + datos.getNombre() + " quiere entrar en la liga."));
        return "✅ Solicitud enviada. Espera a que el Admin te acepte.";
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Usuario datos) {
        Usuario user = usuarioRepository.findByNombre(datos.getNombre());
        if (user == null || !user.getPassword().equals(datos.getPassword())) return Map.of("error", "Credenciales incorrectas.");
        if (!user.isActivo()) return Map.of("error", "⛔ Tu cuenta aún no ha sido aprobada por el Admin.");
        return Map.of("id", user.getId(), "nombre", user.getNombre(), "esAdmin", user.isEsAdmin(), "presupuesto", user.getPresupuesto());
    }

    // --- ADMIN USUARIOS ---
    @GetMapping("/admin/pendientes")
    public List<Usuario> verUsuariosPendientes() {
        return usuarioRepository.findAll().stream().filter(u -> !u.isActivo()).collect(Collectors.toList());
    }

    @PostMapping("/admin/aprobar/{idUsuario}")
    public String aprobarUsuario(@PathVariable Long idUsuario) {
        Usuario u = usuarioRepository.findById(idUsuario).orElseThrow();
        u.setActivo(true);
        usuarioRepository.save(u);
        noticiaRepository.save(new Noticia("👋 BIENVENIDA: " + u.getNombre() + " ha entrado a la liga."));
        return "✅ Usuario aprobado.";
    }

    @DeleteMapping("/admin/rechazar/{idUsuario}")
    public String rechazarUsuario(@PathVariable Long idUsuario) {
        usuarioRepository.deleteById(idUsuario);
        return "🗑️ Solicitud rechazada.";
    }

    // --- DATA ---
    @GetMapping("/jornada/actual")
    public long getNumeroJornadaActualEndpoint() { return getNumeroJornadaReal(); }

    @GetMapping("/usuarios")
    public List<Usuario> verRivales() { return usuarioRepository.findAll().stream().filter(Usuario::isActivo).collect(Collectors.toList()); }

    @GetMapping("/jugadores")
    public List<Jugador> verTodosLosJugadores() { return jugadorRepository.findAll(); }

    @GetMapping("/mercado-diario")
    public List<Jugador> getMercadoDiario() {
        List<Jugador> libres = jugadorRepository.findAll().stream().filter(j -> j.getPropietario() == null).collect(Collectors.toList());
        long seed = LocalDate.now().toEpochDay();
        Collections.shuffle(libres, new Random(seed));
        return libres.stream().limit(12).collect(Collectors.toList());
    }

    @GetMapping("/noticias")
    public List<Noticia> verNoticias() { return noticiaRepository.findAllByOrderByFechaDesc(); }

    @GetMapping("/alineacion/{usuarioId}")
    public List<Jugador> getAlineacion(@PathVariable Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        Jornada jornadaActual = getJornadaActiva();
        Optional<Equipo> equipo = equipoRepository.findByUsuario(usuario).stream().filter(e -> e.getJornada().getId().equals(jornadaActual.getId())).findFirst();
        return equipo.map(Equipo::getJugadoresAlineados).orElse(List.of());
    }

    @GetMapping("/historial/{usuarioId}")
    public List<Map<String, Object>> getHistorialUsuario(@PathVariable Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        return equipoRepository.findByUsuario(usuario).stream()
            .sorted((e1, e2) -> Long.compare(e2.getJornada().getId(), e1.getJornada().getId()))
            .map(e -> Map.<String, Object>of("jornadaId", e.getJornada().getId(), "puntos", e.getPuntosTotalesJornada(), "jugadores", e.getJugadoresAlineados()))
            .collect(Collectors.toList());
    }

    @GetMapping("/jugador/{id}/historial-puntos")
    public List<Map<String, Object>> getHistorialPuntosJugador(@PathVariable Long id) {
        Jugador jugador = jugadorRepository.findById(id).orElseThrow();
        return actuacionRepository.findByJugador(jugador).stream()
            .map(a -> Map.<String, Object>of("jornada", a.getJornada().getId(), "puntos", a.getPuntosTotales()))
            .sorted((m1, m2) -> Long.compare((Long)m1.get("jornada"), (Long)m2.get("jornada")))
            .collect(Collectors.toList());
    }

    @GetMapping("/clasificacion")
    public List<Map<String, Object>> verClasificacion() {
        List<Usuario> usuarios = usuarioRepository.findAll().stream().filter(Usuario::isActivo).collect(Collectors.toList());
        List<Jugador> todosJugadores = jugadorRepository.findAll();
        List<Equipo> todosEquipos = equipoRepository.findAll();
        return usuarios.stream().map(u -> {
            int puntosTotales = todosEquipos.stream().filter(e -> e.getUsuario().getId().equals(u.getId())).mapToInt(Equipo::getPuntosTotalesJornada).sum();
            int valorPlantilla = todosJugadores.stream().filter(j -> j.getPropietario() != null && j.getPropietario().getId().equals(u.getId())).mapToInt(Jugador::getValor).sum();
            return Map.<String, Object>of("nombre", u.getNombre(), "puntos", puntosTotales, "valorPlantilla", valorPlantilla);
        }).sorted((m1, m2) -> Integer.compare((int)m2.get("puntos"), (int)m1.get("puntos"))).collect(Collectors.toList());
    }

    @GetMapping("/premios-pendientes/{idUsuario}")
    public List<Map<String, Object>> verPremiosPendientes(@PathVariable Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow();
        Jornada jornadaActual = getJornadaActiva();
        return equipoRepository.findByUsuario(usuario).stream()
                .filter(e -> !e.getJornada().getId().equals(jornadaActual.getId()))
                .filter(e -> !e.isReclamado())
                .map(e -> {
                    int dinero = (e.getPuntosTotalesJornada() > 0) ? e.getPuntosTotalesJornada() * 100_000 : 0;
                    return Map.<String, Object>of("idEquipo", e.getId(), "jornada", e.getJornada().getId(), "puntos", e.getPuntosTotalesJornada(), "dinero", dinero, "dineroFmt", fmtDinero(dinero));
                }).collect(Collectors.toList());
    }

    @PostMapping("/reclamar-premio/{idEquipo}")
    public String reclamarPremio(@PathVariable Long idEquipo) {
        Equipo equipo = equipoRepository.findById(idEquipo).orElseThrow();
        if (equipo.isReclamado()) return "❌ Ya cobrado.";
        int puntos = equipo.getPuntosTotalesJornada();
        int dinero = (puntos > 0) ? puntos * 100_000 : 0;
        Usuario usuario = equipo.getUsuario();
        usuario.setPresupuesto(usuario.getPresupuesto() + dinero);
        equipo.setReclamado(true);
        usuarioRepository.save(usuario);
        equipoRepository.save(equipo);
        return "💰 ¡Has reclamado el premio! (" + fmtDinero(dinero) + ")";
    }

    @PostMapping("/mercado/comprar/{idJugador}/{idUsuario}")
    public String comprarJugadorLibre(@PathVariable Long idJugador, @PathVariable Long idUsuario) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario comprador = usuarioRepository.findById(idUsuario).orElseThrow();
        if (jugador.getPropietario() != null) return "❌ Error: Jugador ya tiene dueño.";
        comprador.setPresupuesto(comprador.getPresupuesto() - jugador.getValor());
        jugador.setPropietario(comprador);
        jugador.setClausula(jugador.getValor());
        usuarioRepository.save(comprador);
        jugadorRepository.save(jugador);
        noticiaRepository.save(new Noticia("💰 MERCADO: " + comprador.getNombre() + " ficha a " + jugador.getNombre() + " por " + fmtDinero(jugador.getValor())));
        return "✅ Fichaje realizado.";
    }

    @PostMapping("/mercado/robar/{idJugador}/{idLadron}")
    public String robarJugador(@PathVariable Long idJugador, @PathVariable Long idLadron) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario ladron = usuarioRepository.findById(idLadron).orElseThrow();
        Usuario victima = jugador.getPropietario();
        if (victima == null) return "❌ Es libre, fíchalo normal.";
        if (victima.getId().equals(ladron.getId())) return "❌ No te puedes robar a ti mismo.";
        int precioRobo = jugador.getClausula();
        ladron.setPresupuesto(ladron.getPresupuesto() - precioRobo);
        victima.setPresupuesto(victima.getPresupuesto() + precioRobo);
        jugador.setPropietario(ladron);
        jugador.setClausula((int)(precioRobo * 1.5));
        usuarioRepository.save(ladron);
        usuarioRepository.save(victima);
        jugadorRepository.save(jugador);
        noticiaRepository.save(new Noticia("🔥 CLÁUSULAZO: El mánager " + ladron.getNombre() + " robó el jugador " + jugador.getNombre() + " al mánager " + victima.getNombre() + " por " + fmtDinero(precioRobo)));
        return "✅ ¡Robo completado!";
    }

    @PostMapping("/mercado/vender/{idJugador}/{idUsuario}")
    public String venderJugador(@PathVariable Long idJugador, @PathVariable Long idUsuario) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario vendedor = usuarioRepository.findById(idUsuario).orElseThrow();
        if (jugador.getPropietario() == null || !jugador.getPropietario().getId().equals(idUsuario)) return "❌ No es tuyo.";
        int ingreso = jugador.getValor() + (jugador.getClausula() - jugador.getValor()) / 2;
        vendedor.setPresupuesto(vendedor.getPresupuesto() + ingreso);
        jugador.setPropietario(null);
        jugador.setClausula(jugador.getValor()); 
        Jornada jornadaActual = getJornadaActiva();
        List<Equipo> equipos = equipoRepository.findByUsuario(vendedor);
        for(Equipo e : equipos) {
            if(e.getJornada().getId().equals(jornadaActual.getId())) { e.getJugadoresAlineados().remove(jugador); equipoRepository.save(e); }
        }
        usuarioRepository.save(vendedor);
        jugadorRepository.save(jugador);
        noticiaRepository.save(new Noticia("👋 VENTA: " + vendedor.getNombre() + " vende a " + jugador.getNombre() + " y recibe " + fmtDinero(ingreso)));
        return "✅ Jugador vendido. Recibes " + fmtDinero(ingreso);
    }

    @PostMapping("/jugador/subir-clausula/{idJugador}/{cantidad}")
    public String subirClausula(@PathVariable Long idJugador, @PathVariable int cantidad) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario propietario = jugador.getPropietario();
        if (cantidad <= 0) return "❌ Cantidad inválida.";
        propietario.setPresupuesto(propietario.getPresupuesto() - cantidad);
        jugador.setClausula(jugador.getClausula() + (cantidad * 2));
        usuarioRepository.save(propietario);
        jugadorRepository.save(jugador);
        return "✅ Blindado. Nueva cláusula: " + fmtDinero(jugador.getClausula());
    }

    @PostMapping("/alinear/{usuarioId}")
    public String guardarAlineacion(@RequestBody List<Long> idsJugadores, @PathVariable Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        Jornada jornada = getJornadaActiva(); 
        List<Jugador> seleccionados = jugadorRepository.findAllById(idsJugadores);
        for (Jugador j : seleccionados) { if (j.getPropietario() == null || !j.getPropietario().getId().equals(usuarioId)) return "❌ " + j.getNombre() + " no es tuyo."; }
        Equipo equipo = equipoRepository.findByUsuario(usuario).stream().filter(e -> e.getJornada().getId().equals(jornada.getId())).findFirst().orElse(new Equipo(usuario, jornada));
        equipo.setJugadoresAlineados(seleccionados);
        equipoRepository.save(equipo);
        return "✅ Alineación guardada para Jornada " + getNumeroJornadaReal();
    }

    // 🔴 PUNTO 7: ACTUALIZACIÓN DE VALOR DE MERCADO AL REGISTRAR
    @PostMapping("/admin/registrar")
    public String registrarPartido(@RequestBody DatosPartido datos) {
        Jugador jugador = jugadorRepository.findById(datos.idJugador).orElseThrow();
        Jornada jornada = getJornadaActiva(); 
        Actuacion actuacion = actuacionRepository.findByJugadorAndJornada(jugador, jornada).orElse(new Actuacion(jugador, jornada));

        actuacion.setVictoria(datos.victoria);
        actuacion.setDerrota(datos.derrota);
        actuacion.setGolesMarcados(datos.goles);
        actuacion.setGolesEncajados(datos.golesEncajados);
        
        // 1. Calculamos puntos
        int puntos = calculadora.calcularPuntos(actuacion);
        actuacion.setPuntosTotales(puntos);
        actuacionRepository.save(actuacion);
        
        // 2. Sumamos al total del jugador
        jugador.setPuntosAcumulados(jugador.getPuntosAcumulados() + puntos);
        
        // 3. 🔴 ACTUALIZAMOS VALOR DE MERCADO
        // Regla: 100.000€ por cada punto (positivo o negativo)
        int cambioValor = puntos * 100_000;
        int nuevoValor = jugador.getValor() + cambioValor;
        
        // 4. Tope mínimo: 150.000€
        if (nuevoValor < 150_000) {
            nuevoValor = 150_000;
        }
        
        jugador.setValor(nuevoValor);
        // OJO: La cláusula NO la tocamos automáticamente (solo sube si el manager paga para blindar)
        // Esto crea juego: si el valor sube mucho y la cláusula se queda baja, ¡hay peligro de robo!

        jugadorRepository.save(jugador);
        
        return "✅ Puntos: " + puntos + " | Nuevo Valor: " + fmtDinero(nuevoValor);
    }

    @PostMapping("/admin/cerrar-jornada")
    public String cerrarJornada() {
        Jornada jornadaActual = getJornadaActiva();
        long numJornadaCerrada = getNumeroJornadaReal();
        List<Equipo> equipos = equipoRepository.findByJornada(jornadaActual);
        StringBuilder resumenPremios = new StringBuilder();

        for (Equipo equipo : equipos) {
            Usuario manager = equipo.getUsuario();
            if (manager.getPresupuesto() < 0) {
                equipo.setPuntosTotalesJornada(0);
                equipoRepository.save(equipo);
                resumenPremios.append("🚫 ").append(manager.getNombre()).append(" (Saldo Negativo - 0 pts)\n");
                continue; 
            }
            int puntos = calculadora.calcularTotalEquipo(equipo);
            if (puntos < 0) puntos = 0; // Para el premio, no restamos
            equipo.setPuntosTotalesJornada(puntos);
            equipoRepository.save(equipo);
            
            if (puntos > 0) {
                resumenPremios.append("✅ ").append(manager.getNombre()).append(": ").append(puntos).append("p\n");
            }
        }
        Jornada nuevaJornada = new Jornada();
        jornadaRepository.save(nuevaJornada);
        noticiaRepository.save(new Noticia("🏁 JORNADA " + numJornadaCerrada + " FINALIZADA.\n" + resumenPremios));
        return "✅ Jornada " + numJornadaCerrada + " cerrada. ¡Arranca la Jornada " + (numJornadaCerrada + 1) + "!";
    }

    @DeleteMapping("/admin/eliminar-usuario/{idUsuario}")
    public String eliminarUsuario(@PathVariable Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow();
        if (usuario.isEsAdmin()) return "❌ No se puede eliminar al admin.";
        jugadorRepository.findAll().stream().filter(j -> j.getPropietario() != null && j.getPropietario().getId().equals(idUsuario)).forEach(j -> { j.setPropietario(null); j.setClausula(j.getValor()); jugadorRepository.save(j); });
        equipoRepository.deleteAll(equipoRepository.findByUsuario(usuario));
        usuarioRepository.delete(usuario);
        noticiaRepository.save(new Noticia("👮 ADMIN: " + usuario.getNombre() + " ha sido expulsado."));
        return "✅ Usuario eliminado.";
    }

    @PostMapping("/admin/reset-liga")
    public String resetearLiga() {
        List<Jugador> jugadores = jugadorRepository.findAll();
        for (Jugador j : jugadores) { j.setPropietario(null); j.setPuntosAcumulados(0); j.setClausula(j.getValor()); }
        jugadorRepository.saveAll(jugadores);
        List<Usuario> usuarios = usuarioRepository.findAll();
        for (Usuario u : usuarios) { u.setPresupuesto(100_000_000); u.setActivo(true); }
        usuarioRepository.saveAll(usuarios);
        equipoRepository.deleteAll();
        actuacionRepository.deleteAll();
        noticiaRepository.deleteAll();
        jornadaRepository.deleteAll();
        jornadaRepository.save(new Jornada()); 
        noticiaRepository.save(new Noticia("☢️ LIGA RESETEADA: ¡Todos empiezan de cero con 100M! ¡A fichar!"));
        return "✅ Liga reseteada. Recarga la página.";
    }
}
