package com.fantasy.ligabarrio;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Jugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String posicion;
    private int edad;
    private double media;
    private int valor;
    private String imagen;

    @ManyToOne
    private Usuario propietario;
    private int puntosAcumulados;
    private int clausula;
    private Long jornadaFichaje;
    private LocalDate fechaFichaje;
    private LocalDateTime fechaFinBlindaje;
    private LocalDate fechaVenta;

    public Jugador() {
    }

    public Jugador(String nombre, String posicion, int edad, double media, String imagen) {
        this.nombre = nombre;
        this.posicion = posicion;
        this.edad = edad;
        this.media = media;
        this.imagen = imagen;
        this.puntosAcumulados = 0;
        this.jornadaFichaje = 0L;
        this.fechaFinBlindaje = null;
        this.valor = calcularPrecioFinal(edad, media);
        this.clausula = this.valor;
    }

    private int calcularPrecioFinal(int edad, double media) {
        //1. Base exponencial (Potencia 2.88)
        double precioBase = 10_000 * Math.pow(2.88, media);

        //2. Factor edad
        double factorEdad = 1.0;

        if (edad < 27) {
            //JUVENTUD: +16% por año
            factorEdad += (27 - edad) * 0.16;
        } else if (edad <= 40) {
            //MADUREZ: -1.2% suave por cada año.
            factorEdad -= (edad - 27) * 0.012;
        } else {
            //VEJEZ:
            double bajadaSuave = (40 - 27) * 0.02;
            double bajadaFuerte = (edad - 40) * 0.04;
            factorEdad -= (bajadaSuave + bajadaFuerte);
        }

        //Suelo para veteranos con alta media
        if (factorEdad < 0.25) factorEdad = 0.24;

        //3. Cálculo final
        double precioFinal = precioBase * factorEdad;
        if (precioFinal < 250_000) precioFinal = 250_000;

        return (int) precioFinal;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getPosicion() { return posicion; }
    public void setPosicion(String posicion) { this.posicion = posicion; }
    public int getEdad() { return edad; }
    public void setEdad(int edad) { this.edad = edad; }
    public double getMedia() { return media; }
    public void setMedia(double media) { this.media = media; }
    public int getValor() { return valor; }
    public void setValor(int valor) { this.valor = valor; }
    public String getUrlImagen() { return imagen; }
    public void setUrlImagen(String imagen) { this.imagen = imagen; }
    public Usuario getPropietario() { return propietario; }
    public void setPropietario(Usuario propietario) { this.propietario = propietario; }
    public int getPuntosAcumulados() { return puntosAcumulados; }
    public void setPuntosAcumulados(int puntosAcumulados) { this.puntosAcumulados = puntosAcumulados; }
    public int getClausula() { return clausula; }
    public void setClausula(int clausula) { this.clausula = clausula; }
    public Long getJornadaFichaje() { return jornadaFichaje; }
    public void setJornadaFichaje(Long jornadaFichaje) { this.jornadaFichaje = jornadaFichaje; }
    public LocalDate getFechaFichaje() { return fechaFichaje; }
    public void setFechaFichaje(LocalDate fechaFichaje) { this.fechaFichaje = fechaFichaje; }
    public LocalDateTime getFechaFinBlindaje() { return fechaFinBlindaje;}
    public void setFechaFinBlindaje(LocalDateTime fechaFinBlindaje) { this.fechaFinBlindaje = fechaFinBlindaje;}
    public LocalDate getFechaVenta() { return fechaVenta; }
    public void setFechaVenta(LocalDate fechaVenta) { this.fechaVenta = fechaVenta; }
}