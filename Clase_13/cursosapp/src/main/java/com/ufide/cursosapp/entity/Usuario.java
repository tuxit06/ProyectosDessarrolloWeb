package com.ufide.cursosapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El username es obligatorio")
    @Size(max = 50)
    @Column(nullable = false, unique = true)
    private String username;

    // Nunca se guarda en texto plano: siempre pasa por BCryptPasswordEncoder
    // antes de llegar aca (ver SecurityConfig / seed-data.sql).
    @NotBlank(message = "El password es obligatorio")
    @Column(nullable = false)
    private String password;

    // Rol como String simple (decision del curso para S10/S11): "ADMIN" o "USER".
    // No se modela como tabla aparte ni como enum - alcanza para el objetivo
    // de la clase, que es autenticacion (S10) y autorizacion basica (S11).
    @NotBlank(message = "El rol es obligatorio")
    @Column(nullable = false)
    private String rol;

    public Usuario() {
    }

    public Usuario(Long id, String username, String password, String rol) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.rol = rol;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
}
