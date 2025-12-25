
package com.fantasy.ligabarrio;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.ArrayList; 
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Optional;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Collections;
import java.util.Random;
import java.time.LocalDate;
import java.time.ZoneId;

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
        if (jornadas.isEmpty()) {
            Jornada j1 = new Jornada();
            j1.setNumero(1); 
            return jornadaRepository.save(j1);
        }
        
        Jornada activa = jornadas.get(jornadas.size() - 1);
        if (activa.getNumero() <= 0) {
            activa.setNumero(1);
            jornadaRepository.save(activa);
        }
        return activa;
    }

    private long getNumeroJornadaReal() {
        return getJornadaActiva().getNumero();
    }

    // --- AUTH ---
    @PostMapping("/auth/registro")
    public String registrarUsuario(@RequestBody Usuario datos) {
        if (usuarioRepository.findByNombre(datos.getNombre()) != null) return "❌ El nombre ya existe.";
        boolean esPrimero = usuarioRepository.count() == 0;
        Usuario nuevo = new Usuario(datos.getNombre(), datos.getPassword(), 100_000_000, esPrimero);
        nuevo.setActivo(esPrimero); 
        usuarioRepository.save(nuevo);
        
        if (esPrimero) {
            noticiaRepository.save(new Noticia("👑 FUNDADOR: " + datos.getNombre() + " ha inaugurado la liga como Admin."));
            return "✅ ¡Liga inaugurada! Eres el Admin.";
        } else {
            noticiaRepository.save(new Noticia("🔔 SOLICITUD: " + datos.getNombre() + " quiere entrar en la liga."));
            return "✅ Solicitud enviada. Espera a que el Admin te acepte.";
        }
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

    @GetMapping("/admin/jugadores-pendientes")
    public List<Jugador> getJugadoresPendientes() {
        Jornada actual = getJornadaActiva();
        List<Jugador> todos = jugadorRepository.findAll();
        
        return todos.stream()
            .filter(j -> actuacionRepository.findByJugadorAndJornada(j, actual).isEmpty()) // Solo los que faltan por puntuar
            .sorted((j1, j2) -> {
                // 1. Ordenar por Posición
                int p1 = getPrioridadPosicion(j1.getPosicion());
                int p2 = getPrioridadPosicion(j2.getPosicion());
                if (p1 != p2) return Integer.compare(p1, p2);
                // 2. Ordenar por Nombre Alfabético
                return j1.getNombre().compareToIgnoreCase(j2.getNombre());
            })
            .collect(Collectors.toList());
    }
    
    // Auxiliar para ordenar posiciones
    private int getPrioridadPosicion(String pos) {
        if (pos == null) return 99;
        switch (pos.toUpperCase()) {
            case "PORTERO": return 1;
            case "DEFENSA": return 2;
            case "MEDIO": return 3;
            case "DELANTERO": return 4;
            default: return 99;
        }
    }
    
    // Auxiliar para ordenar en el resumen del partido (Lambda reference)
    private int compararPorPosicion(Map<String, Object> m1, Map<String, Object> m2) {
        return Integer.compare(getPrioridadPosicion((String)m1.get("posicion")), getPrioridadPosicion((String)m2.get("posicion")));
    }

    // 🔴 MERCADO FIJO DIARIO (CORREGIDO ERROR DE LONG COMPARISON)
    @GetMapping("/mercado-diario")
    public List<Jugador> getMercadoDiario() {
        Jornada jornadaActual = getJornadaActiva();
        List<Jugador> todos = jugadorRepository.findAll();
        List<Jugador> candidatosHoy = new ArrayList<>();

        // 1. IDENTIFICAR QUIÉNES FORMAN EL MERCADO HOY (SNAPSHOT)
        for (Jugador j : todos) {
            boolean esLibre = (j.getPropietario() == null);
            
            // CORRECCIÓN CRÍTICA: Usamos .equals() para comparar IDs (Objetos Long)
            boolean fichadoHoy = (j.getPropietario() != null 
                                  && j.getJornadaFichaje() != null 
                                  && j.getJornadaFichaje().equals(jornadaActual.getId()));

            if (esLibre || fichadoHoy) {
                candidatosHoy.add(j);
            }
        }

        // 2. ORDENAR POR ID (Para consistencia antes de barajar)
        candidatosHoy.sort((j1, j2) -> Long.compare(j1.getId(), j2.getId()));

        // 3. Barajar con la semilla fija del día
        long seed = LocalDate.now(ZoneId.of("Europe/Madrid")).toEpochDay() + jornadaActual.getId();
        Collections.shuffle(candidatosHoy, new Random(seed));

        // 4. Seleccionar el "Escaparate Fijo" de 14
        int limite = Math.min(candidatosHoy.size(), 14);
        List<Jugador> escaparateFijo = candidatosHoy.subList(0, limite);

        // 5. Mostrar SOLO los que siguen libres
        // Si alguien compró a uno de los 14, se filtra aquí y NO entra nadie a reemplazarlo.
        return escaparateFijo.stream()
                .filter(j -> j.getPropietario() == null)
                .collect(Collectors.toList());
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
            .sorted((e1, e2) -> Integer.compare(e2.getJornada().getNumero(), e1.getJornada().getNumero())) 
            .map(e -> {
                List<Map<String, Object>> detallesJugadores = new ArrayList<>();
                for (Jugador j : e.getJugadoresAlineados()) {
                    int puntosJugador = 0;
                    Optional<Actuacion> act = actuacionRepository.findByJugadorAndJornada(j, e.getJornada());
                    if (act.isPresent()) {
                        puntosJugador = act.get().getPuntosTotales();
                    }
                    detallesJugadores.add(Map.of(
                        "nombre", j.getNombre(),
                        "posicion", j.getPosicion(),
                        "puntos", puntosJugador
                    ));
                }
                return Map.<String, Object>of(
                    "jornadaNumero", e.getJornada().getNumero(), 
                    "puntosTotal", e.getPuntosTotalesJornada(),
                    "jugadores", detallesJugadores
                );
            })
            .collect(Collectors.toList());
    }

    @GetMapping("/jugador/{id}/historial-puntos")
    public List<Map<String, Object>> getHistorialPuntosJugador(@PathVariable Long id) {
        Jugador jugador = jugadorRepository.findById(id).orElseThrow();
        return actuacionRepository.findByJugador(jugador).stream()
            .map(a -> Map.<String, Object>of(
                "jornada", a.getJornada().getNumero(),
                "puntos", a.getPuntosTotales()
            ))
            .sorted((m1, m2) -> Integer.compare((Integer)m1.get("jornada"), (Integer)m2.get("jornada")))
            .collect(Collectors.toList());
    }
    
    // RESUMEN VISUAL DEL PARTIDO POR COLORES (ALINEACIONES)
    @GetMapping("/jornada/{numero}/resumen-partido")
    public Map<String, Object> getResumenPartido(@PathVariable int numero) {
        Jornada jornada = jornadaRepository.findAll().stream()
                .filter(j -> j.getNumero() == numero).findFirst().orElse(null);
        if (jornada == null) return Map.of("error", "Jornada no encontrada");

        List<Actuacion> actuaciones = actuacionRepository.findAll().stream()
                .filter(a -> a.getJornada().getId().equals(jornada.getId()))
                .filter(a -> a.getEquipoColor() != null) // Solo los que tengan color asignado
                .collect(Collectors.toList());

        List<String> coloresUsados = actuaciones.stream()
                .map(Actuacion::getEquipoColor)
                .distinct()
                .collect(Collectors.toList());

        if (coloresUsados.isEmpty()) return Map.of("error", "Sin datos de equipos para esta jornada");
        
        String colorA = coloresUsados.get(0);
        String colorB = (coloresUsados.size() > 1) ? coloresUsados.get(1) : "RIVAL";

        List<Map<String, Object>> equipoA = new ArrayList<>();
        List<Map<String, Object>> equipoB = new ArrayList<>();

        for (Actuacion a : actuaciones) {
            Map<String, Object> dato = Map.of(
                "nombre", a.getJugador().getNombre(),
                "posicion", a.getJugador().getPosicion(),
                "puntos", a.getPuntosTotales(),
                "goles", a.getGolesMarcados(),
                "imagen", a.getJugador().getUrlImagen(), 
                "mvp", (a.getPuntosTotales() >= 10),
                "color", a.getEquipoColor()
            );

            if (a.getEquipoColor().equalsIgnoreCase(colorA)) {
                equipoA.add(dato);
            } else {
                equipoB.add(dato);
            }
        }

        equipoA.sort(this::compararPorPosicion);
        equipoB.sort(this::compararPorPosicion);

        return Map.of(
            "jornada", numero,
            "equipoA", equipoA,
            "colorA", colorA,
            "equipoB", equipoB,
            "colorB", colorB
        );
    }

    @GetMapping("/clasificacion")
    public List<Map<String, Object>> verClasificacion() {
        List<Usuario> usuarios = usuarioRepository.findAll().stream().filter(Usuario::isActivo).collect(Collectors.toList());
        List<Jugador> todosJugadores = jugadorRepository.findAll();
        List<Equipo> todosEquipos = equipoRepository.findAll();
        
        return usuarios.stream().map(u -> {
            int puntosTotales = todosEquipos.stream()
                .filter(e -> e.getUsuario().getId().equals(u.getId()))
                .mapToInt(Equipo::getPuntosTotalesJornada).sum();
                
            int valorPlantilla = todosJugadores.stream()
                .filter(j -> j.getPropietario() != null && j.getPropietario().getId().equals(u.getId()))
                .mapToInt(Jugador::getValor).sum();
                
            return Map.<String, Object>of("nombre", u.getNombre(), "puntos", puntosTotales, "valorPlantilla", valorPlantilla);
        }).sorted((m1, m2) -> {
            int p1 = (int) m1.get("puntos");
            int p2 = (int) m2.get("puntos");
            if (p1 != p2) return Integer.compare(p2, p1);
            int v1 = (int) m1.get("valorPlantilla");
            int v2 = (int) m2.get("valorPlantilla");
            return Integer.compare(v2, v1);
        }).collect(Collectors.toList());
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
                    return Map.<String, Object>of("idEquipo", e.getId(), "jornada", e.getJornada().getNumero(), "puntos", e.getPuntosTotalesJornada(), "dinero", dinero, "dineroFmt", fmtDinero(dinero));
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
        jugador.setJornadaFichaje(getJornadaActiva().getId());
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
        jugador.setJornadaFichaje(getJornadaActiva().getId());
        
        // BORRAR DE ALINEACIÓN DE VÍCTIMA
        Jornada jornadaActual = getJornadaActiva();
        Optional<Equipo> equipoVictima = equipoRepository.findByUsuario(victima).stream()
                .filter(e -> e.getJornada().getId().equals(jornadaActual.getId()))
                .findFirst();
        
        if (equipoVictima.isPresent()) {
            Equipo eq = equipoVictima.get();
            eq.getJugadoresAlineados().remove(jugador);
            equipoRepository.save(eq);
        }

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
        for (Jugador j : seleccionados) {
            if (j.getPropietario() == null || !j.getPropietario().getId().equals(usuarioId)) return "❌ " + j.getNombre() + " no es tuyo.";
        }
        Equipo equipo = equipoRepository.findByUsuario(usuario).stream().filter(e -> e.getJornada().getId().equals(jornada.getId())).findFirst().orElse(new Equipo(usuario, jornada));
        equipo.setJugadoresAlineados(seleccionados);
        equipoRepository.save(equipo);
        return "✅ Alineación guardada para Jornada " + getNumeroJornadaReal();
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
        int cambioValor = puntos * 100_000;
        int nuevoValor = jugador.getValor() + cambioValor;
        if (nuevoValor < 150_000) nuevoValor = 150_000;
        jugador.setValor(nuevoValor);

        jugadorRepository.save(jugador);
        return "✅ Puntos registrados: " + puntos;
    }

    @PostMapping("/admin/cerrar-jornada")
    public String cerrarJornada() {
        Jornada jornadaActual = getJornadaActiva();
        int numJornadaCerrada = jornadaActual.getNumero();
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
            int puntosTotales = 0;
            for(Jugador j : equipo.getJugadoresAlineados()) {
                Optional<Actuacion> act = actuacionRepository.findByJugadorAndJornada(j, jornadaActual);
                if(act.isPresent()) {
                    puntosTotales += act.get().getPuntosTotales();
                }
            }
            equipo.setPuntosTotalesJornada(puntosTotales);
            equipoRepository.save(equipo);
            resumenPremios.append("✅ ").append(manager.getNombre()).append(": ").append(puntosTotales).append("p\n");
        }
        
        Jornada nuevaJornada = new Jornada();
        nuevaJornada.setNumero(numJornadaCerrada + 1);
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
        for (Jugador j : jugadores) { 
            j.setPropietario(null); 
            j.setPuntosAcumulados(0); 
            j.setClausula(j.getValor());
            j.setJornadaFichaje(0L);
        }
        jugadorRepository.saveAll(jugadores);
        List<Usuario> usuarios = usuarioRepository.findAll();
        for (Usuario u : usuarios) { u.setPresupuesto(100_000_000); u.setActivo(true); }
        usuarioRepository.saveAll(usuarios);
        equipoRepository.deleteAll();
        actuacionRepository.deleteAll();
        noticiaRepository.deleteAll();
        jornadaRepository.deleteAll();
        
        Jornada j1 = new Jornada();
        j1.setNumero(1); 
        jornadaRepository.save(j1); 
        
        noticiaRepository.save(new Noticia("☢️ LIGA RESETEADA: ¡Todos empiezan de cero con 100M! ¡A fichar!"));
        return "✅ Liga reseteada. Recarga la página.";
    }

    public static class DatosPartido {
        public Long idJugador;
        public boolean jugado;
        public boolean victoria;
        public boolean derrota;
        public int goles;
        public int golesEncajados;
        public int autogoles;
        public String equipoColor; 
    }
}
