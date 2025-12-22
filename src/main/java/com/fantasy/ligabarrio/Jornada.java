package com.fantasy.ligabarrio;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Jornada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int numero;
    private LocalDate fecha;

    @ManyToOne
    private Temporada temporada;

    public Jornada() {
        this.numero = 1; // 🔴 SEGURIDAD: Por defecto siempre será la 1, nunca la 0
    }

    public Jornada(int numero, LocalDate fecha, Temporada temporada) {
        this.numero = (numero <= 0) ? 1 : numero; // 🔴 SEGURIDAD: Si intentan poner 0, ponemos 1
        this.fecha = fecha;
        this.temporada = temporada;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; } // 🔴 ESTE METODO ES VITAL
    
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    
    public Temporada getTemporada() { return temporada; }
    public void setTemporada(Temporada temporada) { this.temporada = temporada; }
}
