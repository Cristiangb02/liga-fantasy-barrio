package com.fantasy.ligabarrio;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List; // 🔴 IMPORTANTE: Faltaba importar List

public interface ActuacionRepository extends JpaRepository<Actuacion, Long> {
    
    // Para buscar si ya jugó en una jornada concreta (para no repetir)
    Optional<Actuacion> findByJugadorAndJornada(Jugador jugador, Jornada jornada);

    // 🔴 ESTA ES LA LÍNEA QUE FALTABA Y DABA EL ERROR
    // Sirve para sacar el historial completo de un jugador
    List<Actuacion> findByJugador(Jugador jugador);
}
