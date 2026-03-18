package com.fantasy.ligabarrio.repository;

import com.fantasy.ligabarrio.model.Jugador;
import com.fantasy.ligabarrio.model.Oferta;
import com.fantasy.ligabarrio.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {
    List<Oferta> findByVendedorAndEstado(Usuario vendedor, String estado);
    List<Oferta> findByCompradorAndEstado(Usuario comprador, String estado);
    List<Oferta> findByJugador(Jugador jugador);
}