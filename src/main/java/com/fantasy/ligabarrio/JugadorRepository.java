package com.fantasy.ligabarrio;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JugadorRepository extends JpaRepository<Jugador, Long> {
    
    List<Jugador> findByNombre(String nombre);
    List<Jugador> findByNombreAndPosicion(String nombre, String posicion);
}
