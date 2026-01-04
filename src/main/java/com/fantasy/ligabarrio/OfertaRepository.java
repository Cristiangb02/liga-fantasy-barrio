package com.fantasy.ligabarrio;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {
    List<Oferta> findByVendedorAndEstado(Usuario vendedor, String estado);
    List<Oferta> findByCompradorAndEstado(Usuario comprador, String estado);
    List<Oferta> findByJugador(Jugador jugador);
}