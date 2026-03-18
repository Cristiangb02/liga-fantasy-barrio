package com.fantasy.ligabarrio.controller;

import com.fantasy.ligabarrio.model.Noticia;
import com.fantasy.ligabarrio.repository.NoticiaRepository;
import com.fantasy.ligabarrio.model.Usuario;
import com.fantasy.ligabarrio.repository.UsuarioRepository;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final NoticiaRepository noticiaRepository;

    public AuthController(UsuarioRepository usuarioRepository, NoticiaRepository noticiaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.noticiaRepository = noticiaRepository;
    }

    @PostMapping("/registro")
    public String registrarUsuario(@RequestBody Usuario datos) {
        if (usuarioRepository.findByNombre(datos.getNombre()) != null) {
            return "❌ El nombre ya existe.";
        }

        boolean esPrimero = usuarioRepository.count() == 0;
        Usuario nuevo = new Usuario(datos.getNombre(), datos.getPassword(), 100_000_000, esPrimero);
        nuevo.setActivo(esPrimero);
        usuarioRepository.save(nuevo);

        if (esPrimero) {
            noticiaRepository.save(new Noticia("👑 FUNDADOR: " + datos.getNombre() + " ha inaugurado la liga como Admin."));
            return "✅ ¡Liga inaugurada! Eres el Admin.";
        } else {
            return "✅ Solicitud enviada. Contacta con el creador de la app por Whatsapp para que te acepte y luego pulsa el botón 'Entrar'.";
        }
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Usuario datos) {
        Usuario user = usuarioRepository.findByNombre(datos.getNombre());
        if (user == null || !user.getPassword().equals(datos.getPassword())) {
            return Map.of("error", "Credenciales incorrectas.");
        }
        if (!user.isActivo()) {
            return Map.of("error", "⛔ Tu cuenta aún no ha sido aprobada por el Admin.");
        }
        return Map.of(
                "id", user.getId(),
                "nombre", user.getNombre(),
                "esAdmin", user.isEsAdmin(),
                "presupuesto", user.getPresupuesto()
        );
    }
}