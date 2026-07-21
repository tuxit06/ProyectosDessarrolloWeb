package com.ufide.cursosapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// CLASE 11 - PASO A.1: descomentar este import junto con la anotacion de abajo.
// import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
// CLASE 11 - PASO G.3: descomentar este import junto con el @Bean de abajo.
// import org.springframework.security.web.session.HttpSessionEventPublisher;

@Configuration
@EnableWebSecurity
// CLASE 11 - PASO A.1: descomentar la linea de abajo para habilitar
// @PreAuthorize en los metodos del Controller. Sin esta anotacion,
// @PreAuthorize se ignora silenciosamente - no tira error, simplemente
// no protege nada (el error mas comun de esta clase).
// @EnableMethodSecurity
public class SecurityConfig {

    // BCrypt: cada vez que se hashea la misma contrasena da un resultado distinto
    // (usa un "salt" aleatorio), pero matches() siempre puede validarla igual.
    // Nunca comparar contrasenas con equals() ni guardarlas en texto plano.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // CLASE 11 - PASO G.3: descomentar este @Bean. Publica los eventos de
    // creacion/destruccion de HttpSession al resto de Spring Security. Sin
    // esto, maximumSessions() de mas abajo cuenta sesiones viejas que el
    // navegador ya cerro como si siguieran activas (se "desincroniza" con
    // la realidad).
    // @Bean
    // public HttpSessionEventPublisher httpSessionEventPublisher() {
    //     return new HttpSessionEventPublisher();
    // }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Publico: inicio, login, estilos e imagenes
                // CLASE 11 - PASO C.1: agregar "/403" a esta lista de rutas
                // publicas (si no, entrar a /403 tambien pediria login y
                // generaria un loop cuando @PreAuthorize redirige ahi).
                // CLASE 11 - PASO G.3 (continua): agregar tambien
                // "/olvide-password" y "/restablecer-password" - alguien que
                // olvido su contrasena, por definicion, no esta logueado.
                .requestMatchers("/", "/login", "/css/**", "/img/**").permitAll()
                // Todo lo demas (incluido /cursos/**) requiere estar logueado.
                // La restriccion POR ROL ahora vive en los metodos del
                // Controller con @PreAuthorize (Parte B del lab).
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
                // CLASE 11 - PASO D.1: descomentar estas dos lineas para un
                // logout mas completo (invalida la sesion del servidor y
                // borra la cookie, ademas de redirigir).
                // .invalidateHttpSession(true)
                // .deleteCookies("JSESSIONID")
                .permitAll()
            )
            // CLASE 11 - PASO C.2: quitar el punto y coma de la linea de
            // arriba (".logout(...)") y descomentar esta linea completa para
            // redirigir a una pagina 403 propia cuando @PreAuthorize bloquee
            // a alguien. Fijate que el punto y coma final se mueve para aca.
            // .exceptionHandling(ex -> ex.accessDeniedPage("/403"))
            // CLASE 11 - PASO G.4: descomentar (junto con quitar el punto y
            // coma de la linea de arriba, igual que en el PASO C.2). Maximo
            // 1 sesion activa por usuario: si el mismo username se loguea
            // desde un segundo navegador, maxSessionsPreventsLogin(false)
            // invalida la sesion VIEJA en vez de bloquear el login nuevo.
            // .sessionManagement(session -> session
            //     .maximumSessions(1)
            //     .maxSessionsPreventsLogin(false)
            // )
            ;
        return http.build();
    }
}
