package com.fantasy.ligabarrio;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
public class Noticia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mensaje;
    private LocalDateTime fecha;

    public Noticia() {}

    public Noticia(String mensaje) {
        this.mensaje = mensaje;
        this.fecha = LocalDateTime.now();
    }

    // Getters
    public String getMensaje() { return mensaje; }
    public String getFechaBonita() {
        return fecha.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
    }
}