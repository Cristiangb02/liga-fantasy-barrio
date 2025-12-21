package com.fantasy.ligabarrio;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;

@Component
public class InicializadorDatos implements CommandLineRunner {

    private final JugadorRepository jugadorRepository;
    private final TemporadaRepository temporadaRepository;
    private final JornadaRepository jornadaRepository;
    private final ActuacionRepository actuacionRepository;
    private final CalculadoraPuntosService calculadora;
    private final UsuarioRepository usuarioRepository;
    private final EquipoRepository equipoRepository;

    public InicializadorDatos(JugadorRepository jugadorRepository, TemporadaRepository temporadaRepository, JornadaRepository jornadaRepository, ActuacionRepository actuacionRepository, CalculadoraPuntosService calculadora, UsuarioRepository usuarioRepository, EquipoRepository equipoRepository) {
        this.jugadorRepository = jugadorRepository;
        this.temporadaRepository = temporadaRepository;
        this.jornadaRepository = jornadaRepository;
        this.actuacionRepository = actuacionRepository;
        this.calculadora = calculadora;
        this.usuarioRepository = usuarioRepository;
        this.equipoRepository = equipoRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        // 1. TEMPORADA
        Temporada t2025;
        if (temporadaRepository.count() == 0) {
            t2025 = new Temporada(2025);
            temporadaRepository.save(t2025);
        } else {
            t2025 = temporadaRepository.findAll().get(0);
        }

        // 2. JUGADORES (MÁS JUGADORES AÑADIDOS
        if (jugadorRepository.count() == 0) {
            jugadorRepository.saveAll(List.of(
                    new Jugador("Cristian", Posicion.DEFENSA, 60_000_000, "/cristian.png"),
                    new Jugador("Pepe", Posicion.DELANTERO, 9_450_000, "/pepe.png"),
                    new Jugador("Cárdenas", Posicion.MEDIO, 14_500_000, "/cardenas.png"),
                    new Jugador("Sergio", Posicion.MEDIO, 86_210_000, "/sergio.png"),
                    new Jugador("Jose", Posicion.DEFENSA, 45_000_000, "/jose.png"),
                    new Jugador("Luis", Posicion.MEDIO, 21_550_000, "/luis.png"),
                    new Jugador("Juanlu", Posicion.DEFENSA, 17_000_000, "/juanlu.png"),
                    new Jugador("Jhona", Posicion.PORTERO, 10_000_000, "/jhona.png"),
                    new Jugador("Carlos", Posicion.PORTERO, 950_000, "/carlos.png"),
                    new Jugador("Diego", Posicion.DEFENSA, 67_450_000, "/diego.png"),
                    new Jugador("Andrés", Posicion.DEFENSA, 1_000_000, "/andres.png"),
                    new Jugador("Felipe", Posicion.MEDIO, 92_000_000, "/felipe.png"),
                    new Jugador("Cristian", Posicion.DELANTERO, 32_100_000, "/cristian.png"),
                    new Jugador("Fran", Posicion.PORTERO, 12_500_000, "/franportero.png")
            ));
        }

        Jornada jornada1;
        if (jornadaRepository.count() == 0) {
            jornada1 = new Jornada(1, LocalDate.now(), t2025);
            jornadaRepository.save(jornada1);
        } else {
            jornada1 = jornadaRepository.findAll().get(0);
        }

        // 4. CREAR USUARIO ADMIN
        if (usuarioRepository.count() == 0) {
            // Creamos al Admin. IMPORTANTE: el 'true' al final.
            Usuario admin = new Usuario("Cristian", "Huelvamolamazo", 100_000_000, true);
            usuarioRepository.save(admin);

            // Asignamos 2 jugadores a Cristian para empezar (opcional)
            List<Jugador> jugadores = jugadorRepository.findAll();
            if(jugadores.size() >= 2) {
                Jugador j1 = jugadores.get(0);
                Jugador j2 = jugadores.get(1);

                j1.setPropietario(admin);
                j2.setPropietario(admin);
                jugadorRepository.save(j1);
                jugadorRepository.save(j2);

                // Equipo inicial
                Equipo equipoAdmin = new Equipo(admin, jornada1);
                equipoAdmin.alinearJugador(j1);
                equipoAdmin.alinearJugador(j2);
                equipoRepository.save(equipoAdmin);
            }
            System.out.println(">>> Inicialización completada. Usuario 'Cristian' es ADMIN.");
        }
    }

}

