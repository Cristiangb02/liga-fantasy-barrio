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
    private final ActuacionRepository actuacionRepository;
    private final NoticiaRepository noticiaRepository;

    public InicializadorDatos(JugadorRepository jugadorRepository, 
                              TemporadaRepository temporadaRepository, 
                              JornadaRepository jornadaRepository, 
                              UsuarioRepository usuarioRepository, 
                              EquipoRepository equipoRepository,
                              ActuacionRepository actuacionRepository,
                              NoticiaRepository noticiaRepository) {
        this.jugadorRepository = jugadorRepository;
        this.temporadaRepository = temporadaRepository;
        this.jornadaRepository = jornadaRepository;
        this.usuarioRepository = usuarioRepository;
        this.equipoRepository = equipoRepository;
        this.actuacionRepository = actuacionRepository;
        this.noticiaRepository = noticiaRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        System.out.println(">>> 🔄 CHECKING DATA...");

        // 1. TEMPORADA
        if (temporadaRepository.count() == 0) {
            Temporada t2026 = new Temporada(2026);
            temporadaRepository.save(t2026);
        }

        // 2. JUGADORES
        List<Jugador> lista = new ArrayList<>();
        
        // --- PORTEROS ---

        lista.add(new Jugador("Aitor", "PORTERO", 1_000_000, "/user.png"));
        lista.add(new Jugador("Carlos", "PORTERO", 8_130_000, "/carlos.png"));
        lista.add(new Jugador("Carmelo", "PORTERO", 1_760_000, "/user.png"));
        lista.add(new Jugador("Cristian", "PORTERO", 6_150_000, "/cristian.png"));
        lista.add(new Jugador("Diego", "PORTERO", 4_510_000, "/diego.png"));
        lista.add(new Jugador("Fran", "PORTERO", 2_580_000, "/franportero.png"));
        lista.add(new Jugador("Jhona", "PORTERO", 14_500_000, "/jhona.png"));
        lista.add(new Jugador("Juanlu", "PORTERO", 3_850_000, "/juanlu.png"));
        lista.add(new Jugador("Sergio", "PORTERO", 6_570_000, "/sergio.png"));

        // --- DEFENSAS ---
        lista.add(new Jugador("Cardenas", "DEFENSA", 2_370_000, "/cardenas.png"));
        lista.add(new Jugador("Chico", "DEFENSA", 1_760_000, "/user.png")); 
        lista.add(new Jugador("Conce", "DEFENSA", 2_380_000, "/conce.png"));
        lista.add(new Jugador("Diego", "DEFENSA", 14_840_000, "/diego.png")); 
        lista.add(new Jugador("Javi", "DEFENSA", 3_280_000, "/javier.png"));
        lista.add(new Jugador("Javier M.", "DEFENSA", 2_260_000, "/javierm.png"));
        lista.add(new Jugador("Jose", "DEFENSA", 14_070_000, "/jose.png"));
        lista.add(new Jugador("Juanlu", "DEFENSA", 3_700_000, "/juanlu.png"));
        lista.add(new Jugador("Mario", "DEFENSA", 5_260_000, "/mario.png"));
        lista.add(new Jugador("Miguel", "DEFENSA", 2_200_000, "/user.png"));
        lista.add(new Jugador("Primo", "DEFENSA", 11_410_000, "/primo.png")); 
        lista.add(new Jugador("Andrés", "DEFENSA", 1_340_000, "/andres.png"));
        lista.add(new Jugador("Álvaro", "DEFENSA", 5_670_000, "/user.png"));
        lista.add(new Jugador("Cárdenas", "DEFENSA", 2_380_000, "/cardenas.png")); 
        lista.add(new Jugador("Chico", "DEFENSA", 3_560_000, "/user.png")); 
        lista.add(new Jugador("Conce", "DEFENSA", 5_260_000, "/conce.png")); 
        lista.add(new Jugador("Cristian", "DEFENSA", 8_950_000, "/cristian.png"));
        lista.add(new Jugador("Diego", "DEFENSA", 6_570_000, "/diego.png")); 
        lista.add(new Jugador("Javier M", "DEFENSA", 1_290_000, "/javierm.png")); 
        lista.add(new Jugador("Javi", "DEFENSA", 1_360_000, "/javier.png")); 
        lista.add(new Jugador("Jhona", "DEFENSA", 3_560_000, "/jhona.png"));
        lista.add(new Jugador("Jose", "DEFENSA", 3_180_000, "/jose.png")); 
        lista.add(new Jugador("Juan", "DEFENSA", 1_530_000, "/juan.png"));
        lista.add(new Jugador("Juanlu", "DEFENSA", 3_880_000, "/juanlu.png")); 
        lista.add(new Jugador("Lucas", "DEFENSA", 3_700_000, "/lucas.png"));
        lista.add(new Jugador("Luis", "DEFENSA", 4_200_000, "/luis.png"));
        lista.add(new Jugador("Manuel hijo Luis", "DEFENSA", 5_760_000, "/user.png"));
        lista.add(new Jugador("Mario", "DEFENSA", 6_110_000, "/mario.png")); 
        lista.add(new Jugador("Paco", "DEFENSA", 4_510_000, "/paco.png"));
        lista.add(new Jugador("Primo", "DEFENSA", 6_340_000, "/primo.png")); 

        // --- MEDIOS ---
        lista.add(new Jugador("Alberto", "MEDIO", 9_320_000, "/alberto.png"));
        lista.add(new Jugador("Álvaro", "MEDIO", 5_670_000, "/user.png"));
        lista.add(new Jugador("Alberto hijo Javier", "MEDIO", 12_850_000, "/user.png"));
        lista.add(new Jugador("Alejandro", "MEDIO", 9_320_000, "/alejandro.png"));
        lista.add(new Jugador("Alejandro G.", "MEDIO", 8_650_000, "/alejandrogarrocho.png"));
        lista.add(new Jugador("Cardenas", "MEDIO", 5_890_000, "/cardenas.png")); 
        lista.add(new Jugador("Conce", "MEDIO", 8_590_000, "/conce.png")); 
        lista.add(new Jugador("Cristian", "MEDIO", 4_870_000, "/cristian.png")); 
        lista.add(new Jugador("David", "MEDIO", 3_030_000, "/user.png"));
        lista.add(new Jugador("Diego", "MEDIO", 1_890_000, "/diego.png")); 
        lista.add(new Jugador("Felipe", "MEDIO", 7_860_000, "/felipe.png"));
        lista.add(new Jugador("Jesús Jr", "MEDIO", 15_380_000, "/jesusjr.png"));
        lista.add(new Jugador("Jose", "MEDIO", 2_380_000, "/jose.png")); 
        lista.add(new Jugador("Lucas", "MEDIO", 3_850_000, "/lucas.png")); 
        lista.add(new Jugador("Luis", "MEDIO", 5_500_000, "/luis.png")); 
        lista.add(new Jugador("Manuel hijo Luis", "MEDIO", 9_320_000, "/user.png")); 
        lista.add(new Jugador("Oswaldo", "MEDIO", 4_510_000, "/oswi.png"));
        lista.add(new Jugador("Pepe", "MEDIO", 3_850_000, "/pepe.png"));
        lista.add(new Jugador("Primo", "MEDIO", 2_380_000, "/primo.png")); 
        lista.add(new Jugador("Sebas", "MEDIO", 21_470_000, "/user.png"));
        lista.add(new Jugador("Sergio", "MEDIO", 17_250_000, "/sergio.png"));

        // --- DELANTEROS ---
        lista.add(new Jugador("Alberto", "DELANTERO", 5_460_000, "/alberto.png")); 
        lista.add(new Jugador("Cristian", "DELANTERO", 3_560_000, "/cristian.png")); 
        lista.add(new Jugador("Jesus", "DELANTERO", 4_870_000, "/jesus.png"));
        lista.add(new Jugador("Jesús Jr", "DELANTERO", 6_570_000, "/jesusjr.png")); 
        lista.add(new Jugador("Jhona", "DELANTERO", 3_560_000, "/jhona.png")); 
        lista.add(new Jugador("Jose", "DELANTERO", 2_800_000, "/jose.png")); 
        lista.add(new Jugador("Juanlu", "DELANTERO", 2_380_000, "/juanlu.png")); 
        lista.add(new Jugador("Pepe", "DELANTERO", 5_260_000, "/pepe.png")); 
        lista.add(new Jugador("Raúl", "DELANTERO", 6_570_000, "/user.png"));

        // 🔴 LÓGICA CORREGIDA: Buscamos por NOMBRE + POSICIÓN
        for (Jugador j : lista) {
            // "Dame todos los jugadores que se llamen 'Diego' Y jueguen de 'DEFENSA'"
            List<Jugador> existentes = jugadorRepository.findByNombreAndPosicion(j.getNombre(), j.getPosicion());
            
            if (existentes.isEmpty()) {
                // Si no existe esa combinación exacta, lo creamos
                jugadorRepository.save(j);
                System.out.println("✅ NUEVO JUGADOR: " + j.getNombre() + " (" + j.getPosicion() + ")");
            } else {
                // Si ya existe (aunque sea un clon), no creamos más
            }
        }

        // 3. JORNADA
        if (jornadaRepository.count() == 0) {
            Jornada jornada1 = new Jornada(1, LocalDate.now(), new Temporada(2026));
            jornadaRepository.save(jornada1);
        }

        // 4. CREAR USUARIO ADMIN
        if (usuarioRepository.findByNombre("Cristian") == null) {
            Usuario admin = new Usuario("Cristian", "Huelvamolamazo", 100_000_000, true);
            admin.setActivo(true);
            usuarioRepository.save(admin);
            System.out.println("👑 ADMIN CREADO");
        }
        
        System.out.println(">>> ✅ CARGA DE DATOS INTELIGENTE COMPLETADA.");
    }
}
