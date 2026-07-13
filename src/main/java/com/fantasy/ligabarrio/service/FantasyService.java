package com.fantasy.ligabarrio.service;

import com.fantasy.ligabarrio.model.Jornada;
import com.fantasy.ligabarrio.model.Jugador;
import com.fantasy.ligabarrio.model.Oferta;
import com.fantasy.ligabarrio.repository.JornadaRepository;
import com.fantasy.ligabarrio.repository.OfertaRepository;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Comparator;

@Service
public class FantasyService {

    private final JornadaRepository jornadaRepository;
    private final OfertaRepository ofertaRepository;
    private long desplazamiento = 0; //Para la semilla de generación de mercado
    private boolean mantenimientoActivo = false;

    public boolean isMantenimientoActivo() { return mantenimientoActivo; }

    public void setMantenimientoActivo(boolean mantenimientoActivo) {
        this.mantenimientoActivo = mantenimientoActivo;
    }

    public FantasyService(JornadaRepository jornadaRepository, OfertaRepository ofertaRepository) {
        this.jornadaRepository = jornadaRepository;
        this.ofertaRepository = ofertaRepository;
    }

    public long getDesplazamiento() { return desplazamiento; }

    public void incrementarDesplazamiento() {
        this.desplazamiento++;
    }

    @Scheduled(cron = "0 0 10 * * *", zone = "Europe/Madrid")
    public void desbloqueoAutomatico() {
        Jornada actual = getJornadaActiva();
        if (actual.isBloqueada()) {
            actual.setBloqueada(false);
            jornadaRepository.save(actual);
        }
    }

    public Jornada getJornadaActiva() {
        List<Jornada> jornadas = jornadaRepository.findAll();
        Jornada jornadaResultado;

        if (jornadas.isEmpty()) {
            Jornada j1 = new Jornada();
            j1.setNumero(1);
            j1.setBloqueada(false);
            jornadaResultado = jornadaRepository.save(j1);
        } else {
            jornadas.sort(Comparator.comparing(Jornada::getNumero));
             Jornada activa = jornadas.get(jornadas.size() - 1);

            if (activa.getNumero() <= 0) {
                activa.setNumero(1);
                jornadaRepository.save(activa);
            }
            jornadaResultado = activa;
        }

        if (jornadaResultado.isBloqueada()) {
            LocalDateTime ahora = LocalDateTime.now(ZoneId.of("Europe/Madrid"));
            LocalDate hoy = ahora.toLocalDate();
            LocalTime horaActual = ahora.toLocalTime();
            LocalTime horaApertura = LocalTime.of(10, 0);

            if ((jornadaResultado.getDiaBloqueo() != null) && (hoy.isAfter(jornadaResultado.getDiaBloqueo())) && (horaActual.isAfter(horaApertura))) {
                jornadaResultado.setBloqueada(false);
                jornadaResultado.setDiaBloqueo(null);
                jornadaRepository.save(jornadaResultado);
            }
        }
        return jornadaResultado;
    }

    public long getNumeroJornadaReal() {
        return getJornadaActiva().getNumero();
    }

    public String formatearDinero(int cantidad) {
        return NumberFormat.getCurrencyInstance(Locale.of("es", "ES")).format(cantidad);
    }

    public int getPesoPosicion(String pos) {
        if (pos == null) {
            return 5; //Un peso mayor para después el orden
        }
        switch(pos.toUpperCase()) {
            case "PORTERO":
                return 1;
            case "DEFENSA":
                return 2;
            case "MEDIO":
                return 3;
            case "DELANTERO":
                return 4;
            default: return 5;
        }
    }

    public boolean isMercadoCerrado() {
        boolean estaCerrado = false;
        LocalTime ahora = LocalTime.now(ZoneId.of("Europe/Madrid"));
        boolean cerradoNoche = ahora.isAfter(LocalTime.of(21, 30)) || ahora.equals(LocalTime.of(21, 30));
        boolean cerradoManana = ahora.isBefore(LocalTime.of(10, 0));
        if (cerradoNoche || cerradoManana) {
            estaCerrado = true;
        }
        return estaCerrado;
    }

    public void cancelarOfertasPendientes(Jugador j) {
        List<Oferta> pendientes = ofertaRepository.findByJugador(j);
        for(Oferta o : pendientes) {
            if(o.getEstado().equals("PENDIENTE")) {
                o.setEstado("CANCELADA");
                ofertaRepository.save(o);
            }
        }
    }
}