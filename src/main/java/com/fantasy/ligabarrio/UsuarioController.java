package com.fantasy.ligabarrio;

import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/auth")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // REGISTRO
    @PostMapping("/registro")
    public Usuario registrar(@RequestBody Usuario usuario) {
        if (usuarioRepository.findByNombre(usuario.getNombre()) != null) {
            throw new RuntimeException("¡Ese nombre ya está cogido!");
        }

        usuario.setPresupuesto(100_000_000);

        // SEGURIDAD: Los nuevos registros SIEMPRE son usuarios normales (false)
        usuario.setEsAdmin(false);

        return usuarioRepository.save(usuario);
    }

    // LOGIN
    @PostMapping("/login")
    public Usuario login(@RequestBody Usuario loginDatos) {
        Usuario usuarioEncontrado = usuarioRepository.findByNombre(loginDatos.getNombre());

        if (usuarioEncontrado != null && usuarioEncontrado.getPassword().equals(loginDatos.getPassword())) {
            return usuarioEncontrado;
        } else {
            throw new RuntimeException("Usuario o contraseña incorrectos");
        }
    }
}