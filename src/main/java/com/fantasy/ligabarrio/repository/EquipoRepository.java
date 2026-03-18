package com.fantasy.ligabarrio.repository;

import com.fantasy.ligabarrio.model.Equipo;
import com.fantasy.ligabarrio.model.Jornada;
import com.fantasy.ligabarrio.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EquipoRepository extends JpaRepository<Equipo, Long> {
    
    List<Equipo> findByJornada(Jornada jornada);
    List<Equipo> findByUsuario(Usuario usuario);

}
