package com.fantasy.ligabarrio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JugadorRepository extends JpaRepository<Jugador, Long> {

List<Jugador> findByNombre(String nombre);
}

