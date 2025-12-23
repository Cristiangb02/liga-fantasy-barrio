package com.fantasy.ligabarrio;

import jakarta.persistence.*;

@Entity
public class Actuacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Jugador jugador;

    @ManyToOne
    private Jornada jornada;
    private int puntosTotales;
    private boolean victoria;
    private boolean derrota;
    private boolean jugado; // ¿Jugó o no?
    private int autogoles;  
    private int golesMarcados;
    private int golesEncajados;

    public Actuacion() {
    }

    public Actuacion(Jugador jugador, Jornada jornada) {
        this.jugador = jugador;
        this.jornada = jornada;
        this.puntosTotales = 0;
        this.jugado = true; // Por defecto asumimos que juega si se crea, luego se puede cambiar
    }

    // Getters y Setters
    public Long getId() { return id; }
    public Jugador getJugador() { return jugador; }
    public void setJugador(Jugador jugador) { this.jugador = jugador; }
    public Jornada getJornada() { return jornada; }
    public void setJornada(Jornada jornada) { this.jornada = jornada; }
    public int getPuntosTotales() { return puntosTotales; }
    public void setPuntosTotales(int puntosTotales) { this.puntosTotales = puntosTotales; }
    
    public boolean isVictoria() { return victoria; }
    public void setVictoria(boolean victoria) { this.victoria = victoria; }
    public boolean isDerrota() { return derrota; }
    public void setDerrota(boolean derrota) { this.derrota = derrota; }
    
    public int getGolesMarcados() { return golesMarcados; }
    public void setGolesMarcados(int golesMarcados) { this.golesMarcados = golesMarcados; }
    public int getGolesEncajados() { return golesEncajados; }
    public void setGolesEncajados(int golesEncajados) { this.golesEncajados = golesEncajados; }

    public boolean isJugado() { return jugado; }
    public void setJugado(boolean jugado) { this.jugado = jugado; }
    
    public int getAutogoles() { return autogoles; }
    public void setAutogoles(int autogoles) { this.autogoles = autogoles; }
}

