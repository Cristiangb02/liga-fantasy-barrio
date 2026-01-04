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
    private boolean esAdmin;
    private boolean activo; //Control de acceso

    public Usuario() {
    }

    public Usuario(String nombre, String password, int presupuesto, boolean esAdmin) {
        this.nombre = nombre;
        this.password = password;
        this.presupuesto = presupuesto;
        this.esAdmin = esAdmin;
        this.activo = esAdmin;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public int getPresupuesto() { return presupuesto; }
    public void setPresupuesto(int presupuesto) { this.presupuesto = presupuesto; }
    public boolean isEsAdmin() { return esAdmin; }
    public void setEsAdmin(boolean esAdmin) { this.esAdmin = esAdmin; }
    
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
