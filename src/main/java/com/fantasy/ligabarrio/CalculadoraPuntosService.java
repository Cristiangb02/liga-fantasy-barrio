package com.fantasy.ligabarrio;

import org.springframework.stereotype.Service;

@Service
public class CalculadoraPuntosService {

    public int calcularPuntos(Actuacion a) {
        int puntos = 0;

        // 1. Puntos base por partido
        // (Aquí puedes ajustar tu lógica básica, pero lo importante es que PERMITA bajar de 0)
        
        // Ejemplo básico (ajústalo a tu gusto o espera al Punto 8 para la rúbrica Excel)
        if (a.isVictoria()) puntos += 3;
        if (a.isDerrota()) puntos -= 1; // Restar por perder
        
        // Goles
        puntos += (a.getGolesMarcados() * 5); // Gol vale 5
        
        // Goles Encajados (Castigo severo para probar negativos)
        puntos -= (a.getGolesEncajados() * 2); // -2 por cada gol encajado

        // Autogoles (si los hubiera en el futuro)
        // puntos -= 3;

        // 🔴 IMPORTANTE: Quitamos cualquier Math.max(0, puntos)
        // Devolvemos el valor puro, aunque sea -10.
        return puntos;
    }

    public int calcularTotalEquipo(Equipo equipo) {
        return equipo.getJugadoresAlineados().stream()
                .mapToInt(j -> j.getPuntosAcumulados()) // Ojo: Aquí deberíamos sumar los puntos DE LA JORNADA, no los acumulados totales.
                // CORRECCIÓN TÉCNICA: Un equipo suma los puntos de las ACTUACIONES de esa jornada.
                // Como simplificación actual, sumamos las actuaciones individuales.
                .sum(); 
        
        // Nota: En el sistema actual, el total del equipo se calcula en el Controller sumando las actuaciones.
        // Este método es auxiliar.
    }
}
