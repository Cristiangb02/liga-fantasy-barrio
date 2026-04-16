package com.fantasy.ligabarrio.model;

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

    public Long getId() { return id; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getFechaBonita() {
        String resultado = null;
        if (fecha == null) {
            resultado = "";
        }
        DateTimeFormatter formateador = DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm");
        resultado = fecha.format(formateador);
        return resultado;
    }
}
