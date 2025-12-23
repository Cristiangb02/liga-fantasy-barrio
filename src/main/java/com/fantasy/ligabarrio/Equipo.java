package com.fantasy.ligabarrio;

import jakarta.persistence.*;

@Entity
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Usuario usuario;

    @ManyToOne
    private Jornada jornada;

    @ManyToMany
    private java.util.List<Jugador> jugadoresAlineados;
    private int puntosTotalesJornada;    
    private boolean reclamado;

    public Equipo() {
    }

    public Equipo(Usuario usuario, Jornada jornada) {
        this.usuario = usuario;
        this.jornada = jornada;
        this.reclamado = false; 
    }

    public void alinearJugador(Jugador jugador) {
        if (this.jugadoresAlineados == null) {
            this.jugadoresAlineados = new java.util.ArrayList<>();
        }
        this.jugadoresAlineados.add(jugador);
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Jornada getJornada() { return jornada; }
    public void setJornada(Jornada jornada) { this.jornada = jornada; }
    public java.util.List<Jugador> getJugadoresAlineados() { return jugadoresAlineados; }
    public void setJugadoresAlineados(java.util.List<Jugador> jugadoresAlineados) { this.jugadoresAlineados = jugadoresAlineados; }
    public int getPuntosTotalesJornada() { return puntosTotalesJornada; }
    public void setPuntosTotalesJornada(int puntosTotalesJornada) { this.puntosTotalesJornada = puntosTotalesJornada; }
    
    public boolean isReclamado() { return reclamado; }
    public void setReclamado(boolean reclamado) { this.reclamado = reclamado; }
}

