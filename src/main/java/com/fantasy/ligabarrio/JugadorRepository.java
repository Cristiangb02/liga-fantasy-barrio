// Archivo: src/main/java/com/fantasy/ligabarrio/JugadorRepository.java

package com.fantasy.ligabarrio;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JugadorRepository extends JpaRepository<Jugador, Long> {
    
    // IMPORTANTE: Devuelve List<Jugador> para no fallar si hay duplicados
    List<Jugador> findByNombre(String nombre);
    
    // IMPORTANTE: Devuelve List<Jugador> para permitir "Diego Portero" y "Diego Defensa"
    List<Jugador> findByNombreAndPosicion(String nombre, String posicion);
}
