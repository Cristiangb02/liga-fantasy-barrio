package com.fantasy.ligabarrio;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Jornada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int numero; // Jornada 1, Jornada 2...
    private LocalDate fecha;

    @ManyToOne // Muchas jornadas pertenecen a una sola temporada
    private Temporada temporada;

    public Jornada() {
    }

    public Jornada(int numero, LocalDate fecha, Temporada temporada) {
        this.numero = numero;
        this.fecha = fecha;
        this.temporada = temporada;
    }

    // Getters y Setters básicos
    public Long getId() { return id; }
    public int getNumero() { return numero; }
    public LocalDate getFecha() { return fecha; }
    public Temporada getTemporada() { return temporada; }
}