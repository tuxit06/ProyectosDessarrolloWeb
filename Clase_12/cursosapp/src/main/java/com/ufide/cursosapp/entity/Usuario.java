package com.ufide.cursosapp.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import jakarta.validation.constraints.Email;
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

    // CLASE 11 - PASO F.1: email para recuperacion de contrasena (Parte G).
    // Se valida en el formulario (@NotBlank/@Email) pero la columna queda
    // nullable a nivel de base de datos, para no romper las filas que ya
    // existian de S10 cuando Hibernate agregue esta columna con ddl-auto=update.
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato valido")
    private String email;

    // CLASE 11 - PASO G.1: token de un solo uso para restablecer la
    // contrasena, con su fecha de expiracion. UsuarioService los genera en
    // generarTokenReset() y los limpia (vuelven a null) apenas se usan una
    // vez - ver cambiarPassword().
    private String resetToken;
    private LocalDateTime resetTokenExpiracion;

    public Usuario() {
    }

    public Usuario(Long id, String username, String password, String rol, String email) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.rol = rol;
        this.email = email;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public LocalDateTime getResetTokenExpiracion() { return resetTokenExpiracion; }
    public void setResetTokenExpiracion(LocalDateTime resetTokenExpiracion) { this.resetTokenExpiracion = resetTokenExpiracion; }
}
