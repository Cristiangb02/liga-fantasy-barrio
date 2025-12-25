package com.fantasy.ligabarrio;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JugadorRepository extends JpaRepository<Jugador, Long> {
    
    // Búsqueda simple (útil para otras cosas)
    List<Jugador> findByNombre(String nombre);
    
    // 🔴 BÚSQUEDA EXACTA: Nombre Y Posición
    // Esto permite que existan "Diego PORTERO" y "Diego DEFENSA" como personas distintas
    List<Jugador> findByNombreAndPosicion(String nombre, String posicion);
}
