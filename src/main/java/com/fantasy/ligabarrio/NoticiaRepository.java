package com.fantasy.ligabarrio;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoticiaRepository extends JpaRepository<Noticia, Long> {
    // Buscamos las noticias ordenadas: las más nuevas primero
    List<Noticia> findAllByOrderByFechaDesc();
}