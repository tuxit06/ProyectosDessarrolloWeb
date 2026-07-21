package com.ufide.cursosapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
// Habilita @PreAuthorize en los metodos del Controller (S11). Sin esta
// anotacion, @PreAuthorize se ignora silenciosamente - no tira error,
// simplemente no protege nada. Es el error mas comun de esta clase.
@EnableMethodSecurity
public class SecurityConfig {

    // BCrypt: cada vez que se hashea la misma contrasena da un resultado distinto
    // (usa un "salt" aleatorio), pero matches() siempre puede validarla igual.
    // Nunca comparar contrasenas con equals() ni guardarlas en texto plano.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CLASE 12 - PASO C.1: descomentar esta linea para activar el
            // CorsConfigurationSource bean (CorsConfig.java, PASO B.1).
            // .cors(cors -> {})
            .authorizeHttpRequests(auth -> auth
                // Publico: inicio, login, estilos e imagenes, y la pagina 403
                // CLASE 12 - PASO C.2: agregar "/api/**" a esta lista para que
                // la API REST no pida login (foco de la clase: RestController/
                // RequestBody/ResponseEntity/CORS, no autenticacion de APIs).
                .requestMatchers("/", "/login", "/403", "/css/**", "/img/**").permitAll()
                // Todo lo demas (incluido /cursos/**) requiere estar logueado.
                // La restriccion POR ROL (solo ADMIN puede crear/editar/eliminar)
                // ahora vive en los metodos del Controller con @PreAuthorize,
                // no aqui - aqui solo se exige "estar logueado", sea cual sea el rol.
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/cursos", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // Si un usuario autenticado intenta algo que @PreAuthorize le bloquea
            // (ej. USER tratando de eliminar un curso), Spring Security lanza
            // AccessDeniedException. accessDeniedPage() redirige esa excepcion
            // a una vista propia en vez del error 403 en blanco de Spring Boot.
            .exceptionHandling(ex -> ex.accessDeniedPage("/403"));
        return http.build();
    }
}
