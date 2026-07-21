CLASE 12 - PASO B.1: crear `CorsConfig.java` en este paquete (`config/`) copiando el bloque de abajo.

```java
package com.ufide.cursosapp.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

// CORS: el navegador bloquea por defecto que JavaScript corriendo en un
// origen (ej. http://localhost:5500) llame a una API en otro origen
// (ej. http://localhost:8080), a menos que el servidor lo autorice
// explicitamente. Esta clase es esa autorizacion.
//
// IMPORTANTE: esto no afecta a Postman ni a curl - CORS es una restriccion
// que aplican los NAVEGADORES.
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://127.0.0.1:5500"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
```

Nota: este paquete ya existe (tiene `SecurityConfig.java`) - solo hace falta agregar el archivo nuevo al lado.
