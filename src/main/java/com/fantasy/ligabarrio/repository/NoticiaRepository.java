package com.fantasy.ligabarrio.repository;

import com.fantasy.ligabarrio.model.Noticia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoticiaRepository extends JpaRepository<Noticia, Long> {
    List<Noticia> findAllByOrderByFechaDesc();
}