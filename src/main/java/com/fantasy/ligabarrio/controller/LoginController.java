package com.fantasy.ligabarrio.controller;

import com.fantasy.ligabarrio.model.Noticia;
import com.fantasy.ligabarrio.repository.NoticiaRepository;
import com.fantasy.ligabarrio.model.Usuario;
import com.fantasy.ligabarrio.repository.UsuarioRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/auth")
public class LoginController {

    private final UsuarioRepository usuarioRepository;
    private final NoticiaRepository noticiaRepository;

    public LoginController(UsuarioRepository usuarioRepository, NoticiaRepository noticiaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.noticiaRepository = noticiaRepository;
    }

    @PostMapping("/registro")
    public String registrarUsuario(@RequestBody Usuario datos) {
        String resultado;

        if (usuarioRepository.findByNombre(datos.getNombre()) != null) {
            resultado = "❌ El nombre ya existe.";
        } else {
            boolean esPrimero = false;
            if (usuarioRepository.count() == 0) {
                esPrimero = true;
            }

            Usuario nuevo = new Usuario(datos.getNombre(), datos.getPassword(), 100_000_000, esPrimero);
            nuevo.setActivo(esPrimero);
            usuarioRepository.save(nuevo);

            if (esPrimero) {
                noticiaRepository.save(new Noticia(datos.getNombre() + " ha inaugurado la liga como Admin."));
                resultado = "✅ ¡Liga inaugurada! Eres el Admin.";
            } else {
                resultado = "✅ Solicitud enviada. Contacta con el creador de la app por Whatsapp para que te acepte y luego pulsa el botón 'Entrar'.";
            }
        }

        return resultado;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Usuario datos) {
        Map<String, Object> resultado;
        Usuario user = usuarioRepository.findByNombre(datos.getNombre());

        if (user == null || !user.getPassword().equals(datos.getPassword())) {
            resultado = Map.of("error", "Credenciales incorrectas.");
        } else if (!user.isActivo()) {
            resultado = Map.of("error", "⛔ Tu cuenta aún no ha sido aprobada por el Admin.");
        } else {
            resultado = new HashMap<>();
            resultado.put("id", user.getId());
            resultado.put("nombre", user.getNombre());
            resultado.put("esAdmin", user.isEsAdmin());
            resultado.put("presupuesto", user.getPresupuesto());

            if (user.getUrlImagen() != null) {
                resultado.put("urlImagen", user.getUrlImagen());
            } else {
                resultado.put("urlImagen", "");
            }
        }

        return resultado;
    }
}