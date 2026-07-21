package com.ufide.cursosapp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ufide.cursosapp.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Query method derivado del nombre: Spring Data genera el SQL solo.
    // Lo usa CustomUserDetailsService para buscar al usuario que intenta loguearse.
    Optional<Usuario> findByUsername(String username);

    // CLASE 11 - PASO F.2: descomentar para el formulario de "olvide mi
    // contrasena" (Parte G) - busca al usuario por su email en vez de username.
    // Optional<Usuario> findByEmail(String email);

    // CLASE 11 - PASO G.2: descomentar para validar el token que llega por
    // el link del correo antes de mostrar el formulario de restablecer contrasena.
    // Optional<Usuario> findByResetToken(String resetToken);
}
