package com.fantasy.ligabarrio;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LigaFantasyVeteranosApplication {

    public static void main(String[] args) {
        SpringApplication.run(LigaFantasyVeteranosApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(UsuarioRepository usuarioRepository, JornadaRepository jornadaRepository) {
        return args -> {
            // 1. INICIALIZAR JORNADA SI NO EXISTE
            if (jornadaRepository.count() == 0) {
                jornadaRepository.save(new Jornada());
                System.out.println(">>> ✅ Jornada 1 creada automáticamente.");
            }

            // 2. BUSCAR SI YA EXISTE EL ADMIN
            // CORRECCIÓN: Tu repositorio devuelve un objeto Usuario único, no una lista.
            Usuario existingAdmin = usuarioRepository.findByNombre("Cristian");
            
            if (existingAdmin == null) {
                Usuario admin = new Usuario();
                admin.setNombre("Cristian");
                
                // CORRECCIÓN: Probamos con el estándar 'setPassword'.
                // Si esto fallara, tendrías que mirar tu archivo Usuario.java para ver cómo se llama el campo.
                admin.setPassword("1234"); 
                
                admin.setPresupuesto(100_000_000); 
                admin.setEsAdmin(true);
                usuarioRepository.save(admin);
                System.out.println(">>> 👑 ADMIN 'Cristian' creado con éxito.");
            } else {
                System.out.println(">>> ℹ️ El Admin 'Cristian' ya existe. No se crea de nuevo.");
            }
        };
    }
}
