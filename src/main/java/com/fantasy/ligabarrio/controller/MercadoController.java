package com.fantasy.ligabarrio.controller;

import com.fantasy.ligabarrio.model.*;
import com.fantasy.ligabarrio.repository.*;
import com.fantasy.ligabarrio.service.FantasyService;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@RestController
@CrossOrigin(origins = "*")
public class MercadoController {

    private final JugadorRepository jugadorRepository;
    private final UsuarioRepository usuarioRepository;
    private final EquipoRepository equipoRepository;
    private final OfertaRepository ofertaRepository;
    private final NoticiaRepository noticiaRepository;
    private final FantasyService fantasyService;

    public MercadoController(JugadorRepository jugadorRepository, UsuarioRepository usuarioRepository,
                             EquipoRepository equipoRepository, OfertaRepository ofertaRepository,
                             NoticiaRepository noticiaRepository, FantasyService fantasyService) {
        this.jugadorRepository = jugadorRepository;
        this.usuarioRepository = usuarioRepository;
        this.equipoRepository = equipoRepository;
        this.ofertaRepository = ofertaRepository;
        this.noticiaRepository = noticiaRepository;
        this.fantasyService = fantasyService;
    }

    @GetMapping("/ofertas/mis-ofertas/{idUsuario}")
    public Map<String, List<Map<String, Object>>> verMisOfertas(@PathVariable Long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow();

        List<Map<String, Object>> recibidas = ofertaRepository.findByVendedorAndEstado(usuario, "PENDIENTE").stream()
                .map(o -> Map.<String, Object>of(
                        "id", o.getId(), "jugador", o.getJugador().getNombre(),
                        "comprador", o.getComprador().getNombre(), "cantidad", o.getCantidad(),
                        "cantidadFmt", fantasyService.fmtDinero(o.getCantidad())
                )).collect(Collectors.toList());

        List<Map<String, Object>> enviadas = ofertaRepository.findByCompradorAndEstado(usuario, "PENDIENTE").stream()
                .map(o -> Map.<String, Object>of(
                        "id", o.getId(), "jugador", o.getJugador().getNombre(),
                        "vendedor", o.getVendedor().getNombre(), "cantidad", o.getCantidad(),
                        "cantidadFmt", fantasyService.fmtDinero(o.getCantidad())
                )).collect(Collectors.toList());

        return Map.of("recibidas", recibidas, "enviadas", enviadas);
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
            if (fantasyService.getJornadaActiva().isBloqueada()) return "⛔ No se pueden aceptar ofertas durante el bloqueo.";

            Usuario comprador = oferta.getComprador();
            Usuario vendedor = oferta.getVendedor();
            Jugador jugador = oferta.getJugador();

            if (comprador.getPresupuesto() < oferta.getCantidad()) return "❌ El comprador ya no tiene saldo suficiente.";
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
            jugador.setJornadaFichaje(fantasyService.getJornadaActiva().getId());
            jugador.setFechaFichaje(LocalDate.now(ZoneId.of("Europe/Madrid")));

            Jornada jornadaActual = fantasyService.getJornadaActiva();
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
            fantasyService.cancelarOfertasPendientes(jugador);

            noticiaRepository.save(new Noticia("🤝 ACUERDO: " + comprador.getNombre() + " ha comprado a " + jugador.getNombre() + " al mánager " + vendedor.getNombre() + " por " + fantasyService.fmtDinero(oferta.getCantidad())));
            return "✅ Oferta aceptada. El jugador ha sido transferido.";
        }
        return "❌ Error.";
    }

    @PostMapping("/mercado/comprar/{idJugador}/{idUsuario}")
    public String comprarJugadorLibre(@PathVariable Long idJugador, @PathVariable Long idUsuario) {
        if (fantasyService.getJornadaActiva().isBloqueada()) return "⛔ No puedes hacer compras, ventas o clausulazos desde que se conoce la alineación hasta el día siguiente.";
        if (fantasyService.isMercadoCerrado()) return "⛔ MERCADO CERRADO EN ESTOS MOMENTOS ⛔";

        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario comprador = usuarioRepository.findById(idUsuario).orElseThrow();
        if (jugador.getPropietario() != null) return "❌ Error: Este jugador ya ha sido comprado.";

        LocalDate hoy = LocalDate.now(ZoneId.of("Europe/Madrid"));
        if (jugador.getFechaVenta() != null && jugador.getFechaVenta().isEqual(hoy)) {
            return "❌ Este jugador acaba de ser vendido. No saldrá al mercado hasta próximos días.";
        }

        comprador.setPresupuesto(comprador.getPresupuesto() - jugador.getValor());
        jugador.setPropietario(comprador);
        jugador.setClausula(jugador.getValor());
        jugador.setJornadaFichaje(fantasyService.getJornadaActiva().getId());
        jugador.setFechaFichaje(hoy);
        jugador.setFechaFinBlindaje(LocalDateTime.now(ZoneId.of("Europe/Madrid")).plusDays(7));
        fantasyService.cancelarOfertasPendientes(jugador);

        usuarioRepository.save(comprador);
        jugadorRepository.save(jugador);
        noticiaRepository.save(new Noticia("💰 MERCADO: " + comprador.getNombre() + " ha fichado a " + jugador.getNombre() + " (" + jugador.getPosicion() + ") por " + fantasyService.fmtDinero(jugador.getValor())));
        return "✅ Fichaje realizado.";
    }

    @PostMapping("/mercado/robar/{idJugador}/{idLadron}")
    public String robarJugador(@PathVariable Long idJugador, @PathVariable Long idLadron) {
        if (fantasyService.getJornadaActiva().isBloqueada()) return "⛔ Mercado bloqueado.";

        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario ladron = usuarioRepository.findById(idLadron).orElseThrow();
        Usuario victima = jugador.getPropietario();

        if (victima == null) return "❌ Es libre, fíchalo normal.";
        if (victima.getId().equals(ladron.getId())) return "❌ No te puedes robar a ti mismo.";
        if (jugador.getFechaFinBlindaje() != null && jugador.getFechaFinBlindaje().isAfter(LocalDateTime.now(ZoneId.of("Europe/Madrid")))) return "🛡️ JUGADOR BLINDADO. No se puede robar todavía.";

        int precioRobo = jugador.getClausula();
        if (ladron.getPresupuesto() < precioRobo) return "❌ No tienes suficiente dinero. No puedes quedarte en negativo robando jugadores.";

        ladron.setPresupuesto(ladron.getPresupuesto() - precioRobo);
        victima.setPresupuesto(victima.getPresupuesto() + precioRobo);
        jugador.setPropietario(ladron);
        jugador.setClausula(precioRobo);
        jugador.setJornadaFichaje(fantasyService.getJornadaActiva().getId());
        jugador.setFechaFichaje(LocalDate.now(ZoneId.of("Europe/Madrid")));
        jugador.setFechaFinBlindaje(LocalDateTime.now(ZoneId.of("Europe/Madrid")).plusDays(7));

        Jornada jornadaActual = fantasyService.getJornadaActiva();
        equipoRepository.findByUsuario(victima).stream().filter(e -> e.getJornada().getId().equals(jornadaActual.getId())).findFirst().ifPresent(eq -> {
            eq.getJugadoresAlineados().remove(jugador);
            equipoRepository.save(eq);
        });

        fantasyService.cancelarOfertasPendientes(jugador);
        usuarioRepository.save(ladron);
        usuarioRepository.save(victima);
        jugadorRepository.save(jugador);
        noticiaRepository.save(new Noticia("🔥 CLAUSULAZO: " + ladron.getNombre() + " ha robado a " + jugador.getNombre() + " (" + jugador.getPosicion() + ") al mánager " + victima.getNombre() + " por " + fantasyService.fmtDinero(precioRobo)));
        return "✅ ¡Has hecho un clausulazo!";
    }

    @PostMapping("/mercado/vender/{idJugador}/{idUsuario}")
    public String venderJugador(@PathVariable Long idJugador, @PathVariable Long idUsuario) {
        if (fantasyService.getJornadaActiva().isBloqueada()) return "⛔ Mercado bloqueado.";

        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario vendedor = usuarioRepository.findById(idUsuario).orElseThrow();
        if (jugador.getPropietario() == null || !jugador.getPropietario().getId().equals(idUsuario)) return "❌ No es tuyo.";

        int ingreso = jugador.getValor();
        vendedor.setPresupuesto(vendedor.getPresupuesto() + ingreso);
        jugador.setPropietario(null);
        jugador.setClausula(jugador.getValor());
        jugador.setFechaFinBlindaje(null);
        jugador.setFechaVenta(LocalDate.now(ZoneId.of("Europe/Madrid")));

        Jornada jornadaActual = fantasyService.getJornadaActiva();
        for(Equipo e : equipoRepository.findByUsuario(vendedor)) {
            if(e.getJornada().getId().equals(jornadaActual.getId())) {
                e.getJugadoresAlineados().remove(jugador);
                equipoRepository.save(e);
            }
        }

        fantasyService.cancelarOfertasPendientes(jugador);
        usuarioRepository.save(vendedor);
        jugadorRepository.save(jugador);

        noticiaRepository.save(new Noticia("👋 VENTA: " + vendedor.getNombre() + " ha vendido a " + jugador.getNombre() + " (" + jugador.getPosicion() + ") por " + fantasyService.fmtDinero(ingreso)));
        return "✅ Jugador vendido. Has recibido " + fantasyService.fmtDinero(ingreso);
    }

    @PostMapping("/jugador/subir-clausula/{idJugador}/{cantidad}")
    public String subirClausula(@PathVariable Long idJugador, @PathVariable int cantidad) {
        if (fantasyService.getJornadaActiva().isBloqueada()) return "⛔ El mercado está bloqueado.";
        Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
        Usuario propietario = jugador.getPropietario();

        if (cantidad <= 0) return "❌ Cantidad no válida.";
        if (propietario.getPresupuesto() - cantidad < 0) return "❌ No tienes saldo suficiente.";

        propietario.setPresupuesto(propietario.getPresupuesto() - cantidad);
        jugador.setClausula(jugador.getClausula() + (cantidad * 2));
        usuarioRepository.save(propietario);
        jugadorRepository.save(jugador);
        return "✅ Cláusula subida a " + fantasyService.fmtDinero(jugador.getClausula());
    }
}