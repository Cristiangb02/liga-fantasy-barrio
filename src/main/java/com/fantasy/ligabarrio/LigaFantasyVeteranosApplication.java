package com.fantasy.ligabarrio;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.util.List;
import java.util.TimeZone; // 🔴 IMPORTANTE

@SpringBootApplication
public class LigaFantasyVeteranosApplication {

    public static void main(String[] args) {
        //HORA ESPAÑOLA (EUROPE/MADRID)
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"));
        SpringApplication.run(LigaFantasyVeteranosApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(UsuarioRepository usuarioRepository, JornadaRepository jornadaRepository) {
        return args -> {
            //1. INICIALIZAR JORNADA
            if (jornadaRepository.count() == 0) {
                jornadaRepository.save(new Jornada());
                System.out.println(">>> ✅ Jornada 1 creada.");
            }

            // 2. ASEGURAR ADMIN CRISTIAN
            Usuario admin = usuarioRepository.findByNombre("Cristian");
            
            if (admin == null) {
                admin = new Usuario();
                admin.setNombre("Cristian");
                admin.setPresupuesto(100_000_000);
                admin.setEsAdmin(true);
                System.out.println(">>> 🆕 Creando usuario 'Cristian'...");
            } 

            //3. ASEGURAR CONTRASEÑA
            admin.setPassword("Huelvamolamazo");
            usuarioRepository.save(admin);
            
            System.out.println(">>> 👑 ADMIN 'Cristian' asegurado.");
        };
    }
}
