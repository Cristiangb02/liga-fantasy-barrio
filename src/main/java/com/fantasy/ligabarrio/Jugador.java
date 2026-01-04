package com.fantasy.ligabarrio;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime; // IMPORTANTE

@Entity
public class Jugador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String posicion;
    private int valor;
    private int clausula;
    private double mediaPuntos;
    private String urlImagen;
    private int puntosAcumulados;

    // Control de mercado
    private Long jornadaFichaje;
    private LocalDate fechaFichaje;

    // 🛡️ NUEVO: Fecha hasta la que está blindado
    private LocalDateTime fechaFinBlindaje;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario propietario;

    public Jugador() {}

    public Jugador(String nombre, String posicion, int valor, double mediaPuntos, String urlImagen) {
        this.nombre = nombre;
        this.posicion = posicion;
        this.valor = valor;
        this.clausula = valor; // Al inicio clausula = valor
        this.mediaPuntos = mediaPuntos;
        this.urlImagen = urlImagen;
        this.puntosAcumulados = 0;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getPosicion() { return posicion; }
    public void setPosicion(String posicion) { this.posicion = posicion; }
    public int getValor() { return valor; }
    public void setValor(int valor) { this.valor = valor; }
    public int getClausula() { return clausula; }
    public void setClausula(int clausula) { this.clausula = clausula; }
    public double getMediaPuntos() { return mediaPuntos; }
    public void setMediaPuntos(double mediaPuntos) { this.mediaPuntos = mediaPuntos; }
    public String getUrlImagen() { return urlImagen; }
    public void setUrlImagen(String urlImagen) { this.urlImagen = urlImagen; }
    public int getPuntosAcumulados() { return puntosAcumulados; }
    public void setPuntosAcumulados(int puntosAcumulados) { this.puntosAcumulados = puntosAcumulados; }
    public Usuario getPropietario() { return propietario; }
    public void setPropietario(Usuario propietario) { this.propietario = propietario; }
    public Long getJornadaFichaje() { return jornadaFichaje; }
    public void setJornadaFichaje(Long jornadaFichaje) { this.jornadaFichaje = jornadaFichaje; }
    public LocalDate getFechaFichaje() { return fechaFichaje; }
    public void setFechaFichaje(LocalDate fechaFichaje) { this.fechaFichaje = fechaFichaje; }

    // Getter/Setter Blindaje
    public LocalDateTime getFechaFinBlindaje() { return fechaFinBlindaje; }
    public void setFechaFinBlindaje(LocalDateTime fechaFinBlindaje) { this.fechaFinBlindaje = fechaFinBlindaje; }
}