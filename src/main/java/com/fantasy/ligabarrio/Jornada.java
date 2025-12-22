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
    }

    public Jornada(int numero, LocalDate fecha, Temporada temporada) {
        this.numero = numero;
        this.fecha = fecha;
        this.temporada = temporada;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; } // 🔴 ESTE ES EL QUE FALTABA
    
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    
    public Temporada getTemporada() { return temporada; }
    public void setTemporada(Temporada temporada) { this.temporada = temporada; }
}
