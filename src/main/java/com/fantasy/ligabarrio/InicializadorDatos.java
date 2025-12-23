package com.fantasy.ligabarrio;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

@Component
public class InicializadorDatos implements CommandLineRunner {

    private final JugadorRepository jugadorRepository;
    private final TemporadaRepository temporadaRepository;
    private final JornadaRepository jornadaRepository;
    private final UsuarioRepository usuarioRepository;
    private final EquipoRepository equipoRepository;

    public InicializadorDatos(JugadorRepository jugadorRepository, TemporadaRepository temporadaRepository, JornadaRepository jornadaRepository, UsuarioRepository usuarioRepository, EquipoRepository equipoRepository) {
        this.jugadorRepository = jugadorRepository;
        this.temporadaRepository = temporadaRepository;
        this.jornadaRepository = jornadaRepository;
        this.usuarioRepository = usuarioRepository;
        this.equipoRepository = equipoRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        // 1. TEMPORADA
        Temporada t2026;
        if (temporadaRepository.count() == 0) {
            t2026 = new Temporada(2026);
            temporadaRepository.save(t2026);
        } else {
            t2026 = temporadaRepository.findAll().get(0);
        }

        // 2. JUGADORES
        if (jugadorRepository.count() == 0) {
            List<Jugador> lista = new ArrayList<>();
            
            // --- PORTEROS ---
            lista.add(new Jugador("Alfonso", "PORTERO", 13_250_000, "/images/alfonso.png"));
            lista.add(new Jugador("Antonio", "PORTERO", 4_170_000, "/images/antonioportero.png"));
            lista.add(new Jugador("Aitor", "PORTERO", 3_700_000, "/images/user.png"));
            lista.add(new Jugador("Carlos", "PORTERO", 8_130_000, "/images/carlos.png"));
            lista.add(new Jugador("Carmelo", "PORTERO", 1_760_000, "/images/user.png"));
            lista.add(new Jugador("Cristian", "PORTERO", 6_150_000, "/images/cristian.png"));
            lista.add(new Jugador("Diego", "PORTERO", 4_510_000, "/images/diego.png"));
            lista.add(new Jugador("Fran", "PORTERO", 2_580_000, "/images/franportero.png"));
            lista.add(new Jugador("Jhona", "PORTERO", 14_500_000, "/images/jhona.png"));
            lista.add(new Jugador("Juanlu", "PORTERO", 3_850_000, "/images/juanlu.png"));
            lista.add(new Jugador("Sergio", "PORTERO", 6_570_000, "/images/sergio.png"));

            // --- DEFENSAS (Centrales + Laterales) ---
            lista.add(new Jugador("Cardenas", "DEFENSA", 2_370_000, "/images/cardenas.png"));
            lista.add(new Jugador("Chico", "DEFENSA", 1_760_000, "/images/user.png")); // Chico C
            lista.add(new Jugador("Conce", "DEFENSA", 2_380_000, "/images/conce.png"));
            lista.add(new Jugador("Diego", "DEFENSA", 14_840_000, "/images/diego.png")); // Diego C
            lista.add(new Jugador("Javi", "DEFENSA", 3_280_000, "/images/javier.png"));
            lista.add(new Jugador("Javier M.", "DEFENSA", 2_260_000, "/images/javierm.png"));
            lista.add(new Jugador("Jose", "DEFENSA", 14_070_000, "/images/jose.png"));
            lista.add(new Jugador("Juanlu", "DEFENSA", 3_700_000, "/images/juanlu.png"));
            lista.add(new Jugador("Mario", "DEFENSA", 5_260_000, "/images/mario.png"));
            lista.add(new Jugador("Miguel", "DEFENSA", 2_200_000, "/images/user.png"));
            lista.add(new Jugador("Primo", "DEFENSA", 11_410_000, "/images/primo.png")); // Primo C
            lista.add(new Jugador("Andrés", "DEFENSA", 1_340_000, "/images/andres.png"));
            lista.add(new Jugador("Álvaro", "DEFENSA", 5_670_000, "/images/user.png"));
            lista.add(new Jugador("Cárdenas", "DEFENSA", 2_380_000, "/images/cardenas.png")); // Cardenas L
            lista.add(new Jugador("Chico", "DEFENSA", 3_560_000, "/images/user.png")); // Chico L
            lista.add(new Jugador("Conce", "DEFENSA", 5_260_000, "/images/conce.png")); // Conce L
            lista.add(new Jugador("Cristian", "DEFENSA", 8_950_000, "/images/cristian.png"));
            lista.add(new Jugador("Diego", "DEFENSA", 6_570_000, "/images/diego.png")); // Diego L
            lista.add(new Jugador("Javier M", "DEFENSA", 1_290_000, "/images/javierm.png")); // Javier M L
            lista.add(new Jugador("Javi", "DEFENSA", 1_360_000, "/images/javier.png")); // Javi L
            lista.add(new Jugador("Jhona", "DEFENSA", 3_560_000, "/images/jhona.png"));
            lista.add(new Jugador("Jose", "DEFENSA", 3_180_000, "/images/jose.png")); // Jose L
            lista.add(new Jugador("Juan", "DEFENSA", 1_530_000, "/images/juan.png"));
            lista.add(new Jugador("Juanlu", "DEFENSA", 3_880_000, "/images/juanlu.png")); // Juanlu L
            lista.add(new Jugador("Lucas", "DEFENSA", 3_700_000, "/images/lucas.png"));
            lista.add(new Jugador("Luis", "DEFENSA", 4_200_000, "/images/luis.png"));
            lista.add(new Jugador("Manuel hijo Luis", "DEFENSA", 5_760_000, "/images/user.png"));
            lista.add(new Jugador("Mario", "DEFENSA", 6_110_000, "/images/mario.png")); // Mario L
            lista.add(new Jugador("Paco", "DEFENSA", 4_510_000, "/images/paco.png"));
            lista.add(new Jugador("Primo", "DEFENSA", 6_340_000, "/images/primo.png")); // Primo L

            // --- MEDIOS ---
            lista.add(new Jugador("Alberto", "MEDIO", 9_320_000, "/images/alberto.png"));
            lista.add(new Jugador("Álvaro", "MEDIO", 5_670_000, "/images/user.png"));
            lista.add(new Jugador("Alberto hijo Javier", "MEDIO", 12_850_000, "/images/user.png"));
            lista.add(new Jugador("Alejandro", "MEDIO", 9_320_000, "/images/alejandro.png"));
            lista.add(new Jugador("Alejandro G.", "MEDIO", 8_650_000, "/images/alejandrogarrocho.png"));
            lista.add(new Jugador("Cardenas", "MEDIO", 5_890_000, "/images/cardenas.png")); // Cardenas M
            lista.add(new Jugador("Conce", "MEDIO", 8_590_000, "/images/conce.png")); // Conce M
            lista.add(new Jugador("Cristian", "MEDIO", 4_870_000, "/images/cristian.png")); // Cristian M
            lista.add(new Jugador("David", "MEDIO", 3_030_000, "/images/user.png"));
            lista.add(new Jugador("Diego", "MEDIO", 1_890_000, "/images/diego.png")); // Diego M
            lista.add(new Jugador("Felipe", "MEDIO", 7_860_000, "/images/felipe.png"));
            lista.add(new Jugador("Jesús Jr", "MEDIO", 15_380_000, "/images/jesusjr.png"));
            lista.add(new Jugador("Jose", "MEDIO", 2_380_000, "/images/jose.png")); // Jose M
            lista.add(new Jugador("Lucas", "MEDIO", 3_850_000, "/images/lucas.png")); // Lucas M
            lista.add(new Jugador("Luis", "MEDIO", 5_500_000, "/images/luis.png")); // Luis M
            lista.add(new Jugador("Manuel hijo Luis", "MEDIO", 9_320_000, "/images/user.png")); // Manuel M
            lista.add(new Jugador("Oswaldo", "MEDIO", 4_510_000, "/images/oswi.png"));
            lista.add(new Jugador("Pepe", "MEDIO", 3_850_000, "/images/pepe.png"));
            lista.add(new Jugador("Primo", "MEDIO", 2_380_000, "/images/primo.png")); // Primo M
            lista.add(new Jugador("Sebas", "MEDIO", 21_470_000, "/images/user.png"));
            lista.add(new Jugador("Sergio", "MEDIO", 17_250_000, "/images/sergio.png"));

            // --- DELANTEROS ---
            lista.add(new Jugador("Alberto", "DELANTERO", 5_460_000, "/images/alberto.png")); // Alberto DC
            lista.add(new Jugador("Cristian", "DELANTERO", 3_560_000, "/images/cristian.png")); // Cristian DC
            lista.add(new Jugador("Jesus", "DELANTERO", 4_870_000, "/images/jesus.png"));
            lista.add(new Jugador("Jesús Jr", "DELANTERO", 6_570_000, "/images/jesusjr.png")); // Jesus Jr DC
            lista.add(new Jugador("Jhona", "DELANTERO", 3_560_000, "/images/jhona.png")); // Jhona DC
            lista.add(new Jugador("Jose", "DELANTERO", 2_800_000, "/images/jose.png")); // Jose DC
            lista.add(new Jugador("Juanlu", "DELANTERO", 2_380_000, "/images/juanlu.png")); // Juanlu DC
            lista.add(new Jugador("Pepe", "DELANTERO", 5_260_000, "/images/pepe.png")); // Pepe DC
            lista.add(new Jugador("Raúl", "DELANTERO", 6_570_000, "/images/user.png"));

            jugadorRepository.saveAll(lista);
        }

        // 3. JORNADA
        Jornada jornada1;
        if (jornadaRepository.count() == 0) {
            jornada1 = new Jornada(1, LocalDate.now(), t2026);
            jornadaRepository.save(jornada1);
        } else {
            jornada1 = jornadaRepository.findAll().get(0);
        }

        // 4. CREAR USUARIO ADMIN
        if (usuarioRepository.count() == 0) {
            Usuario admin = new Usuario("Cristian", "Huelvamolamazo", 100_000_000, true);
            admin.setActivo(true);
            usuarioRepository.save(admin);
            System.out.println(">>> Inicialización completada. Usuario 'Cristian' es ADMIN.");
        }
    }
}

