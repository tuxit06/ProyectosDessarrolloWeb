CLASE 11 - PARTE F.4: crear el archivo `UsuarioService.java` en este mismo paquete (`service/`), copiando el bloque de abajo.

```java
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
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        return repo.save(usuario);
    }

    // Se usa al EDITAR: si el campo password del formulario viene vacio, el
    // admin no quiso cambiarla - se conserva el hash existente. Por eso el
    // Controller NO usa @Valid en el metodo actualizar() (el password puede
    // llegar en blanco a proposito).
    public Usuario actualizar(Long id, Usuario formUsuario) {
        Usuario existente = repo.findById(id).orElseThrow();
        existente.setUsername(formUsuario.getUsername());
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
```

Reglas:

- Requiere que ya hayas hecho el PASO F.1 (campos `email`, `resetToken`, `resetTokenExpiracion` en `Usuario.java`) y el PASO F.2/G.2 (`findByEmail`/`findByResetToken` en `UsuarioRepository.java`) - si no, esto no compila.
- No crea un `PasswordEncoder` nuevo: se inyecta el mismo `@Bean` que ya vive en `SecurityConfig` desde S10.

---

CLASE 11 - PARTE G.8: crear el archivo `EmailService.java` en este mismo paquete (`service/`), copiando el bloque de abajo. Requiere que ya hayas agregado la dependencia `spring-boot-starter-mail` (ya viene en el `pom.xml` de este proyecto) y la configuracion `spring.mail.*` en `application.properties` (ya viene tambien, solo falta que el profesor comparta las credenciales de Mailtrap en clase).

```java
package com.ufide.cursosapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// Envoltorio simple sobre JavaMailSender, que Spring Boot autoconfigura solo
// a partir de las propiedades spring.mail.* en application.properties (no
// hace falta un @Bean manual - por eso alcanza con @Autowired directo).
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Un solo metodo: mandar el link de recuperacion de contrasena. En un
    // sistema real se usarian plantillas HTML (Thymeleaf tiene soporte para
    // esto), pero para el objetivo de la clase alcanza con texto plano.
    public void enviarLinkRecuperacion(String destinatario, String enlace) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject("SC-403 - Recuperar contrasena");
        mensaje.setText(
                "Recibimos una solicitud para restablecer tu contrasena.\n\n" +
                "Hace click en el siguiente enlace (valido por 30 minutos):\n" +
                enlace + "\n\n" +
                "Si no pediste este cambio, ignora este correo - tu contrasena " +
                "actual sigue funcionando normalmente."
        );
        mailSender.send(mensaje);
    }
}
```
