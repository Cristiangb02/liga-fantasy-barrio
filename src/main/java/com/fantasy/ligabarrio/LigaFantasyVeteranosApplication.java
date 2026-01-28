package com.fantasy.ligabarrio;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.util.List;
import java.util.TimeZone;

@SpringBootApplication
public class LigaFantasyVeteranosApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Madrid"));
        SpringApplication.run(LigaFantasyVeteranosApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(UsuarioRepository usuarioRepository, JornadaRepository jornadaRepository) {
        return args -> {
            if (jornadaRepository.count() == 0) {
                jornadaRepository.save(new Jornada());
            }

            Usuario admin = usuarioRepository.findByNombre("Cristiangb02");

            if (admin == null) {
                admin = new Usuario();
                admin.setNombre("Cristiangb02");
                admin.setPresupuesto(100_000_000);
                admin.setEsAdmin(true);
            }

            admin.setPassword("Huelvamolamazo");
            usuarioRepository.save(admin);
        };
    }
}
