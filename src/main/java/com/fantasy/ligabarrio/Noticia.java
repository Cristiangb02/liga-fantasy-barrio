package com.fantasy.ligabarrio;package com.fantasy.ligabarrio;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
public class Noticia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mensaje;
    private LocalDateTime fecha;

    public Noticia() {
    }

    public Noticia(String mensaje) {
        this.mensaje = mensaje;
        this.fecha = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    // 🔴 CAMBIO PUNTO 11: Añadimos la hora (HH:mm) al formato
    public String getFechaBonita() {
        if (fecha == null) return "";
        // Formato: Día/Mes/Año - Hora:Minutos
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm");
        return fecha.format(formatter);
    }
}
}
