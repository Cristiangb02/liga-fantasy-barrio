package com.fantasy.ligabarrio;

import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class CalculadoraPuntosService {

    private final Random random = new Random();

    public int calcularPuntos(Actuacion a) {
        //Si no ha jugado, no suma nada
        if (!a.isJugado()) return 0;

        int puntos = 1; //Por jugar el partido, +1
        String pos = a.getJugador().getPosicion().toUpperCase();

        // --- RESULTADO DEL PARTIDO ---
        if (a.isVictoria()) {
            puntos += 2;
            puntos += random.nextInt(4);
        } else if (a.isDerrota()) {
            puntos -= 2;
            puntos += random.nextInt(2);
        } else {
            puntos += random.nextInt(2); //+0 o +1 como mucho
        }

        switch (pos) {
            case "PORTERO":
                //1. Goles Encajados:
                //    - 0 goles: +7 pts (Premio portería a cero muy alto)
                //    - <3 goles: +4 pts
                //    - 3-6 goles: 0 pts
                //    - >6 goles: -3 pts
                puntos += calcularGolesEncajados(a.getGolesEncajados(), 7, 4, 0, -3);

                //2. Goles Marcados: ¡Premio enorme (x10) porque es muy difícil!
                puntos += (a.getGolesMarcados() * 10);

                //3. Autogoles: (-3)
                puntos += (a.getAutogoles() * -3);
                break;

            case "DEFENSA":
                //1. Goles Encajados:
                //    - 0 goles: +5 pts
                //    - <3 goles: +3 pts
                //    - 3-6 goles: -1 pt
                //    - >6 goles: -3 pts
                puntos += calcularGolesEncajados(a.getGolesEncajados(), 5, 3, -1, -3);

                //2. Goles Marcados: Buen premio (x6)
                puntos += (a.getGolesMarcados() * 6);

                //3. Autogoles: (-4)
                puntos += (a.getAutogoles() * -4);
                break;

            case "MEDIO":
                //1. Goles Encajados:
                //    - 0 goles: +4 pts
                //    - <3 goles: +3 pts
                //    - 3-6 goles: 0 pts
                //    - >6 goles: -4 pts
                puntos += calcularGolesEncajados(a.getGolesEncajados(), 4, 3, 0, -4);

                //2. Goles Marcados: Valor normal (x5)
                puntos += (a.getGolesMarcados() * 5);

                //3. Autogoles: Castigo medio (-3)
                puntos += (a.getAutogoles() * -3);
                break;

            case "DELANTERO":
                //1. Goles Encajados:
                //    - 0 goles: +3 pts
                //    - <3 goles: +2 pts
                //    - 3-6 goles: 0 pts
                //    - >6 goles: -1 pts
                puntos += calcularGolesEncajados(a.getGolesEncajados(), 3, 2, 0, -1);

                //2. Goles Marcados:
                puntos += (a.getGolesMarcados() * 4);

                //3. Autogoles:
                puntos += (a.getAutogoles() * -3);
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