package com.fantasy.ligabarrio;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Temporada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private int anio;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private boolean activa;

    public Temporada() {
    }

    public Temporada(int anio) {
        this.anio = anio;
        this.nombre = "Temporada " + anio;
        this.fechaInicio = LocalDate.of(anio, 1, 1);
        this.fechaFin = LocalDate.of(anio, 12, 31);
        this.activa = true;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public int getAnio() { return anio; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
}