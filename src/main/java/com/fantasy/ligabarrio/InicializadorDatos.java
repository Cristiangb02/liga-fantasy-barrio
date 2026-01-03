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

        System.out.println(">>> 🔄 CARGANDO JUGADORES (DEFENSAS FUSIONADOS)...");

        // 1. TEMPORADA
        Temporada t2026;
        if (temporadaRepository.count() == 0) {
            t2026 = new Temporada(2026);
            temporadaRepository.save(t2026);
        } else {
            t2026 = temporadaRepository.findAll().get(0);
        }

        List<Jugador> lista = new ArrayList<>();

        // --- PORTEROS ---
        lista.add(new Jugador("Carlos", "PORTERO", 52, 6.8, "/carlos.png"));
        lista.add(new Jugador("Carmelo", "PORTERO", 54, 6.26, "/carmelo.png"));
        lista.add(new Jugador("Cristian", "PORTERO", 24, 7.26, "/cristian.png"));
        lista.add(new Jugador("Diego", "PORTERO", 49, 7.23, "/diego.png"));
        lista.add(new Jugador("Fran", "PORTERO", 53, 6.47, "/franportero.png"));
        lista.add(new Jugador("Jhona", "PORTERO", 48, 8.06, "/jhona.png"));
        lista.add(new Jugador("Juanlu", "PORTERO", 56, 5.98, "/juanlu.png"));
        lista.add(new Jugador("Sergio", "PORTERO", 34, 6.56, "/sergio.png"));

        // --- DEFENSAS (FUSIÓN CENTRALES + LATERALES) ---

        lista.add(new Jugador("Alejandro", "DEFENSA", 32, 6.63, "/alejandro.png")); // Solo era Lateral
        lista.add(new Jugador("Alejandro G.", "DEFENSA", 30, 6.58, "/alejandrogarrocho.png")); // (6.53 + 6.63)/2
        lista.add(new Jugador("Andrés", "DEFENSA", 52, 4.72, "/andres.png")); // (4.15 + 5.29)/2
        lista.add(new Jugador("Cardenas", "DEFENSA", 45, 6.20, "/cardenas.png")); // (6.16 + 6.23)/2
        lista.add(new Jugador("Chico", "DEFENSA", 46, 5.79, "/user.png")); // (5.58 + 5.99)/2
        lista.add(new Jugador("Conce", "DEFENSA", 29, 5.84, "/conce.png")); // (5.80 + 5.88)/2
        lista.add(new Jugador("Cristian", "DEFENSA", 24, 7.08, "/cristian.png")); // (5.99 + 8.16)/2
        lista.add(new Jugador("Diego", "DEFENSA", 49, 8.07, "/diego.png")); // (8.61 + 7.53)/2
        lista.add(new Jugador("Javier", "DEFENSA", 58, 6.06, "/javier.png")); // (5.97 + 6.14)/2
        lista.add(new Jugador("Javier M.", "DEFENSA", 54, 5.88, "/javierm.png")); // (6.24 + 5.52)/2
        lista.add(new Jugador("Jesús Jr", "DEFENSA", 25, 6.62, "/jesusjr.png")); // (6.28 + 6.96)/2
        lista.add(new Jugador("Jhona", "DEFENSA", 48, 6.29, "/jhona.png")); // (5.63 + 6.94)/2
        lista.add(new Jugador("Jose", "DEFENSA", 45, 7.03, "/jose.png")); // (7.53 + 6.52)/2
        lista.add(new Jugador("Juan", "DEFENSA", 45, 6.04, "/juan.png")); // (5.54 + 6.53)/2
        lista.add(new Jugador("Juanlu", "DEFENSA", 56, 6.89, "/juanlu.png")); // (6.70 + 7.08)/2
        lista.add(new Jugador("Lucas", "DEFENSA", 55, 6.50, "/lucas.png")); // (6.11 + 6.89)/2
        lista.add(new Jugador("Luis", "DEFENSA", 57, 6.19, "/luis.png")); // Solo era Lateral
        lista.add(new Jugador("Mario", "DEFENSA", 59, 7.01, "/mario.png")); // (6.34 + 7.67)/2
        lista.add(new Jugador("Paco", "DEFENSA", 62, 7.11, "/paco.png")); // (6.52 + 7.69)/2
        lista.add(new Jugador("Primo", "DEFENSA", 46, 6.87, "/primo.png")); // (7.26 + 6.47)/2
        lista.add(new Jugador("Sergio", "DEFENSA", 34, 6.98, "/sergio.png")); // Solo era Lateral

        // --- MEDIOS ---
        lista.add(new Jugador("Alberto", "MEDIO", 39, 8.06, "/alberto.png"));
        lista.add(new Jugador("Alejandro", "MEDIO", 32, 8.16, "/alejandro.png"));
        lista.add(new Jugador("Alejandro G.", "MEDIO", 30, 8.52, "/alejandrogarrocho.png"));
        lista.add(new Jugador("Cardenas", "MEDIO", 45, 6.8, "/cardenas.png"));
        lista.add(new Jugador("Conce", "MEDIO", 29, 7.13, "/conce.png"));
        lista.add(new Jugador("Cristian", "MEDIO", 24, 7.66, "/cristian.png"));
        lista.add(new Jugador("Felipe", "MEDIO", 31, 7.69, "/felipe.png"));
        lista.add(new Jugador("Jesús Jr", "MEDIO", 25, 7.53, "/jesusjr.png"));
        lista.add(new Jugador("Lucas", "MEDIO", 55, 5.85, "/lucas.png"));
        lista.add(new Jugador("Luis", "MEDIO", 57, 7.21, "/luis.png"));
        lista.add(new Jugador("Oswaldo", "MEDIO", 45, 8.19, "/oswi.png"));
        lista.add(new Jugador("Pepe", "MEDIO", 67, 6.97, "/pepe.png"));
        lista.add(new Jugador("Sebas", "MEDIO", 33, 8.13, "/sebastian.png"));
        lista.add(new Jugador("Sergio", "MEDIO", 34, 8.88, "/sergio.png"));

        // --- DELANTEROS ---
        lista.add(new Jugador("Cristian", "DELANTERO", 24, 7.96, "/cristian.png"));
        lista.add(new Jugador("Jesus", "DELANTERO", 42, 7.53, "/jesus.png"));
        lista.add(new Jugador("Jhona", "DELANTERO", 48, 6.16, "/jhona.png"));
        lista.add(new Jugador("Juan", "DELANTERO", 45, 5.97, "/juan.png"));
        lista.add(new Jugador("Juanlu", "DELANTERO", 56, 5.89, "/juanlu.png"));
        lista.add(new Jugador("Pepe", "DELANTERO", 67, 7.02, "/pepe.png"));

        //INSERCIÓN SEGURA (Evita duplicados)
        for (Jugador j : lista) {
            List<Jugador> existentes = jugadorRepository.findByNombreAndPosicion(j.getNombre(), j.getPosicion());

            if (existentes.isEmpty()) {
                jugadorRepository.save(j);
                System.out.println("✅ " + j.getNombre() + " (" + j.getPosicion() + ") -> " + j.getValor() + "€");
            }
        }

        //3. JORNADA
        if (jornadaRepository.count() == 0) {
            Jornada jornada1 = new Jornada(1, LocalDate.now(), t2026); // Usamos la temporada recuperada
            jornadaRepository.save(jornada1);
        }

        // 4. CREAR USUARIO ADMIN
        if (usuarioRepository.findByNombre("Cristian") == null) {
            Usuario admin = new Usuario("Cristian", "Huelvamolamazo", 100_000_000, true);
            admin.setActivo(true);
            usuarioRepository.save(admin);
            System.out.println("👑 ADMIN CREADO");
        }

        //CUANDO HAYA QUE ACTUALIZAR LA FOTO DE UN JUGADOR (EJEMPLO CON SEBAS)
        /*
        List<Jugador> sebasList = jugadorRepository.findByNombre("Sebas");
        if (!sebasList.isEmpty()) {
            Jugador sebas = sebasList.get(0);
            sebas.setUrlImagen("/sebastian.png");
            jugadorRepository.save(sebas);
        }
        */

        System.out.println(">>> ✅ CARGA DE DATOS COMPLETADA.");
    }
}