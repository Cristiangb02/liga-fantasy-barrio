package com.fantasy.ligabarrio;

import jakarta.persistence.*;

@Entity
public class Jugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String posicion;
    private int edad;   // NUEVO
    private double media; // NUEVO (La nota del 0 al 10)

    private int valor;
    private String imagen;

    @ManyToOne
    private Usuario propietario;

    private int puntosAcumulados;
    private int clausula;
    private Long jornadaFichaje;

    public Jugador() {
    }

    // CONSTRUCTOR NUEVO: Ya no pide "valor", pide "edad" y "media"
    public Jugador(String nombre, String posicion, int edad, double media, String imagen) {
        this.nombre = nombre;
        this.posicion = posicion;
        this.edad = edad;
        this.media = media;
        this.imagen = imagen;
        this.puntosAcumulados = 0;
        this.jornadaFichaje = 0L;

        // 🧮 CÁLCULO AUTOMÁTICO DE PRECIO f(x,y)
        this.valor = calcularPrecioInicial(edad, media);
        this.clausula = this.valor;
    }

    // LA FÓRMULA MATEMÁTICA
    private int calcularPrecioInicial(int edad, double media) {
        // 1. Base exponencial según la media (ajusta el 150000 o el 1.65 si quieres precios más altos/bajos)
        double precioBase = 150_000 * Math.pow(1.65, media);

        // 2. Factor de edad (Jóvenes valen más, veteranos menos)
        // 27 es la edad "neutra". Cada año de diferencia es un +/- 1.5%
        double factorEdad = 1.0 + ((27.0 - edad) * 0.015);

        // 3. Cálculo final
        double precioFinal = precioBase * factorEdad;

        // 4. Mínimo 150k y redondeo a bonitos (multiplos de 10.000)
        if (precioFinal < 150_000) precioFinal = 150_000;
        return (int) (Math.round(precioFinal / 10000.0) * 10000);
    }

    // --- GETTERS Y SETTERS ---
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
    public String getUrlImagen() { return imagen; } // Mantenemos getUrlImagen para compatibilidad con tu HTML
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