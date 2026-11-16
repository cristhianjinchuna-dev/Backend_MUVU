package com.example.desarrollo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "habitacion", indexes = {
        @Index(name = "idx_habitacion_arrendador", columnList = "arrendador_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Habitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String direccion;

    private Double latitud;

    private Double longitud;

    @Positive
    @Column(nullable = false)
    private double precio;

    @Positive
    @Column(nullable = false)
    private Integer area;

    @Column(nullable = false)
    private Boolean esDestacada = false;

    // Texto libre del arrendador: servicios, reglas, cercanías... Opcional
    @Size(max = 1000)
    @Column(length = 1000)
    private String descripcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "arrendador_id", nullable = false)
    private Arrendador arrendador;

    @OneToMany(mappedBy = "habitacion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Imagen> imagenes = new ArrayList<>();

    @OneToMany(mappedBy = "habitacion", fetch = FetchType.LAZY)
    private List<Reserva> reservas = new ArrayList<>();

    @OneToMany(mappedBy = "habitacion", fetch = FetchType.LAZY)
    private List<PagoPublicidad> pagoPublicidades = new ArrayList<>();

    @OneToMany(mappedBy = "receptor", fetch = FetchType.LAZY)
    private List<Calificacion> calificacionesRecibidas = new ArrayList<>();

    //Para agregarImagenHabitacion ->ImagenService

    public void agregarImagen(Imagen imagen) {
        imagenes.add(imagen);
        imagen.setHabitacion(this);
    }

    public void quitarImagen(Imagen imagen) {
        imagenes.remove(imagen);
    }
}