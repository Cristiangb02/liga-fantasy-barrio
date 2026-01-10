package com.fantasy.ligabarrio;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List; 

public interface ActuacionRepository extends JpaRepository<Actuacion, Long> {
    
    Optional<Actuacion> findByJugadorAndJornada(Jugador jugador, Jornada jornada);
    List<Actuacion> findByJugador(Jugador jugador);
}

