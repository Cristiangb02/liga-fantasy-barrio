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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@RestController
@CrossOrigin(origins = "*")
public class FantasyController {

    private final EquipoRepository equipoRepository;
    private final JugadorRepository jugadorRepository;
    private final UsuarioRepository usuarioRepository;
    private final JornadaRepository jornadaRepository;
    private final ActuacionRepository actuacionRepository;
    private final NoticiaRepository noticiaRepository;
    private final OfertaRepository ofertaRepository; // 🆕 Nuevo repo
    private final CalculadoraPuntosService calculadora;

    public FantasyController(EquipoRepository equipoRepository, JugadorRepository jugadorRepository, UsuarioRepository usuarioRepository, JornadaRepository jornadaRepository, ActuacionRepository actuacionRepository, NoticiaRepository noticiaRepository, OfertaRepository ofertaRepository, CalculadoraPuntosService calculadora) {
        this.equipoRepository = equipoRepository;
        this.jugadorRepository = jugadorRepository;
        this.usuarioRepository = usuarioRepository;
        this.jornadaRepository = jornadaRepository;
        this.actuacionRepository = actuacionRepository;
        this.noticiaRepository = noticiaRepository;
        this.ofertaRepository = ofertaRepository;
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

    private long getNumeroJornadaReal() { return getJornadaActiva().getNumero(); }

    // --- HORARIO MERCADO (21:30 - 01:30 CERRADO) ---
    private boolean isMercadoCerrado() {
        LocalTime ahora = LocalTime.now(ZoneId.of("Europe/Madrid"));
        LocalTime inicioCierre = LocalTime.of(21, 30); // 21:30
        LocalTime finCierre = LocalTime.of(1, 30);     // 01:30 (madrugada siguiente)

        //Está cerrado si es DESPUÉS de las 21:30 o ANTES de la 01:30
        return ahora.isAfter(inicioCierre) || ahora.isBefore(finCierre);
    }

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
            return "✅ Solicitud enviada. Contacta con el creador de la app por Whatsapp para que te acepte y luego pulsa el botón 'Entrar'.";
        }
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Usuario datos) {
        Usuario user = usuarioRepository.findByNombre(datos.getNombre());
        if (user == null || !user.getPassword().equals(datos.getPassword())) return Map.of("error", "Credenciales incorrectas.");
        if (!user.isActivo()) return Map.of("error", "⛔ Tu cuenta aún no ha sido aprobada por el Admin.");
        return Map.of("id", user.getId(), "nombre", user.getNombre(), "esAdmin", user.isEsAdmin(), "presupuesto", user.getPresupuesto());
    }

    // --- ADMIN USUARIOS (CON CONTRASEÑAS VISIBLES 🕵️) ---
    @GetMapping("/admin/usuarios-gestion")
    public List<Usuario> getUsuariosGestion() {
        return usuarioRepository.findAll();
    }

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

    // --- DATOS GLOBALES Y JUGADORES ---
    @GetMapping("/jornada/actual")
    public long getNumeroJornadaActualEndpoint() { return getNumeroJornadaReal(); }

    @GetMapping("/usuarios")
    public List<Usuario> verRivales() { return usuarioRepository.findAll().stream().filter(Usuario::isActivo).collect(Collectors.toList()); }

    @GetMapping("/jugadores")
    public List<Map<String, Object>> verTodosLosJugadores() {
        LocalDateTime ahora = LocalDateTime.now(ZoneId.of("Europe/Madrid"));
        return jugadorRepository.findAll().stream().map(j -> {
            boolean blindado = j.getFechaFinBlindaje() != null && j.getFechaFinBlindaje().isAfter(ahora);
            long segundosRestantes = blindado ? ChronoUnit.SECONDS.between(ahora, j.getFechaFinBlindaje()) : 0;

            return Map.<String, Object>of(
                    "id", j.getId(),
                    "nombre", j.getNombre(),
                    "posicion", j.getPosicion(),
                    "valor", j.getValor(),
                    "clausula", j.getClausula(),
                    "puntosAcumulados", j.getPuntosAcumulados(),
                    "urlImagen", (j.getUrlImagen() != null ? j.getUrlImagen() : ""),
                    "propietario", (j.getPropietario() != null ? j.getPropietario() : Map.of()), // Para evitar nulls
                    "blindado", blindado,
                    "segundosBlindaje", segundosRestantes
            );
        }).collect(Collectors.toList());
    }

    @GetMapping("/mercado-diario")
    public List<Jugador> getMercadoDiario() {
        List<Jugador> todos = jugadorRepository.findAll();
        List<Jugador> candidatosHoy = new ArrayList<>();
        LocalDate hoy = LocalDate.now(ZoneId.of("Europe/Madrid"));

        for (Jugador j : todos) {
            boolean esLibre = (j.getPropietario() == null);
            boolean fichadoHoy = (j.getPropietario() != null && j.getFechaFichaje() != null && j.getFechaFichaje().equals(hoy));
            if (esLibre || fichadoHoy) candidatosHoy.add(j);
        }
        candidatosHoy.sort((j1, j2) -> Long.compare(j1.getId(), j2.getId()));
        long seed = hoy.toEpochDay();
        Collections.shuffle(candidatosHoy, new Random(seed));
        int limite = Math.min(candidatosHoy.size(), 14);
        List<Jugador> escaparateFijo = candidatosHoy.subList(0, limite);
        return escaparateFijo.stream().filter(j -> j.getPropietario() == null).collect(Collectors.toList());
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

        // Actualizamos Cláusula (solo si tiene dueño, si es libre la cláusula suele ser el valor)
        if (jugador.getPropietario() != null) {
            // La cláusula sube en la misma cantidad exacta que el valor
            jugador.setClausula(jugador.getClausula() + cambioValor);
        } else {
            // Si es libre, cláusula = valor
            jugador.setClausula(nuevoValor);
        }

        jugadorRepository.save(jugador);
        return "✅ Puntos registrados de " + jugador.getNombre() + ": "  + puntos;
    }

    // --- OPERACIONES DE MERCADO ---

    @PostMapping("/mercado/comprar/{idJugador}/{idUsuario}")
    public String comprarJugadorLibre(@PathVariable Long idJugador, @PathVariable Long idUsuario) {
        //Cierre
        if (isMercadoCerrado()) return "⛔ EL MERCADO ESTÁ CERRADO EN ESTOS MOMENTOS (21:00 - 09:00)";

        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario comprador = usuarioRepository.findById(idUsuario).orElseThrow();
        if (jugador.getPropietario() != null) return "❌ Error: Este jugador ya ha sido comprado.";

        comprador.setPresupuesto(comprador.getPresupuesto() - jugador.getValor());
        jugador.setPropietario(comprador);
        jugador.setClausula(jugador.getValor());
        jugador.setJornadaFichaje(getJornadaActiva().getId());
        jugador.setFechaFichaje(LocalDate.now(ZoneId.of("Europe/Madrid")));

        //Blindaje de 7 días
        jugador.setFechaFinBlindaje(LocalDateTime.now(ZoneId.of("Europe/Madrid")).plusDays(7));
        //Cancelar ofertas pendientes de este jugador al ser comprado
        cancelarOfertasPendientes(jugador);

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

        if (jugador.getFechaFinBlindaje() != null && jugador.getFechaFinBlindaje().isAfter(LocalDateTime.now(ZoneId.of("Europe/Madrid")))) {
            return "🛡️ JUGADOR BLINDADO. No se puede robar todavía.";
        }

        int precioRobo = jugador.getClausula();
        if (ladron.getPresupuesto() < precioRobo) return "❌ No tienes suficiente dinero para el clausulazo.";

        ladron.setPresupuesto(ladron.getPresupuesto() - precioRobo);
        victima.setPresupuesto(victima.getPresupuesto() + precioRobo);
        jugador.setPropietario(ladron);
        jugador.setClausula(precioRobo);
        jugador.setJornadaFichaje(getJornadaActiva().getId());
        jugador.setFechaFichaje(LocalDate.now(ZoneId.of("Europe/Madrid")));

        //BLINDAJE 7 DÍAS
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
        noticiaRepository.save(new Noticia("🔥 CLAUSULAZO: " + ladron.getNombre() + " robó a " + jugador.getNombre() + " por " + fmtDinero(precioRobo)));
        return "✅ ¡Has hecho un clausulazo!";
    }

    @PostMapping("/mercado/vender/{idJugador}/{idUsuario}")
    public String venderJugador(@PathVariable Long idJugador, @PathVariable Long idUsuario) {
        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario vendedor = usuarioRepository.findById(idUsuario).orElseThrow();
        if (jugador.getPropietario() == null || !jugador.getPropietario().getId().equals(idUsuario)) return "❌ No es tuyo.";

        int ingreso = jugador.getValor() + (jugador.getClausula() - jugador.getValor()) / 2; // Recuperas mitad de inversión extra
        vendedor.setPresupuesto(vendedor.getPresupuesto() + ingreso);
        jugador.setPropietario(null);
        jugador.setClausula(jugador.getValor());
        jugador.setFechaFinBlindaje(null);

        Jornada jornadaActual = getJornadaActiva();
        List<Equipo> equipos = equipoRepository.findByUsuario(vendedor);
        for(Equipo e : equipos) {
            if(e.getJornada().getId().equals(jornadaActual.getId())) { e.getJugadoresAlineados().remove(jugador); equipoRepository.save(e); }
        }

        cancelarOfertasPendientes(jugador);

        usuarioRepository.save(vendedor);
        jugadorRepository.save(jugador);
        noticiaRepository.save(new Noticia("👋 VENTA: " + vendedor.getNombre() + " vende a " + jugador.getNombre() + " por " + fmtDinero(ingreso)));
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
        return "✅ Cláusula subida a " + fmtDinero(jugador.getClausula());
    }

    // --- SISTEMA DE OFERTAS ---

    @GetMapping("/ofertas/recibidas/{idUsuario}")
    public List<Map<String, Object>> verOfertasRecibidas(@PathVariable Long idUsuario) {
        Usuario vendedor = usuarioRepository.findById(idUsuario).orElseThrow();
        return ofertaRepository.findByVendedorAndEstado(vendedor, "PENDIENTE").stream()
                .map(o -> Map.<String, Object>of(
                        "id", o.getId(),
                        "jugador", o.getJugador().getNombre(),
                        "comprador", o.getComprador().getNombre(),
                        "cantidad", o.getCantidad(),
                        "cantidadFmt", fmtDinero(o.getCantidad())
                )).collect(Collectors.toList());
    }

    @PostMapping("/ofertas/crear")
    public String crearOferta(@RequestBody Map<String, Object> datos) {
        Long idJugador = Long.valueOf(datos.get("idJugador").toString());
        Long idComprador = Long.valueOf(datos.get("idComprador").toString());
        int cantidad = Integer.parseInt(datos.get("cantidad").toString());

        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario comprador = usuarioRepository.findById(idComprador).orElseThrow();
        Usuario vendedor = jugador.getPropietario();

        if (vendedor == null) return "❌ El jugador es libre, fíchalo en el mercado.";
        if (vendedor.getId().equals(comprador.getId())) return "❌ No puedes ofertarte a ti mismo.";
        if (comprador.getPresupuesto() < cantidad) return "❌ No tienes saldo suficiente.";

        Oferta oferta = new Oferta(jugador, comprador, vendedor, cantidad);
        ofertaRepository.save(oferta);
        return "✅ Oferta enviada a " + vendedor.getNombre();
    }

    @PostMapping("/ofertas/responder/{idOferta}/{accion}")
    public String responderOferta(@PathVariable Long idOferta, @PathVariable String accion) {
        Oferta oferta = ofertaRepository.findById(idOferta).orElseThrow();
        if (!oferta.getEstado().equals("PENDIENTE")) return "❌ Esta oferta ya no está activa.";

        if (accion.equals("rechazar")) {
            oferta.setEstado("RECHAZADA");
            ofertaRepository.save(oferta);
            return "❌ Oferta rechazada.";
        } else if (accion.equals("aceptar")) {
            Usuario comprador = oferta.getComprador();
            Usuario vendedor = oferta.getVendedor();
            Jugador jugador = oferta.getJugador();

            // Comprobación final de saldo
            if (comprador.getPresupuesto() < oferta.getCantidad()) {
                return "❌ El comprador ya no tiene saldo suficiente.";
            }
            // Comprobación de propiedad (por si lo vendió mientras)
            if (jugador.getPropietario() == null || !jugador.getPropietario().getId().equals(vendedor.getId())) {
                oferta.setEstado("CANCELADA");
                ofertaRepository.save(oferta);
                return "❌ El jugador ya no pertenece a este usuario.";
            }

            // Transacción
            comprador.setPresupuesto(comprador.getPresupuesto() - oferta.getCantidad());
            vendedor.setPresupuesto(vendedor.getPresupuesto() + oferta.getCantidad());
            jugador.setPropietario(comprador);
            jugador.setClausula(oferta.getCantidad());
            jugador.setFechaFinBlindaje(LocalDateTime.now(ZoneId.of("Europe/Madrid")).plusDays(7));
            jugador.setJornadaFichaje(getJornadaActiva().getId());
            jugador.setFechaFichaje(LocalDate.now(ZoneId.of("Europe/Madrid")));

            // Actualizar equipos alineados
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

            // Cancelar otras ofertas por el mismo jugador
            cancelarOfertasPendientes(jugador);

            noticiaRepository.save(new Noticia("🤝 ACUERDO: " + comprador.getNombre() + " compra a " + jugador.getNombre() + " de " + vendedor.getNombre() + " por " + fmtDinero(oferta.getCantidad())));
            return "✅ Oferta aceptada. El jugador ha sido transferido.";
        }
        return "❌ Acción desconocida.";
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

    // --- RESTO DE ENDPOINTS ESTÁNDAR ---

    @PostMapping("/alinear/{usuarioId}")
    public String guardarAlineacion(@RequestBody List<Long> idsJugadores, @PathVariable Long usuarioId) {
        if (idsJugadores == null) return "❌ Error: Lista vacía.";
        if (idsJugadores.size() > 7) return "❌ Máximo 7 jugadores.";
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        Jornada jornada = getJornadaActiva();
        List<Jugador> seleccionados = jugadorRepository.findAllById(idsJugadores);
        for (Jugador j : seleccionados) {
            if (j.getPropietario() == null || !j.getPropietario().getId().equals(usuarioId)) return "❌ " + j.getNombre() + " no te pertenece.";
        }
        Equipo equipo = equipoRepository.findByUsuario(usuario).stream()
                .filter(e -> e.getJornada().getId().equals(jornada.getId()))
                .findFirst().orElse(new Equipo(usuario, jornada));
        equipo.setJugadoresAlineados(seleccionados);
        equipoRepository.save(equipo);
        return "✅ Alineación guardada J-" + getNumeroJornadaReal();
    }

    @GetMapping("/alineacion/{usuarioId}")
    public List<Jugador> getAlineacion(@PathVariable Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        Jornada jornadaActual = getJornadaActiva();
        Optional<Equipo> equipo = equipoRepository.findByUsuario(usuario).stream().filter(e -> e.getJornada().getId().equals(jornadaActual.getId())).findFirst();
        return equipo.map(Equipo::getJugadoresAlineados).orElse(List.of());
    }

    @GetMapping("/noticias")
    public List<Noticia> verNoticias() { return noticiaRepository.findAllByOrderByFechaDesc(); }

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
        Jornada jornadaActual = getJornadaActiva();
        return equipoRepository.findByUsuario(usuario).stream()
                .filter(e -> !e.getJornada().getId().equals(jornadaActual.getId()) && !e.isReclamado())
                .map(e -> {
                    int p = e.getPuntosTotalesJornada();
                    int dinero = (p > 0) ? p * 100_000 : 0;

                    // Lógica MVP
                    int maxPuntos = actuacionRepository.findAll().stream()
                            .filter(a -> a.getJornada().getId().equals(e.getJornada().getId()))
                            .mapToInt(Actuacion::getPuntosTotales).max().orElse(0);
                    boolean mvp = false;
                    String nombreMvp = "";
                    if (maxPuntos > 0) {
                        for (Jugador j : e.getJugadoresAlineados()) {
                            Optional<Actuacion> act = actuacionRepository.findByJugadorAndJornada(j, e.getJornada());
                            if (act.isPresent() && act.get().getPuntosTotales() == maxPuntos) { mvp = true; nombreMvp = j.getNombre(); break; }
                        }
                    }
                    int bonus = mvp ? 1_000_000 : 0;
                    return Map.<String, Object>of("idEquipo", e.getId(), "jornada", e.getJornada().getNumero(), "puntos", p, "dineroFmt", fmtDinero(dinero + bonus), "tieneMvp", mvp, "nombreMvp", nombreMvp, "bonusFmt", fmtDinero(bonus));
                }).collect(Collectors.toList());
    }

    @PostMapping("/reclamar-premio/{idEquipo}")
    public String reclamarPremio(@PathVariable Long idEquipo) {
        Equipo equipo = equipoRepository.findById(idEquipo).orElseThrow();
        if (equipo.isReclamado()) return "❌ Ya cobrado.";
        int p = equipo.getPuntosTotalesJornada();
        int base = (p > 0) ? p * 100_000 : 0;
        int max = actuacionRepository.findAll().stream().filter(a -> a.getJornada().getId().equals(equipo.getJornada().getId())).mapToInt(Actuacion::getPuntosTotales).max().orElse(0);
        boolean mvp = false;
        if(max > 0) {
            for(Jugador j : equipo.getJugadoresAlineados()){
                Optional<Actuacion> act = actuacionRepository.findByJugadorAndJornada(j, equipo.getJornada());
                if(act.isPresent() && act.get().getPuntosTotales() == max) { mvp = true; break; }
            }
        }
        int bonus = mvp ? 1_000_000 : 0;

        Usuario u = equipo.getUsuario();
        u.setPresupuesto(u.getPresupuesto() + base + bonus);
        equipo.setReclamado(true);
        usuarioRepository.save(u);
        equipoRepository.save(equipo);
        return "💰 Reclamado: " + fmtDinero(base) + (mvp ? " + 🏆 " + fmtDinero(bonus) : "");
    }

    @PostMapping("/admin/cerrar-jornada")
    public String cerrarJornada() {
        Jornada actual = getJornadaActiva();
        List<Equipo> equipos = equipoRepository.findByJornada(actual);
        StringBuilder res = new StringBuilder();
        for (Equipo e : equipos) {
            Usuario m = e.getUsuario();
            if (m.getPresupuesto() < 0) { e.setPuntosTotalesJornada(0); equipoRepository.save(e); res.append("🚫 ").append(m.getNombre()).append(" (Saldo Negativo)\n"); continue; }
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
        return "✅ Jornada Cerrada.";
    }

    @GetMapping("/admin/jugadores-pendientes")
    public List<Jugador> getJugadoresPendientes() {
        Jornada actual = getJornadaActiva();
        return jugadorRepository.findAll().stream()
                .filter(j -> actuacionRepository.findByJugadorAndJornada(j, actual).isEmpty())
                .sorted((j1, j2) -> j1.getNombre().compareToIgnoreCase(j2.getNombre()))
                .collect(Collectors.toList());
    }

    @PostMapping("/admin/reset-liga")
    public String resetearLiga() {
        List<Jugador> jugadores = jugadorRepository.findAll();
        for (Jugador j : jugadores) {
            j.setPropietario(null); j.setPuntosAcumulados(0); j.setClausula(j.getValor());
            j.setJornadaFichaje(0L); j.setFechaFichaje(null); j.setFechaFinBlindaje(null);
        }
        jugadorRepository.saveAll(jugadores);
        List<Usuario> usuarios = usuarioRepository.findAll();
        for (Usuario u : usuarios) { u.setPresupuesto(100_000_000); u.setActivo(true); }
        usuarioRepository.saveAll(usuarios);
        equipoRepository.deleteAll();
        actuacionRepository.deleteAll();
        noticiaRepository.deleteAll();
        ofertaRepository.deleteAll(); // Borrar ofertas
        jornadaRepository.deleteAll();
        Jornada j1 = new Jornada(); j1.setNumero(1); jornadaRepository.save(j1);
        noticiaRepository.save(new Noticia("☢️ LIGA RESETEADA."));
        return "✅ Liga reseteada.";
    }

    @DeleteMapping("/admin/eliminar-usuario/{idUsuario}")
    public String eliminarUsuario(@PathVariable Long idUsuario) {
        Usuario u = usuarioRepository.findById(idUsuario).orElseThrow();
        if(u.isEsAdmin()) return "❌ No se puede borrar al admin.";
        jugadorRepository.findAll().stream().filter(j -> j.getPropietario() != null && j.getPropietario().getId().equals(idUsuario)).forEach(j -> {
            j.setPropietario(null); j.setClausula(j.getValor()); jugadorRepository.save(j);
        });
        equipoRepository.deleteAll(equipoRepository.findByUsuario(u));
        ofertaRepository.deleteAll(ofertaRepository.findByVendedorAndEstado(u, "PENDIENTE"));
        ofertaRepository.deleteAll(ofertaRepository.findByCompradorAndEstado(u, "PENDIENTE"));
        usuarioRepository.delete(u);
        return "✅ Usuario eliminado.";
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
            return cmp != 0 ? cmp : Integer.compare((int)m2.get("valorPlantilla"), (int)m1.get("valorPlantilla"));
        }).collect(Collectors.toList());
    }

    // Auxiliar DTO
    public static class DatosPartido {
        public Long idJugador; public boolean jugado; public boolean victoria; public boolean derrota;
        public int goles; public int golesEncajados; public int autogoles; public String equipoColor;
    }
}