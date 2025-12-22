package com.fantasy.ligabarrio;

import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class CalculadoraPuntosService {

    private final Random random = new Random();

    public int calcularPuntos(Actuacion a) {
        // 1. Si no juega, 0 puntos directos (según rúbrica común)
        if (!a.isJugado()) return 0;

        int puntos = 1; // +1 por jugar (Rúbrica común)
        String pos = a.getJugador().getPosicion().toUpperCase(); // PORTERO, DEFENSA, MEDIO, DELANTERO

        // 2. Resultado del Partido (Rúbrica común)
        if (a.isVictoria()) {
            puntos += 2;
            // Factor Suerte Extra por ganar (0 a 3)
            puntos += random.nextInt(4); // nextInt(4) da 0, 1, 2 o 3
        } else if (a.isDerrota()) {
            puntos -= 1;
            // Factor Suerte Extra por perder (0 a 1)
            puntos += random.nextInt(2); // nextInt(2) da 0 o 1
        }
        // Empate suma 0

        // 3. Reglas Específicas por Posición
        switch (pos) {
            case "PORTERO":
                puntos += calcularGolesEncajados(a.getGolesEncajados(), 6, 3, 0, -2);
                puntos += (a.getGolesMarcados() * 10); // Asumimos +10 por rareza
                puntos += (a.getAutogoles() * -2);
                break;

            case "DEFENSA":
                puntos += calcularGolesEncajados(a.getGolesEncajados(), 5, 3, -1, -3);
                puntos += (a.getGolesMarcados() * 6);
                puntos += (a.getAutogoles() * -4);
                break;

            case "MEDIO":
                puntos += calcularGolesEncajados(a.getGolesEncajados(), 2, 1, -2, -4);
                puntos += (a.getGolesMarcados() * 4);
                puntos += (a.getAutogoles() * -3);
                break;

            case "DELANTERO":
                puntos += calcularGolesEncajados(a.getGolesEncajados(), 1, 0, -3, -5);
                puntos += (a.getGolesMarcados() * 3);
                puntos += (a.getAutogoles() * -2);
                break;
        }

        return puntos;
    }

    // Método auxiliar para limpiar el switch
    private int calcularGolesEncajados(int goles, int pCero, int pMenos3, int p3a6, int pMas6) {
        if (goles == 0) return pCero;
        if (goles < 3) return pMenos3;
        if (goles <= 6) return p3a6;
        return pMas6;
    }

    public int calcularTotalEquipo(Equipo equipo) {
        // Suma simple de los puntos ya calculados en las actuaciones
        return equipo.getPuntosTotalesJornada(); 
    }
}
