package com.fantasy.ligabarrio;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Oferta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Jugador jugador;

    @ManyToOne
    private Usuario comprador;

    @ManyToOne
    private Usuario vendedor;

    private int cantidad;
    private String estado; //"PENDIENTE", "ACEPTADA", "RECHAZADA"
    private LocalDateTime fecha;

    public Oferta() {}

    public Oferta(Jugador jugador, Usuario comprador, Usuario vendedor, int cantidad) {
        this.jugador = jugador;
        this.comprador = comprador;
        this.vendedor = vendedor;
        this.cantidad = cantidad;
        this.estado = "PENDIENTE";
        this.fecha = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Jugador getJugador() { return jugador; }
    public Usuario getComprador() { return comprador; }
    public Usuario getVendedor() { return vendedor; }
    public int getCantidad() { return cantidad; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getFecha() { return fecha; }
}