package com.fantasy.ligabarrio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional; // <--- Importante añadir esto

@Repository
public interface ActuacionRepository extends JpaRepository<Actuacion, Long> {

    // Método Nuevo: Busca qué hizo un jugador concreto en un día concreto
    Optional<Actuacion> findByJugadorAndJornada(Jugador jugador, Jornada jornada);
}