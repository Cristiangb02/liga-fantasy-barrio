package com.fantasy.ligabarrio;

import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class CalculadoraPuntosService {

    private final ActuacionRepository actuacionRepository; // Necesitamos esto ahora
    private final Random random = new Random();

    // Constructor para inyectar el repositorio
    public CalculadoraPuntosService(ActuacionRepository actuacionRepository) {
        this.actuacionRepository = actuacionRepository;
    }

    // --- TU MÉTODO DE SIEMPRE (Calcula puntos de un jugador individual) ---
    public int calcularPuntos(Actuacion act) {
        int total = 0;
        Posicion pos = act.getJugador().getPosicion();

        // 1. Jugar
        total += 1;

        // 2. Resultado
        if (act.isVictoria()) {
            total += 2;
            total += random.nextInt(4); // Azar
        } else if (act.isDerrota()) {
            total -= 1;
            total += random.nextInt(2); // Azar
        }

        // 3. Goles Marcados
        int ptsGol = 0;
        switch (pos) {
            case PORTERO, DEFENSA -> ptsGol = 6;
            case MEDIO -> ptsGol = 4;
            case DELANTERO -> ptsGol = 3;
        }
        total += (act.getGolesMarcados() * ptsGol);

        // 4. Goles Encajados
        if (pos == Posicion.PORTERO) {
            if (act.getGolesEncajados() == 0) total += 6;
            else if (act.getGolesEncajados() < 3) total += 3;
            else if (act.getGolesEncajados() > 6) total -= 2;
        } else if (pos == Posicion.DEFENSA) {
            if (act.getGolesEncajados() == 0) total += 5;
            else if (act.getGolesEncajados() < 3) total += 3;
            else if (act.getGolesEncajados() >= 3 && act.getGolesEncajados() <= 6) total -= 1;
            else total -= 3;
        }

        // 5. Autogoles
        if (pos == Posicion.PORTERO) total -= (2 * act.getAutogoles());
        else total -= (4 * act.getAutogoles());

        return total;
    }

    // --- NUEVO MÉTODO: Calcula los puntos de un EQUIPO FANTASY entero ---
    public int calcularTotalEquipo(Equipo equipo) {
        int sumaTotal = 0;
        Jornada jornada = equipo.getJornada();

        // Recorremos los jugadores que Pepito ha alineado
        for (Jugador jugador : equipo.getJugadoresAlineados()) {

            // Buscamos qué hizo ese jugador esa jornada
            var actuacionOpt = actuacionRepository.findByJugadorAndJornada(jugador, jornada);

            if (actuacionOpt.isPresent()) {
                // Si jugó, sumamos sus puntos
                sumaTotal += actuacionOpt.get().getPuntosTotales();
            } else {
                // Si no jugó (o no hay datos), suma 0
                System.out.println("OJO: " + jugador.getNombre() + " no jugó en la jornada " + jornada.getNumero());
            }
        }
        return sumaTotal;
    }
}