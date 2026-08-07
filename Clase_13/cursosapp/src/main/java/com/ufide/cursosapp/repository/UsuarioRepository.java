package com.ufide.cursosapp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ufide.cursosapp.entity.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Query method derivado del nombre: Spring Data genera el SQL solo.
    // Lo usa CustomUserDetailsService para buscar al usuario que intenta loguearse.
    Optional<Usuario> findByUsername(String username);
}
