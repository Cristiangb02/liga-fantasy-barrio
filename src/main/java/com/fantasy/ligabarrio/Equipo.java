package com.fantasy.ligabarrio;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Usuario usuario; // ¿De quién es este equipo?

    @ManyToOne
    private Jornada jornada; // ¿Para qué jornada es?

    // IMPORTANTE: Muchos equipos pueden tener al mismo jugador (ManyToMany)
    @ManyToMany
    private List<Jugador> jugadoresAlineados = new ArrayList<>();

    private int puntosTotalesJornada; // La suma de los puntos de sus jugadores

    public Equipo() {
    }

    public Equipo(Usuario usuario, Jornada jornada) {
        this.usuario = usuario;
        this.jornada = jornada;
    }

    // Método útil para añadir jugadores fácilmente
    public void alinearJugador(Jugador j) {
        this.jugadoresAlineados.add(j);
    }

    // Getters y Setters
    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Jornada getJornada() { return jornada; }
    public void setJornada(Jornada jornada) { this.jornada = jornada; }
    public List<Jugador> getJugadoresAlineados() { return jugadoresAlineados; }
    public void setJugadoresAlineados(List<Jugador> jugadoresAlineados) { this.jugadoresAlineados = jugadoresAlineados; }
    public int getPuntosTotalesJornada() { return puntosTotalesJornada; }
    public void setPuntosTotalesJornada(int puntosTotalesJornada) { this.puntosTotalesJornada = puntosTotalesJornada; }
}