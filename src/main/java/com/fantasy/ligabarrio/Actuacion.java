package com.fantasy.ligabarrio;

import jakarta.persistence.*;

@Entity
public class Actuacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Jugador jugador; // ¿Quién?

    @ManyToOne
    private Jornada jornada; // ¿Cuándo?

    // --- DATOS REALES DEL PARTIDO (Tu Excel) ---
    private boolean jugado;      // +1 punto
    private boolean victoria;    // +2 puntos
    private boolean derrota;     // -1 punto
    // (El empate no puntúa extra, así que si victoria y derrota son false, es empate)

    private int golesMarcados;
    private int golesEncajados;  // Clave para Porteros/Defensas
    private int autogoles;       // Resta puntos

    // --- RESULTADO FINAL ---
    private int puntosTotales;   // Aquí guardaremos el cálculo final

    public Actuacion() {
    }

    // Constructor útil
    public Actuacion(Jugador jugador, Jornada jornada) {
        this.jugador = jugador;
        this.jornada = jornada;
        this.jugado = true; // Por defecto asumimos que si creamos esto, jugó
    }

    // Getters y Setters
    public Long getId() { return id; }
    public Jugador getJugador() { return jugador; }
    public void setJugador(Jugador jugador) { this.jugador = jugador; }
    public Jornada getJornada() { return jornada; }
    public void setJornada(Jornada jornada) { this.jornada = jornada; }

    public boolean isVictoria() { return victoria; }
    public void setVictoria(boolean victoria) { this.victoria = victoria; }
    public boolean isDerrota() { return derrota; }
    public void setDerrota(boolean derrota) { this.derrota = derrota; }

    public int getGolesMarcados() { return golesMarcados; }
    public void setGolesMarcados(int golesMarcados) { this.golesMarcados = golesMarcados; }
    public int getGolesEncajados() { return golesEncajados; }
    public void setGolesEncajados(int golesEncajados) { this.golesEncajados = golesEncajados; }
    public int getAutogoles() { return autogoles; }
    public void setAutogoles(int autogoles) { this.autogoles = autogoles; }

    public int getPuntosTotales() { return puntosTotales; }
    public void setPuntosTotales(int puntosTotales) { this.puntosTotales = puntosTotales; }
}