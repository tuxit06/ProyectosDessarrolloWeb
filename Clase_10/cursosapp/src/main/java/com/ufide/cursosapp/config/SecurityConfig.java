package com.ufide.cursosapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
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
                .authorizeHttpRequests(auth -> auth
                        // Publico: inicio, login, estilos e imagenes
                        .requestMatchers("/", "/login", "/css/**", "/img/**").permitAll()
                        // Todo lo demas (incluido /cursos/**) requiere estar logueado.
                        // La restriccion POR ROL se agrega en S11 con @PreAuthorize.
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/cursos", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}