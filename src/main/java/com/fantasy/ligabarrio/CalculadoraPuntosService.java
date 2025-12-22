package com.fantasy.ligabarrio;

import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class CalculadoraPuntosService {

    private final Random random = new Random();

    public int calcularPuntos(Actuacion a) {
        // 1. Si no juega, 0 puntos directos
        if (!a.isJugado()) return 0;

        int puntos = 1; // +1 por jugar
        
        // 🔴 CORRECCIÓN: Usamos .name() porque Posicion es un Enum
        String pos = a.getJugador().getPosicion().name(); // Devuelve "PORTERO", "DEFENSA"...

        // 2. Resultado del Partido
        if (a.isVictoria()) {
            puntos += 2;
            puntos += random.nextInt(4); // 0 a 3 extra
        } else if (a.isDerrota()) {
            puntos -= 1;
            puntos += random.nextInt(2); // 0 a 1 extra
        }
        
        // 3. Reglas Específicas por Posición
        switch (pos) {
            case "PORTERO":
                puntos += calcularGolesEncajados(a.getGolesEncajados(), 6, 3, 0, -2);
                puntos += (a.getGolesMarcados() * 10);
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

    private int calcularGolesEncajados(int goles, int pCero, int pMenos3, int p3a6, int pMas6) {
        if (goles == 0) return pCero;
        if (goles < 3) return pMenos3;
        if (goles <= 6) return p3a6;
        return pMas6;
    }

    public int calcularTotalEquipo(Equipo equipo) {
        return equipo.getPuntosTotalesJornada(); 
    }
}
