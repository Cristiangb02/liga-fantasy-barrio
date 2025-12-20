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
            // 1. INICIALIZAR JORNADA SI NO EXISTE
            if (jornadaRepository.count() == 0) {
                jornadaRepository.save(new Jornada());
                System.out.println(">>> ✅ Jornada 1 creada automáticamente.");
            }

            // 2. BUSCAR SI YA EXISTE EL ADMIN (Para evitar duplicados al reiniciar)
            // Buscamos por nombre, si devuelve una lista vacía o null, lo creamos.
            // Usamos una lista para evitar errores si ya hubiera duplicados previos.
            List<Usuario> admins = usuarioRepository.findByNombre("Cristian");
            
            if (admins.isEmpty()) {
                Usuario admin = new Usuario();
                admin.setNombre("Cristian");
                admin.setContrasena("1234");
                admin.setPresupuesto(100_000_000); // 100M Iniciales
                admin.setEsAdmin(true);
                usuarioRepository.save(admin);
                System.out.println(">>> 👑 ADMIN 'Cristian' creado con éxito.");
            } else {
                System.out.println(">>> ℹ️ El Admin 'Cristian' ya existe. No se crea de nuevo.");
            }
        };
    }
