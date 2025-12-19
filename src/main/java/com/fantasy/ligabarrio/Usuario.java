package com.fantasy.ligabarrio;

import jakarta.persistence.*;

@Entity
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String password;
    private int presupuesto;
    private boolean esAdmin; // <--- ESTO ES LO QUE NECESITAMOS

    public Usuario() {
    }

    public Usuario(String nombre, String password, int presupuesto, boolean esAdmin) {
        this.nombre = nombre;
        this.password = password;
        this.presupuesto = presupuesto;
        this.esAdmin = esAdmin;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public int getPresupuesto() { return presupuesto; }
    public void setPresupuesto(int presupuesto) { this.presupuesto = presupuesto; }

    // IMPORTANTE: Java necesita esto para saber si eres admin
    public boolean isEsAdmin() { return esAdmin; }
    public void setEsAdmin(boolean esAdmin) { this.esAdmin = esAdmin; }
}