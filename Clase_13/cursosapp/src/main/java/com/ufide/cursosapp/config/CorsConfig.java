package com.ufide.cursosapp.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

// CORS (Cross-Origin Resource Sharing): el navegador bloquea por defecto que
// JavaScript corriendo en un origen (ej. http://localhost:5500) llame a una
// API en otro origen (ej. http://localhost:8080) - a menos que el servidor
// diga explicitamente "este origen puede llamarme". Esta clase es esa
// autorizacion explicita.
//
// IMPORTANTE: esto NO afecta a Postman ni a curl - CORS es una restriccion
// que aplican los NAVEGADORES, no los clientes HTTP en general. Postman
// funciona igual con o sin esta configuracion.
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // En un proyecto real, poner aca el/los dominio(s) real(es) del
        // frontend (nunca "*" si se usan credenciales/cookies). Para el lab,
        // se permiten un par de origenes tipicos de desarrollo local.
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://127.0.0.1:5500"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Solo se aplica a la API JSON - las rutas HTML (/cursos, /login, etc.)
        // no las consume JavaScript de otro origen, no hace falta CORS ahi.
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
