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

    public InicializadorDatos(JugadorRepository jugadorRepository, TemporadaRepository temporadaRepository,
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

        Temporada t2026;
        if (temporadaRepository.count() == 0) {
            t2026 = new Temporada(2026);
            temporadaRepository.save(t2026);
        } else {
            t2026 = temporadaRepository.findAll().get(0);
        }

        List<Jugador> lista = new ArrayList<>();

        //PORTEROS
        lista.add(new Jugador("Carlos", "PORTERO", 52, 6.8, "/images/carlos.png"));
        lista.add(new Jugador("Carmelo", "PORTERO", 54, 6.26, "/images/carmelo.png"));
        lista.add(new Jugador("Cristian", "PORTERO", 24, 7.26, "/images/cristian.png"));
        lista.add(new Jugador("Diego", "PORTERO", 49, 7.23, "/images/diego.png"));
        lista.add(new Jugador("Fran", "PORTERO", 53, 6.47, "/images/franportero.png"));
        lista.add(new Jugador("Jhona", "PORTERO", 48, 8.06, "/images/jhona.png"));
        lista.add(new Jugador("Juanlu", "PORTERO", 56, 5.98, "/images/juanlu.png"));
        lista.add(new Jugador("Sergio", "PORTERO", 34, 6.56, "/images/sergio.png"));

        //DEFENSAS
        lista.add(new Jugador("Alejandro", "DEFENSA", 32, 6.63, "/images/alejandro.png"));
        lista.add(new Jugador("Alejandro G.", "DEFENSA", 30, 6.58, "/images/alejandrogarrocho.png"));
        lista.add(new Jugador("Andrés", "DEFENSA", 52, 4.72, "/images/andres.png"));
        lista.add(new Jugador("Cardenas", "DEFENSA", 45, 6.20, "/images/cardenas.png"));
        lista.add(new Jugador("Chico", "DEFENSA", 46, 5.79, "/images/chico.png"));
        lista.add(new Jugador("Conce", "DEFENSA", 29, 5.84, "/images/conce.png"));
        lista.add(new Jugador("Cristian", "DEFENSA", 24, 7.08, "/images/cristian.png"));
        lista.add(new Jugador("David", "DEFENSA", 36, 6.19, "/images/david.png"));
        lista.add(new Jugador("Diego", "DEFENSA", 49, 8.07, "/images/diego.png"));
        lista.add(new Jugador("Javier", "DEFENSA", 58, 6.06, "/images/javier.png"));
        lista.add(new Jugador("Javier M.", "DEFENSA", 54, 5.88, "/images/javierm.png"));
        lista.add(new Jugador("Jesús Jr", "DEFENSA", 25, 6.62, "/images/jesusjr.png"));
        lista.add(new Jugador("Jhona", "DEFENSA", 48, 6.29, "/images/jhona.png"));
        lista.add(new Jugador("Jose", "DEFENSA", 45, 7.03, "/images/jose.png"));
        lista.add(new Jugador("Juan", "DEFENSA", 45, 6.04, "/images/juan.png"));
        lista.add(new Jugador("Juanlu", "DEFENSA", 56, 6.89, "/images/juanlu.png"));
        lista.add(new Jugador("Lucas", "DEFENSA", 55, 6.50, "/images/lucas.png"));
        lista.add(new Jugador("Luis", "DEFENSA", 57, 6.19, "/images/luis.png"));
        lista.add(new Jugador("Mario", "DEFENSA", 59, 7.01, "/images/mario.png"));
        lista.add(new Jugador("Pablo", "DEFENSA", 26, 6.63, "/images/pablo.png"));
        lista.add(new Jugador("Paco", "DEFENSA", 62, 7.11, "/images/paco.png"));
        lista.add(new Jugador("Primo", "DEFENSA", 46, 6.87, "/images/primo.png"));
        lista.add(new Jugador("Sebas", "DEFENSA", 33, 6.13, "/images/sebastian.png"));
        lista.add(new Jugador("Sergio", "DEFENSA", 34, 6.98, "/images/sergio.png"));

        //MEDIOS
        lista.add(new Jugador("Alberto", "MEDIO", 39, 8.06, "/images/alberto.png"));
        lista.add(new Jugador("Alejandro", "MEDIO", 32, 8.16, "/images/alejandro.png"));
        lista.add(new Jugador("Alejandro G.", "MEDIO", 30, 8.52, "/images/alejandrogarrocho.png"));
        lista.add(new Jugador("Cardenas", "MEDIO", 45, 6.8, "/images/cardenas.png"));
        lista.add(new Jugador("Chico", "MEDIO", 46, 5.29, "/images/chico.png"));
        lista.add(new Jugador("Conce", "MEDIO", 29, 7.13, "/images/conce.png"));
        lista.add(new Jugador("Cristian", "MEDIO", 24, 7.66, "/images/cristian.png"));
        lista.add(new Jugador("David", "MEDIO", 36, 7.19, "/images/david.png"));
        lista.add(new Jugador("Felipe", "MEDIO", 31, 7.69, "/images/felipe.png"));
        lista.add(new Jugador("Javier", "MEDIO", 58, 5.76, "/images/javier.png"));
        lista.add(new Jugador("Javier M.", "MEDIO", 54, 5.58, "/images/javierm.png"));
        lista.add(new Jugador("Jesús Jr", "MEDIO", 25, 7.53, "/images/jesusjr.png"));
        lista.add(new Jugador("Juan", "MEDIO", 45, 5.74, "/images/juan.png"));
        lista.add(new Jugador("Lucas", "MEDIO", 55, 5.85, "/images/lucas.png"));
        lista.add(new Jugador("Luis", "MEDIO", 57, 7.21, "/images/luis.png"));
        lista.add(new Jugador("Mario", "MEDIO", 59, 6.01, "/images/mario.png"));
        lista.add(new Jugador("Oswaldo", "MEDIO", 45, 8.19, "/images/oswi.png"));
        lista.add(new Jugador("Pablo", "MEDIO", 26, 8.16, "/images/pablo.png"));
        lista.add(new Jugador("Pepe", "MEDIO", 67, 6.97, "/images/pepe.png"));
        lista.add(new Jugador("Primo", "MEDIO", 46, 6.57, "/images/primo.png"));
        lista.add(new Jugador("Sebas", "MEDIO", 33, 8.13, "/images/sebastian.png"));
        lista.add(new Jugador("Sergio", "MEDIO", 34, 8.88, "/images/sergio.png"));

        //DELANTEROS
        lista.add(new Jugador("Alejandro", "DELANTERO", 32, 6.1, "/images/alejandro.png"));
        lista.add(new Jugador("Cristian", "DELANTERO", 24, 7.96, "/images/cristian.png"));
        lista.add(new Jugador("David", "DELANTERO", 36, 6.19, "/images/david.png"));
        lista.add(new Jugador("Felipe", "DELANTERO", 31, 7.79, "/images/felipe.png"));
        lista.add(new Jugador("Jesus", "DELANTERO", 42, 7.53, "/images/jesus.png"));
        lista.add(new Jugador("Jesús Jr", "DELANTERO", 25, 6.53, "/images/jesusjr.png"));
        lista.add(new Jugador("Jhona", "DELANTERO", 48, 6.16, "/images/jhona.png"));
        lista.add(new Jugador("Juan", "DELANTERO", 45, 5.97, "/images/juan.png"));
        lista.add(new Jugador("Juanlu", "DELANTERO", 56, 5.89, "/images/juanlu.png"));
        lista.add(new Jugador("Pepe", "DELANTERO", 67, 7.02, "/images/pepe.png"));
        lista.add(new Jugador("Sebas", "DELANTERO", 33, 7.13, "/images/sebastian.png"));
        lista.add(new Jugador("Sergio", "DELANTERO", 34, 6.88, "/images/sergio.png"));

        for (Jugador j : lista) {
            List<Jugador> existentes = jugadorRepository.findByNombreAndPosicion(j.getNombre(), j.getPosicion());

            if (existentes.isEmpty()) {
                // Si no existe, lo crea nuevo
                jugadorRepository.save(j);
            } else {
                Jugador jugadorExistente = existentes.get(0);
                jugadorExistente.setUrlImagen(j.getUrlImagen());
                jugadorRepository.save(jugadorExistente);
            }
        }

        if (jornadaRepository.count() == 0) {
            Jornada jornada1 = new Jornada(1, LocalDate.now(), t2026);
            jornadaRepository.save(jornada1);
        }

        if (usuarioRepository.findByNombre("Cristiangb02") == null) {
            Usuario admin = new Usuario("Cristiangb02", "Huelvamolamazo", 100_000_000, true);
            admin.setActivo(true);
            usuarioRepository.save(admin);
        }

        //CUANDO HAYA QUE ACTUALIZAR LA FOTO DE UN JUGADOR
        /*
        List<Jugador> listaJugadorExistente = jugadorRepository.findByNombre("nombre");
        for (Jugador c : listaJugadorExistente) {
            c.setUrlImagen("/nombre.png");
            jugadorRepository.save(c);
        }
          */
        System.out.println("CARGA DE DATOS COMPLETADA.");
    }
}