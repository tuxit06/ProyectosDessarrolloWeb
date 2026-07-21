package com.ufide.cursosapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

// CLASE 12 - PASO E.4: descomentar estos dos imports junto con el resto del
// bloque de mas abajo.
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.security.authentication.AuthenticationManager;
// import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
// import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
// import com.ufide.cursosapp.security.JwtAuthFilter;

@Configuration
@EnableWebSecurity
// Habilita @PreAuthorize en los metodos del Controller (S11). Sin esta
// anotacion, @PreAuthorize se ignora silenciosamente - no tira error,
// simplemente no protege nada. Es el error mas comun de S11, y en la
// Parte E de esta clase tambien protege los metodos de escritura de
// CursoRestController.
@EnableMethodSecurity
public class SecurityConfig {

    // CLASE 12 - PASO E.4: descomentar este campo. Spring inyecta el bean
    // JwtAuthFilter (creado en el PASO E.2) automaticamente por ser @Component.
    // @Autowired
    // private JwtAuthFilter jwtAuthFilter;

    // BCrypt: cada vez que se hashea la misma contrasena da un resultado distinto
    // (usa un "salt" aleatorio), pero matches() siempre puede validarla igual.
    // Nunca comparar contrasenas con equals() ni guardarlas en texto plano.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Heredado de S11: publica los eventos de creacion/destruccion de
    // HttpSession al resto de Spring Security. Sin este bean, maximumSessions()
    // de mas abajo cuenta sesiones viejas que el navegador ya cerro como si
    // siguieran activas.
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    // CLASE 12 - PASO E.4: descomentar este bean. Expone el
    // AuthenticationManager que Spring Security ya arma internamente para
    // formLogin(), para poder usarlo en AuthController.login() y autenticar
    // username+password "a mano" antes de emitir un JWT.
    // @Bean
    // public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    //     return config.getAuthenticationManager();
    // }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CLASE 12 - PASO C.1: descomentar esta linea para activar el
            // CorsConfigurationSource bean (CorsConfig, PASO B.1).
            // .cors(cors -> {})
            .authorizeHttpRequests(auth -> auth
                // Publico: inicio, login, estilos e imagenes, la pagina 403,
                // y el flujo de recuperacion de contrasena (heredado de S11).
                .requestMatchers("/", "/login", "/403", "/css/**", "/img/**",
                        "/olvide-password", "/restablecer-password").permitAll()
                // CLASE 12 - PASO C.2: descomentar esta linea para que la API
                // sea publica (funciona simple, sin JWT, mientras armamos
                // RestController/CORS en las Partes A-C).
                // .requestMatchers("/api/**").permitAll()
                //
                // CLASE 12 - PASO E.4: cuando llegues a la Parte E (JWT),
                // volve a COMENTAR la linea de PASO C.2 de arriba (ya cumplio
                // su proposito) y descomenta estas dos en su lugar: el login
                // de la API queda publico (hace falta poder pedir un token
                // sin tener uno todavia), el resto de /api/** ahora exige un
                // JWT valido en vez de quedar abierto.
                // .requestMatchers("/api/auth/login").permitAll()
                // .requestMatchers("/api/**").authenticated()
                // Todo lo demas (incluido /cursos/** y /usuarios/**) requiere
                // estar logueado. La restriccion POR ROL vive en los
                // metodos/clases del Controller con @PreAuthorize, no aqui.
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
            // Si un usuario autenticado intenta algo que @PreAuthorize le bloquea,
            // Spring Security lanza AccessDeniedException. accessDeniedPage()
            // redirige esa excepcion a una vista propia (heredado de S11).
            .exceptionHandling(ex -> ex.accessDeniedPage("/403"))
            // Heredado de S11: maximo 1 sesion activa por usuario para las
            // vistas HTML. No afecta a la API una vez que use JWT (sin
            // estado, sin cookie de sesion).
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            );
            // CLASE 12 - PASO E.4: descomentar esta linea (junto con el punto
            // y coma de arriba, que hay que borrar - el addFilterBefore pasa
            // a ser el ultimo eslabon de la cadena). El filtro JWT corre
            // ANTES del filtro que procesa el login por formulario: si el
            // request trae un Bearer token valido, ya llega autenticado a
            // ese punto; si no, sigue de largo sin tocar nada.
            // .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
