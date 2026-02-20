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
    private LocalDate diaBloqueo;

    @ManyToOne
    private Temporada temporada;

    @Column(columnDefinition = "boolean default false")
    private boolean bloqueada = false;

    public Jornada() {
        this.numero = 1;
        this.bloqueada = false;
    }

    public Jornada(int numero, LocalDate fecha, Temporada temporada) {
        if (numero <= 0) {
            this.numero = 1;
        } else {
            this.numero = numero;
        }
        this.fecha = fecha;
        this.temporada = temporada;
        this.bloqueada = false;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public Temporada getTemporada() { return temporada; }
    public void setTemporada(Temporada temporada) { this.temporada = temporada; }
    public boolean isBloqueada() { return bloqueada; }
    public void setBloqueada(boolean bloqueada) { this.bloqueada = bloqueada; }
    public LocalDate getDiaBloqueo() { return diaBloqueo;}
    public void setDiaBloqueo(LocalDate diaBloqueo) { this.diaBloqueo = diaBloqueo;}
}