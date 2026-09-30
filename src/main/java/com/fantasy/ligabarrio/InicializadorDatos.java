package com.fantasy.ligabarrio;

import com.fantasy.ligabarrio.model.Jornada;
import com.fantasy.ligabarrio.model.Jugador;
import com.fantasy.ligabarrio.model.Temporada;
import com.fantasy.ligabarrio.model.Usuario;
import com.fantasy.ligabarrio.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

@Component
public class InicializadorDatos implements CommandLineRunner {

    private final JugadorRepository juR;
    private final TemporadaRepository tR;
    private final JornadaRepository joR;
    private final UsuarioRepository uR;
    private final EquipoRepository eR;
    private final ActuacionRepository aR;
    private final NoticiaRepository nR;

    @Value("${admin.usuario}")
    private String adminUser;

    @Value("${admin.password}")
    private String adminPass;

    public InicializadorDatos(JugadorRepository juR, TemporadaRepository tR, JornadaRepository joR, UsuarioRepository uR,
                              EquipoRepository eR, ActuacionRepository aR, NoticiaRepository nR) {
        this.juR = juR;
        this.tR = tR;
        this.joR = joR;
        this.uR = uR;
        this.eR = eR;
        this.aR = aR;
        this.nR = nR;
    }

    @Override
    public void run(String... args) throws Exception {

        Temporada t2026;
        if (tR.count() == 0) {
            t2026 = new Temporada(2026);
            tR.save(t2026);
        } else {
            t2026 = tR.findAll().get(0);
        }

        List<Jugador> lista = new ArrayList<>();

        //PORTEROS
        lista.add(new Jugador("Carlos", "PORTERO", 52, 6.8, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791134/carlos.png"));
        lista.add(new Jugador("Carmelo", "PORTERO", 54, 6.26, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791134/carmelo.png"));
        lista.add(new Jugador("Cristian", "PORTERO", 24, 7.26, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791146/cristianportero.png"));
        lista.add(new Jugador("Diego", "PORTERO", 49, 7.23, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791135/diegop.png"));
        lista.add(new Jugador("Fran", "PORTERO", 53, 6.47, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791146/franportero.png"));
        lista.add(new Jugador("Jhona", "PORTERO", 48, 8.06, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791138/jhonap.jpg"));
        lista.add(new Jugador("Juanlu", "PORTERO", 56, 5.98, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791139/juanlup.png"));
        lista.add(new Jugador("Sergio", "PORTERO", 34, 6.56, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791145/sergiop.png"));
        lista.add(new Jugador("Sebas", "PORTERO", 30, 8, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791143/sebasp.png"));

        //DEFENSAS
        lista.add(new Jugador("Alejandro", "DEFENSA", 32, 6.63, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791134/alejandro.png"));
        lista.add(new Jugador("Alejandro G.", "DEFENSA", 30, 6.58, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791134/alejandrogarrocho.png"));
        lista.add(new Jugador("Álvaro", "DEFENSA", 33, 6.08, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791145/alvaro.png"));
        lista.add(new Jugador("Andrés", "DEFENSA", 52, 4.72, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791134/andres.png"));
        lista.add(new Jugador("Cardenas", "DEFENSA", 45, 6.20, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791133/cardenas.png"));
        lista.add(new Jugador("Chico", "DEFENSA", 46, 5.79, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791134/chico.png"));
        lista.add(new Jugador("Conce", "DEFENSA", 29, 5.84, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791146/conce.png"));
        lista.add(new Jugador("Cristian", "DEFENSA", 24, 7.08, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791134/cristian.png"));
        lista.add(new Jugador("David", "DEFENSA", 36, 6.19, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791134/david.png"));
        lista.add(new Jugador("Diego", "DEFENSA", 49, 8.07, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791135/diego.png"));
        lista.add(new Jugador("Javier", "DEFENSA", 58, 6.06, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791135/javier.png"));
        lista.add(new Jugador("Javier M.", "DEFENSA", 54, 5.88, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791135/javierm.png"));
        lista.add(new Jugador("Jesús Jr", "DEFENSA", 25, 6.62, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791136/jesusjr.png"));
        lista.add(new Jugador("Jhona", "DEFENSA", 48, 6.29, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791138/jhona.jpg"));
        lista.add(new Jugador("Jose", "DEFENSA", 45, 7.03, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791138/jose.png"));
        lista.add(new Jugador("Juan", "DEFENSA", 45, 6.04, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791139/juan.jpg"));
        lista.add(new Jugador("Juanlu", "DEFENSA", 56, 6.89, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791139/juanlu.png"));
        lista.add(new Jugador("Lucas", "DEFENSA", 55, 6.50, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791140/lucas.png"));
        lista.add(new Jugador("Luis", "DEFENSA", 57, 6.19, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791140/luis.png"));
        lista.add(new Jugador("Mario", "DEFENSA", 59, 7.01, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791141/mario.png"));
        lista.add(new Jugador("Pablo", "DEFENSA", 26, 6.63, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791819/user.png"));
        lista.add(new Jugador("Pablo M.", "DEFENSA", 21, 8, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791142/pabloM.png"));
        lista.add(new Jugador("Paco", "DEFENSA", 62, 7.11, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791143/paco.png"));
        lista.add(new Jugador("Primo", "DEFENSA", 46, 6.87, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791143/primo.png"));
        lista.add(new Jugador("Sebas", "DEFENSA", 33, 6.13, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791144/sebastian.png"));
        lista.add(new Jugador("Sergio", "DEFENSA", 34, 6.98, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791145/sergio.png"));

        //MEDIOS
        lista.add(new Jugador("Alberto", "MEDIO", 39, 8.06, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791133/alberto.png"));
        lista.add(new Jugador("Alejandro", "MEDIO", 32, 8.16, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791134/alejandro.png"));
        lista.add(new Jugador("Alejandro G.", "MEDIO", 30, 8.52, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791134/alejandrogarrocho.png"));
        lista.add(new Jugador("Álvaro", "MEDIO", 33, 7.52, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791145/alvaro.png"));
        lista.add(new Jugador("Álvaro O.", "MEDIO", 25, 7.5, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791145/alvaroO.png"));
        lista.add(new Jugador("Cardenas", "MEDIO", 45, 6.8, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791133/cardenas.png"));
        lista.add(new Jugador("Chico", "MEDIO", 46, 5.29, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791134/chico.png"));
        lista.add(new Jugador("Conce", "MEDIO", 29, 7.13, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791146/conce.png"));
        lista.add(new Jugador("Cristian", "MEDIO", 24, 7.66, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791134/cristian.png"));
        lista.add(new Jugador("David", "MEDIO", 36, 7.19, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791134/david.png"));
        lista.add(new Jugador("David amigo Felipe", "MEDIO", 32, 7.6, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791819/user.png"));
        lista.add(new Jugador("Felipe", "MEDIO", 31, 7.69, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791135/felipe.png"));
        lista.add(new Jugador("Javier", "MEDIO", 58, 5.76, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791135/javier.png"));
        lista.add(new Jugador("Javier V", "MEDIO", 35, 8.2, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791135/javierv.png"));
        lista.add(new Jugador("Javier M.", "MEDIO", 54, 5.58, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791135/javierm.png"));
        lista.add(new Jugador("Jesús Jr", "MEDIO", 25, 7.53, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791136/jesusjr.png"));
        lista.add(new Jugador("Juan", "MEDIO", 45, 5.74, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791139/juan.jpg"));
        lista.add(new Jugador("Lucas", "MEDIO", 55, 5.85, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791140/lucas.png"));
        lista.add(new Jugador("Luis", "MEDIO", 57, 7.21, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791140/luis.png"));
        lista.add(new Jugador("Mario", "MEDIO", 59, 6.01, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791141/mario.png"));
        lista.add(new Jugador("Oswaldo", "MEDIO", 45, 8.19, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791142/oswi.jpg"));
        lista.add(new Jugador("Pablo", "MEDIO", 26, 8.16, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791819/user.png"));
        lista.add(new Jugador("Pablo M.", "MEDIO", 21, 8, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791142/pabloM.png"));
        lista.add(new Jugador("Pepe", "MEDIO", 67, 6.97, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791143/pepe.jpg"));
        lista.add(new Jugador("Primo", "MEDIO", 46, 6.57, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791143/primo.png"));
        lista.add(new Jugador("Sebas", "MEDIO", 33, 8.13, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791144/sebastian.png"));
        lista.add(new Jugador("Sebas D.", "MEDIO", 31, 7.54, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791819/user.png"));
        lista.add(new Jugador("Sergio", "MEDIO", 34, 8.88, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791145/sergio.png"));

        //DELANTEROS
        lista.add(new Jugador("Alejandro", "DELANTERO", 32, 6.1, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791134/alejandro.png"));
        lista.add(new Jugador("Álvaro O.", "DELANTERO", 25, 7, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791145/alvaroO.png"));
        lista.add(new Jugador("Cristian", "DELANTERO", 24, 7.96, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791134/cristian.png"));
        lista.add(new Jugador("David", "DELANTERO", 36, 6.19, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791134/david.png"));
        lista.add(new Jugador("Felipe", "DELANTERO", 31, 7.79, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791135/felipe.png"));
        lista.add(new Jugador("Jesus", "DELANTERO", 42, 7.53, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791135/jesus.png"));
        lista.add(new Jugador("Jesús Jr", "DELANTERO", 25, 6.53, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791136/jesusjr.png"));
        lista.add(new Jugador("Jhona", "DELANTERO", 48, 6.16, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791138/jhona.jpg"));
        lista.add(new Jugador("Juan", "DELANTERO", 45, 5.97, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791139/juan.jpg"));
        lista.add(new Jugador("Juanlu", "DELANTERO", 56, 5.89, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791139/juanlu.png"));
        lista.add(new Jugador("Pablo M.", "DELANTERO", 21, 8, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791142/pabloM.png"));
        lista.add(new Jugador("Pepe", "DELANTERO", 67, 7.02, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791143/pepe.jpg"));
        lista.add(new Jugador("Sebas", "DELANTERO", 33, 7.13, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791144/sebastian.png"));
        lista.add(new Jugador("Sergio", "DELANTERO", 34, 6.88, "https://res.cloudinary.com/w1zhmtmj/image/upload/v1790791145/sergio.png"));

        for (Jugador j : lista) {
            List<Jugador> existentes = juR.findByNombreAndPosicion(j.getNombre(), j.getPosicion());

            if (existentes.isEmpty()) {
                juR.save(j);
            }
        }

        if (joR.count() == 0) {
            Jornada jornada1 = new Jornada(1, LocalDate.now(), t2026);
            joR.save(jornada1);
        }

        if (uR.findByNombre(adminUser) == null) {
            Usuario admin = new Usuario(adminUser, adminPass, 100_000_000, true);
            admin.setActivo(true);
            uR.save(admin);
        }
    }
}