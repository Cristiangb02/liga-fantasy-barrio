package com.fantasy.ligabarrio.repository;

import com.fantasy.ligabarrio.model.Jugador;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface JugadorRepository extends JpaRepository<Jugador, Long> {
    
    List<Jugador> findByNombre(String nombre);
    List<Jugador> findByNombreAndPosicion(String nombre, String posicion);
}
