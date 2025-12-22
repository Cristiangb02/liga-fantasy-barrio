package com.fantasy.ligabarrio;

import jakarta.persistence.*;

@Entity
public class Jugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String posicion; // PORTERO, DEFENSA, MEDIO, DELANTERO
    private int valor;
    private int clausula;
    private int puntosAcumulados;
    private String urlImagen;

    @ManyToOne
    private Usuario propietario;
    
    // 🔴 NUEVO CAMPO: Guardamos el ID de la jornada en la que se fichó
    private Long jornadaFichaje;

    public Jugador() {
    }

    public Jugador(String nombre, String posicion, int valor, String urlImagen) {
        this.nombre = nombre;
        this.posicion = posicion;
        this.valor = valor;
        this.clausula = valor; 
        this.puntosAcumulados = 0;
        this.urlImagen = urlImagen;
        this.jornadaFichaje = 0L; // Inicializamos a 0
    }

    // Getters y Setters básicos
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getPosicion() { return posicion; }
    public void setPosicion(String posicion) { this.posicion = posicion; }
    public int getValor() { return valor; }
    public void setValor(int valor) { this.valor = valor; }
    public int getClausula() { return clausula; }
    public void setClausula(int clausula) { this.clausula = clausula; }
    public int getPuntosAcumulados() { return puntosAcumulados; }
    public void setPuntosAcumulados(int puntosAcumulados) { this.puntosAcumulados = puntosAcumulados; }
    public String getUrlImagen() { return urlImagen; }
    public void setUrlImagen(String urlImagen) { this.urlImagen = urlImagen; }
    public Usuario getPropietario() { return propietario; }
    public void setPropietario(Usuario propietario) { this.propietario = propietario; }

    // 🔴 Getters y Setters NUEVO CAMPO
    public Long getJornadaFichaje() { return jornadaFichaje; }
    public void setJornadaFichaje(Long jornadaFichaje) { this.jornadaFichaje = jornadaFichaje; }
}
