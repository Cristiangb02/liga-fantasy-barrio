package com.fantasy.ligabarrio;

import jakarta.persistence.*; // Importa las herramientas de base de datos
import java.time.LocalDate;

@Entity // ESTO ES MAGIA: Le dice a Spring "Crea una tabla llamada Temporada"
public class Temporada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Identificador único (1, 2, 3...)

    private String nombre; // Ej: "Temporada 2025"
    private int anio;      // 2025

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    private boolean activa; // Para saber si es la que estamos jugando ahora

    // Constructor vacío (Obligatorio para Spring)
    public Temporada() {
    }

    // Constructor para crearla nosotros fácil
    public Temporada(int anio) {
        this.anio = anio;
        this.nombre = "Temporada " + anio;
        this.fechaInicio = LocalDate.of(anio, 1, 1);
        this.fechaFin = LocalDate.of(anio, 12, 31);
        this.activa = true;
    }

    // Getters y Setters (Necesarios para que todo funcione)
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public int getAnio() { return anio; }
    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
}