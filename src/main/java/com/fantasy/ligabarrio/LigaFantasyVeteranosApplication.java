package com.fantasy.ligabarrio;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.util.List;

@SpringBootApplication
public class LigaFantasyVeteranosApplication {

    public static void main(String[] args) {
        SpringApplication.run(LigaFantasyVeteranosApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(UsuarioRepository usuarioRepository, JornadaRepository jornadaRepository) {
        return args -> {
            // 1. INICIALIZAR JORNADA
            if (jornadaRepository.count() == 0) {
                jornadaRepository.save(new Jornada());
                System.out.println(">>> ✅ Jornada 1 creada.");
            }

            // 2. DIAGNÓSTICO: ¿QUIÉN ESTÁ EN LA BASE DE DATOS?
            List<Usuario> todos = usuarioRepository.findAll();
            System.out.println(">>> 📊 REPORTE DE USUARIOS EN DB (" + todos.size() + "):");
            for (Usuario u : todos) {
                System.out.println("   👤 Usuario: " + u.getNombre() + " | Pass: " + u.getPassword());
            }

            // 3. ASEGURAR ADMIN CRISTIAN
            Usuario admin = usuarioRepository.findByNombre("Cristian");
            
            if (admin == null) {
                admin = new Usuario();
                admin.setNombre("Cristian");
                admin.setPresupuesto(100_000_000);
                admin.setEsAdmin(true);
                System.out.println(">>> 🆕 Creando usuario 'Cristian' desde cero...");
            } else {
                System.out.println(">>> ♻️ Usuario 'Cristian' encontrado. Actualizando contraseña...");
            }

            // 4. FORZAR LA CONTRASEÑA CORRECTA SIEMPRE
            admin.setPassword("1234");
            usuarioRepository.save(admin);
            
            System.out.println(">>> 👑 ADMIN 'Cristian' LISTO con contraseña '1234'. ¡Prueba a entrar ahora!");
        };
    }
}
