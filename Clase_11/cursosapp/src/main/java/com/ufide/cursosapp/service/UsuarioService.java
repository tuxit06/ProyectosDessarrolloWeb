package com.ufide.cursosapp.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ufide.cursosapp.entity.Usuario;
import com.ufide.cursosapp.repository.UsuarioRepository;

// CRUD de usuarios (Parte F) + generacion/validacion de tokens de
// recuperacion de contrasena (Parte G). El PasswordEncoder es el MISMO bean
// que ya existe en SecurityConfig desde S10 - se reutiliza, no se crea uno
// nuevo (seria un segundo hasher inconsistente con el que valida el login).
@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository repo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Usuario> listar() {
        return repo.findAll();
    }

    public Optional<Usuario> buscarPorId(Long id) {
        return repo.findById(id);
    }

    // Se usa al CREAR un usuario nuevo desde el formulario: la contrasena
    // llega en texto plano y hay que hashearla ANTES de guardar. Sin este
    // paso, el login del nuevo usuario fallaria (BCrypt nunca reconoceria
    // un password en texto plano como valido).
    public Usuario crear(Usuario usuario) {
        // usuario.getPassword() -> patito123
        // passwordEncoder.encode(patito123) ->
        // 2b$10$Qj/x8JpJMw7eOP7tPSqyRO9.IqRQQSyDWMQUUy/1mI0IJTPy5eSIq
        // usuario.setPassword(2b$10$Qj/x8JpJMw7eOP7tPSqyRO9.IqRQQSyDWMQUUy/1mI0IJTPy5eSIq)
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        return repo.save(usuario);
    }

    // Se usa al EDITAR: si el campo password del formulario viene vacio, el
    // admin no quiso cambiarla - se conserva el hash existente. Por eso el
    // Controller NO usa @Valid en el metodo actualizar() (el password puede
    // llegar en blanco a proposito).
    public Usuario actualizar(Long id, Usuario formUsuario) {
        Usuario existente = repo.findById(id).orElseThrow();
        if (formUsuario.getUsername() != null && !formUsuario.getUsername().isBlank()) {
            existente.setUsername(formUsuario.getUsername());
        }
        existente.setEmail(formUsuario.getEmail());
        existente.setRol(formUsuario.getRol());
        if (formUsuario.getPassword() != null && !formUsuario.getPassword().isBlank()) {
            existente.setPassword(passwordEncoder.encode(formUsuario.getPassword()));
        }
        return repo.save(existente);
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }

    // ===== Recuperacion de contrasena (Parte G) =====

    public Optional<Usuario> buscarPorEmail(String email) {
        return repo.findByEmail(email);
    }

    // Genera un token de un solo uso (UUID: practicamente imposible de
    // adivinar) valido por 30 minutos, lo guarda en el usuario y lo devuelve
    // para que el Controller arme el link del correo.
    public String generarTokenReset(Usuario usuario) {
        String token = UUID.randomUUID().toString();
        usuario.setResetToken(token);
        usuario.setResetTokenExpiracion(LocalDateTime.now().plusMinutes(30));
        repo.save(usuario);
        return token;
    }

    // Un token es valido si existe Y todavia no vencio. Si cualquiera de las
    // dos condiciones falla, el Optional viene vacio y el Controller muestra
    // "enlace invalido o vencido" sin distinguir cual de las dos paso.
    public Optional<Usuario> buscarPorTokenValido(String token) {
        return repo.findByResetToken(token)
                .filter(u -> u.getResetTokenExpiracion() != null
                        && u.getResetTokenExpiracion().isAfter(LocalDateTime.now()));
    }

    // Guarda la nueva contrasena (hasheada) y limpia el token - un token
    // usado NO debe poder reutilizarse para cambiar la contrasena de nuevo.
    public void cambiarPassword(Usuario usuario, String nuevaPasswordPlano) {
        usuario.setPassword(passwordEncoder.encode(nuevaPasswordPlano));
        usuario.setResetToken(null);
        usuario.setResetTokenExpiracion(null);
        repo.save(usuario);
    }
}