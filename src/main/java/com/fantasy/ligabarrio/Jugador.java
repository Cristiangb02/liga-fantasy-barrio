package com.fantasy.ligabarrio;

import jakarta.persistence.*;

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

        //CÁLCULO DE PRECIO
        this.valor = calcularPrecioFinal(edad, media);
        this.clausula = this.valor;
    }

    private int calcularPrecioFinal(int edad, double media) {
        // 1. BASE EXPONENCIAL (Potencia 2.92)
        //Mantenemos la potencia alta para que los cracks destaquen.
        double precioBase = 10_000 * Math.pow(2.92, media);

        //2. FACTOR EDAD
        double factorEdad = 1.0;

        if (edad< 27) {
            // JUVENTUD (O EDAD 0): +16% por año
            factorEdad += (27 - edad) * 0.16;
        } else if (edad <= 40) {
            //MADUREZ: -15% suave por año.
            factorEdad -= (edad - 27) * 0.015;
        } else {
            //DECLIVE SUAVIZADO:
            //Bajada previa hasta los 40 (-12% total)
            double bajadaSuave = (40 - 27) * 0.02;
            //AHORA: -5% por año a partir de 40 (Antes era -12%)
            double bajadaFuerte = (edad - 40) * 0.05;

            factorEdad -= (bajadaSuave + bajadaFuerte);
        }

        //Esto asegura que un veterano con buena media siga valiendo al menos el 24% de su valor base.
        if (factorEdad < 0.25) factorEdad = 0.24;

        //3. CÁLCULO FINAL
        double precioFinal = precioBase * factorEdad;

        //Mínimo absoluto de mercado: 250.000 €
        if (precioFinal < 250_000) precioFinal = 250_000;

        //Redondear a las 50.000 unidades más cercanas
        //return (int) (Math.round(precioFinal / 50000.0) * 50000);
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
}