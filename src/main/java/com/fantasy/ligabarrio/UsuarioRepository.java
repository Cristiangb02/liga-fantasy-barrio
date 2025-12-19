package com.fantasy.ligabarrio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // Esto es muy útil: nos permitirá buscar usuarios por su nombre (para el login futuro)
    Usuario findByNombre(String nombre);
}