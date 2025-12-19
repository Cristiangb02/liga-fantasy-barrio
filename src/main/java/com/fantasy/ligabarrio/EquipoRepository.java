package com.fantasy.ligabarrio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EquipoRepository extends JpaRepository<Equipo, Long> {
    // Esto nos vendrá genial para sacar la clasificación: "Dame todos los equipos de la Jornada 1"
    List<Equipo> findByJornada(Jornada jornada);

    // O para ver el historial de un amigo: "Dame todos los equipos que ha hecho Pepito"
    List<Equipo> findByUsuario(Usuario usuario);
}