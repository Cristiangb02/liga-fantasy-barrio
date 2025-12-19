package com.fantasy.ligabarrio;

import jakarta.persistence.*;

@Entity
public class Jugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    @Enumerated(EnumType.STRING)
    private Posicion posicion;
    private int valor;
    private int clausula; // <--- NUEVO: PRECIO PARA ROBARLO
    private String urlImagen;
    private int puntosAcumulados = 0;

    @ManyToOne
    private Usuario propietario;

    public Jugador() {}

    public Jugador(String nombre, Posicion posicion, int valor, String urlImagen) {
        this.nombre = nombre;
        this.posicion = posicion;
        this.valor = valor;
        // La cláusula inicial será el doble del valor (por ejemplo)
        this.clausula = valor * 2;
        this.urlImagen = urlImagen;
        this.puntosAcumulados = 0;
    }

    // GETTERS Y SETTERS
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Posicion getPosicion() { return posicion; }
    public void setPosicion(Posicion posicion) { this.posicion = posicion; }
    public int getValor() { return valor; }
    public void setValor(int valor) { this.valor = valor; }

    // Getter/Setter Clausula
    public int getClausula() { return clausula; }
    public void setClausula(int clausula) { this.clausula = clausula; }

    public String getUrlImagen() { return urlImagen; }
    public void setUrlImagen(String urlImagen) { this.urlImagen = urlImagen; }
    public int getPuntosAcumulados() { return puntosAcumulados; }
    public void setPuntosAcumulados(int puntosAcumulados) { this.puntosAcumulados = puntosAcumulados; }
    public Usuario getPropietario() { return propietario; }
    public void setPropietario(Usuario propietario) { this.propietario = propietario; }
}