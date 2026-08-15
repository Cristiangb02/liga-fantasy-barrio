    package com.fantasy.ligabarrio.controller;
    
    import com.fantasy.ligabarrio.model.*;
    import com.fantasy.ligabarrio.repository.*;
    import com.fantasy.ligabarrio.service.FantasyService;
    import org.springframework.web.bind.annotation.*;
    import java.util.*;
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
            List<Map<String, Object>> listaRecibidas = new ArrayList<>();
            List<Map<String, Object>> listaEnviadas = new ArrayList<>();

            List<Oferta> ofertasRecibidas = ofertaRepository.findByVendedorAndEstado(usuario, "PENDIENTE");
            for (Oferta o : ofertasRecibidas) {
                Map<String, Object> ofertaMapeada = Map.of(
                        "id", o.getId(),
                        "jugador", o.getJugador().getNombre(),
                        "comprador", o.getComprador().getNombre(),
                        "cantidad", o.getCantidad(),
                        "cantidadFmt", fantasyService.formatearDinero(o.getCantidad())
                );
                listaRecibidas.add(ofertaMapeada);
            }

            List<Oferta> ofertasEnviadas = ofertaRepository.findByCompradorAndEstado(usuario, "PENDIENTE");
            for (Oferta o : ofertasEnviadas) {
                Map<String, Object> ofertaMap = Map.of(
                        "id", o.getId(),
                        "jugador", o.getJugador().getNombre(),
                        "vendedor", o.getVendedor().getNombre(),
                        "cantidad", o.getCantidad(),
                        "cantidadFmt", fantasyService.formatearDinero(o.getCantidad())
                );
                listaEnviadas.add(ofertaMap);
            }

            Map<String, List<Map<String, Object>>> resultado = new HashMap<>();
            resultado.put("recibidas", listaRecibidas);
            resultado.put("enviadas", listaEnviadas);

            return resultado;
        }

        @PostMapping("/ofertas/crear")
        public String crearOferta(@RequestBody Map<String, Object> datos) {

            String resultado;
            Long idJugador = Long.valueOf(datos.get("idJugador").toString());
            Long idComprador = Long.valueOf(datos.get("idComprador").toString());
            int cantidad = Integer.parseInt(datos.get("cantidad").toString());

            Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
            Usuario comprador = usuarioRepository.findById(idComprador).orElseThrow();
            Usuario vendedor = jugador.getPropietario();

            if (vendedor == null) {
                resultado = "❌ El jugador está libre, fíchalo en el mercado.";
            } else if (vendedor.getId().equals(comprador.getId())) {
                resultado = "❌ No puedes hacerte una oferta a ti mismo.";
            } else if (comprador.getPresupuesto() < cantidad) {
                resultado = "❌ No tienes suficiente dinero para comprar a este jugador.";
            } else {
                Oferta oferta = new Oferta(jugador, comprador, vendedor, cantidad);
                ofertaRepository.save(oferta);
                resultado = "✅ Oferta enviada a " + vendedor.getNombre();
            }

            return resultado;
        }

        @PostMapping("/ofertas/responder/{idOferta}/{accion}")
        public String responderOferta(@PathVariable Long idOferta, @PathVariable String accion) {

            String resultado;
            Oferta oferta = ofertaRepository.findById(idOferta).orElseThrow();

            if (!oferta.getEstado().equals("PENDIENTE")) {
                resultado = "❌ Esta oferta ya no está activa.";

            } else if (accion.equals("rechazar")) {
                oferta.setEstado("RECHAZADA");
                ofertaRepository.save(oferta);
                resultado = "❌ Oferta rechazada.";

            } else if (accion.equals("aceptar")) {
                Usuario comprador = oferta.getComprador();
                Usuario vendedor = oferta.getVendedor();
                Jugador jugador = oferta.getJugador();

                if (fantasyService.getJornadaActiva().isBloqueada()) {
                    resultado = "⛔ No se pueden aceptar ofertas durante el bloqueo.";

                } else if (comprador.getPresupuesto() < oferta.getCantidad()) {
                    resultado = "❌ El comprador ya no tiene saldo suficiente.";

                } else if (jugador.getPropietario() == null || !jugador.getPropietario().getId().equals(vendedor.getId())) {
                    oferta.setEstado("CANCELADA");
                    ofertaRepository.save(oferta);
                    resultado = "❌ El jugador ya no pertenece a este usuario.";

                } else {
                    comprador.setPresupuesto(comprador.getPresupuesto() - oferta.getCantidad());
                    vendedor.setPresupuesto(vendedor.getPresupuesto() + oferta.getCantidad());
                    jugador.setPropietario(comprador);
                    jugador.setClausula(Math.max(oferta.getCantidad(), jugador.getValor()));
                    jugador.setFechaFinBlindaje(LocalDateTime.now(ZoneId.of("Europe/Madrid")).plusDays(7));
                    jugador.setJornadaFichaje(fantasyService.getJornadaActiva().getId());
                    jugador.setFechaFichaje(LocalDate.now(ZoneId.of("Europe/Madrid")));

                    //Se saca al jugador de la alineación del vendedor
                    Jornada jornadaActual = fantasyService.getJornadaActiva();
                    equipoRepository.findByUsuario(vendedor).stream()
                            .filter(e -> e.getJornada().getId().equals(jornadaActual.getId()))
                            .findFirst()
                            .ifPresent(e -> {
                                e.getJugadoresAlineados().remove(jugador);
                                equipoRepository.save(e);
                            });

                    oferta.setEstado("ACEPTADA");
                    usuarioRepository.save(comprador);
                    usuarioRepository.save(vendedor);
                    jugadorRepository.save(jugador);
                    ofertaRepository.save(oferta);
                    fantasyService.cancelarOfertasPendientes(jugador);

                    String mensajeNoticia = "ACUERDO: " + comprador.getNombre() + " ha comprado a " +
                            jugador.getNombre() + " al mánager " + vendedor.getNombre() +
                            " por " + fantasyService.formatearDinero(oferta.getCantidad());
                    noticiaRepository.save(new Noticia(mensajeNoticia));

                    resultado = "✅ Oferta aceptada. El jugador ha sido transferido.";
                }

            } else {
                resultado = "❌ Error. Algo ha salido mal.";
            }
            return resultado;
        }

        @PostMapping("/mercado/comprar/{idJugador}/{idUsuario}")
        public String comprarJugadorLibre(@PathVariable Long idJugador, @PathVariable Long idUsuario) {

            String resultado;

            if (fantasyService.getJornadaActiva().isBloqueada()) { //Si hay bloqueo no se puede comprar a nadi
                resultado = "⛔ No puedes hacer compras, ventas o clausulazos desde que se conoce la alineación hasta el día siguiente.";
            } else if (fantasyService.isMercadoCerrado()) { //Si está cerrado el mercado
                resultado = "⛔ MERCADO CERRADO EN ESTOS MOMENTOS ⛔";
            } else { //Si no hay bloqueos

                Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
                Usuario comprador = usuarioRepository.findById(idUsuario).orElseThrow();
                LocalDate hoy = LocalDate.now(ZoneId.of("Europe/Madrid"));

                if (jugador.getPropietario() != null) { //Si alguien ha comprado ya al jugador
                    resultado = "❌ Error: Este jugador ya ha sido comprado.";

                } else if (jugador.getFechaVenta() != null && jugador.getFechaVenta().isEqual(hoy)) {
                    resultado = "❌ Este jugador acaba de ser vendido.";

                } else { //Compra correcta
                    comprador.setPresupuesto(comprador.getPresupuesto() - jugador.getValor());
                    jugador.setPropietario(comprador);
                    jugador.setClausula(jugador.getValor());
                    jugador.setJornadaFichaje(fantasyService.getJornadaActiva().getId());
                    jugador.setFechaFichaje(hoy);
                    jugador.setFechaFinBlindaje(LocalDateTime.now(ZoneId.of("Europe/Madrid")).plusDays(7));

                    fantasyService.cancelarOfertasPendientes(jugador);
                    usuarioRepository.save(comprador);
                    jugadorRepository.save(jugador);

                    String msjNoticia = "💰 MERCADO: " + comprador.getNombre() + " ha fichado a " +
                            jugador.getNombre() + " (" + jugador.getPosicion() + ") por " +  fantasyService.formatearDinero(jugador.getValor());

                    noticiaRepository.save(new Noticia(msjNoticia));
                    resultado = "✅ Fichaje realizado con éxito.";
                }
            }
            return resultado;
        }

        @PostMapping("/mercado/robar/{idJugador}/{idLadron}")
        public String robarJugador(@PathVariable Long idJugador, @PathVariable Long idLadron) {

            String resultado;

            if (fantasyService.getJornadaActiva().isBloqueada()) {
                resultado = "⛔ El mercado está bloqueado ahora mismo.";
            } else {
                Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
                Usuario ladron = usuarioRepository.findById(idLadron).orElseThrow();
                Usuario victima = jugador.getPropietario();
                LocalDateTime ahora = LocalDateTime.now(ZoneId.of("Europe/Madrid"));

                if (victima == null) { //Si está justamente libre
                    resultado = "❌ El jugador ha sido vendido, no lo tiene ya el mánager al que le querías robar.";

                } else if (victima.getId().equals(ladron.getId())) {
                    resultado = "❌ No te puedes robar a ti mismo.";

                } else if (jugador.getFechaFinBlindaje() != null && jugador.getFechaFinBlindaje().isAfter(ahora)) {
                    resultado = "El jugador está blindado. No se puede robar todavía.";

                } else if (ladron.getPresupuesto() < jugador.getClausula()) {
                    resultado = "❌ No tienes suficiente dinero. No puedes quedarte en negativo robando jugadores.";

                } else { //Se dan las condiciones: hay clausulazo
                    int precioRobo = jugador.getClausula();

                    ladron.setPresupuesto(ladron.getPresupuesto() - precioRobo);
                    victima.setPresupuesto(victima.getPresupuesto() + precioRobo);
                    jugador.setPropietario(ladron);
                    jugador.setClausula(precioRobo);
                    jugador.setJornadaFichaje(fantasyService.getJornadaActiva().getId());
                    jugador.setFechaFichaje(ahora.toLocalDate());
                    jugador.setFechaFinBlindaje(ahora.plusDays(7));

                    //Quitamos el jugador de la alineación de la víctima
                    Jornada jornadaActual = fantasyService.getJornadaActiva();
                    List<Equipo> equiposDeLaVictima = equipoRepository.findByUsuario(victima);

                    for (Equipo equipo : equiposDeLaVictima) {
                        if (equipo.getJornada().getId().equals(jornadaActual.getId())) { //El equipo de la jornada actual
                            equipo.getJugadoresAlineados().remove(jugador);
                            equipoRepository.save(equipo);
                        }
                    }

                    //Una vez quitado, se guardan los datos
                    fantasyService.cancelarOfertasPendientes(jugador);
                    usuarioRepository.save(ladron);
                    usuarioRepository.save(victima);
                    jugadorRepository.save(jugador);

                    String mensajeRobo = "🔥 CLAUSULAZO: " + ladron.getNombre() + " ha robado a " +
                            jugador.getNombre() + " (" + jugador.getPosicion() + ") al mánager " +
                            victima.getNombre() + " por " + fantasyService.formatearDinero(precioRobo);
                    noticiaRepository.save(new Noticia(mensajeRobo));
                    resultado = "✅ ¡Has hecho un clausulazo!";
                }
            }

            return resultado;
        }

        @PostMapping("/mercado/vender/{idJugador}/{idUsuario}")
        public String venderJugador(@PathVariable Long idJugador, @PathVariable Long idUsuario) {

            String resultado;

            if (fantasyService.getJornadaActiva().isBloqueada()) { //No se puede vender si hay bloqueo
                resultado = "⛔ Mercado bloqueado.";
            } else {
                Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
                Usuario vendedor = usuarioRepository.findById(idUsuario).orElseThrow();

                if (jugador.getPropietario() == null || !jugador.getPropietario().getId().equals(idUsuario)) {
                    resultado = "❌ Error. Este jugador no es tuyo."; //Por seguridad por si se manipulan las URLs

                } else { //Se produce la venta
                    int ingreso = jugador.getValor();

                    vendedor.setPresupuesto(vendedor.getPresupuesto() + ingreso);
                    jugador.setPropietario(null);
                    jugador.setClausula(jugador.getValor());
                    jugador.setFechaFinBlindaje(null);
                    jugador.setFechaVenta(LocalDate.now(ZoneId.of("Europe/Madrid")));

                    //Actualizamos la alineación actual
                    Jornada jornadaActual = fantasyService.getJornadaActiva();
                    for (Equipo e : equipoRepository.findByUsuario(vendedor)) {
                        if (e.getJornada().getId().equals(jornadaActual.getId())) {
                            e.getJugadoresAlineados().remove(jugador);
                            equipoRepository.save(e);
                        }
                    }

                    fantasyService.cancelarOfertasPendientes(jugador);
                    usuarioRepository.save(vendedor);
                    jugadorRepository.save(jugador);

                    String mensajeVenta = "👋 VENTA: " + vendedor.getNombre() + " ha vendido a " +
                            jugador.getNombre() + " (" + jugador.getPosicion() + ") por " + fantasyService.formatearDinero(ingreso);
                    noticiaRepository.save(new Noticia(mensajeVenta));
                    resultado = "✅ Jugador vendido. Has recibido " + fantasyService.formatearDinero(ingreso);
                }
            }
            return resultado;
        }

        @PostMapping("/jugador/subir-clausula/{idJugador}/{cantidad}")
        public String subirClausula(@PathVariable Long idJugador, @PathVariable int cantidad) {
            String resultado;

            Jugador jugador = jugadorRepository.findById(idJugador).orElseThrow();
            Usuario propietario = jugador.getPropietario();

            if (propietario == null) {
                resultado = "❌ El jugador no tiene propietario.";

            } else if (cantidad <= 0) {
                resultado = "❌ Cantidad no válida.";

            } else if (propietario.getPresupuesto() < cantidad) {
                resultado = "❌ No tienes saldo suficiente.";

            } else {
                propietario.setPresupuesto(propietario.getPresupuesto() - cantidad);
                jugador.setClausula(jugador.getClausula() + (cantidad * 2));
                usuarioRepository.save(propietario);
                jugadorRepository.save(jugador);
                resultado = "✅ Cláusula subida a " + fantasyService.formatearDinero(jugador.getClausula());
            }
            return resultado;
        }
    }