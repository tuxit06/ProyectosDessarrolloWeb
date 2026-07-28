CLASE 12 - PASO D.1: crear `Rol.java` en este paquete (`security/`) copiando el bloque de abajo.

```java
package com.ufide.cursosapp.security;

// Constantes de rol como enum, en vez de strings sueltos ("ADMIN", "USER")
// repetidos a mano en @PreAuthorize, seed-data.sql, formularios, etc.
//
// Un enum no evita errores de tipeo dentro de una expresion SpEL
// (@PreAuthorize("hasRole('ADMIN')") sigue siendo, por dentro, un String) -
// pero sí evita errores de tipeo en cualquier lugar de Java que use
// Rol.ADMIN en vez de escribir "ADMIN" a mano, y sirve como fuente unica de
// verdad sobre que roles existen en el sistema. Ver RolRestController
// (expone estos valores via API) y UsuarioService.validarRol() (los usa
// para validar datos que entran desde un formulario o un POST JSON).
public enum Rol {
    ADMIN,
    USER
}
```

---

CLASE 12 - PASO E.1: crear `JwtService.java` en este paquete (`security/`) copiando el bloque de abajo.

```java
package com.ufide.cursosapp.security;

import java.util.Date;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

// Genera y valida los JWT que usa la API REST en vez de una sesion. Un JWT
// es, en el fondo, tres partes separadas por puntos (header.payload.signature)
// codificadas en Base64 - la firma es lo que garantiza que nadie modifico el
// contenido sin conocer la clave secreta.
@Component
public class JwtService {

    // Minimo 32 caracteres (256 bits) para el algoritmo HS256 - jjwt tira
    // WeakKeyException si es mas corta. Ya viene configurada en
    // application.properties (app.jwt.secret) con un valor de desarrollo.
    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Arma un token con el username como "subject" y el rol como claim
    // extra - asi el filtro puede reconstruir un Authentication completo
    // sin volver a consultar la base de datos en cada request.
    public String generarToken(String username, String rol) {
        Date ahora = new Date();
        Date expira = new Date(ahora.getTime() + expirationMs);
        return Jwts.builder()
                .subject(username)
                .claim("rol", rol)
                .issuedAt(ahora)
                .expiration(expira)
                .signWith(key())
                .compact();
    }

    public String extraerUsername(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    public String extraerRol(String token) {
        return extraerClaim(token, claims -> claims.get("rol", String.class));
    }

    // Un token es valido si el username coincide Y todavia no vencio.
    public boolean esValido(String token, String username) {
        try {
            String usernameDelToken = extraerUsername(token);
            return usernameDelToken.equals(username) && !estaExpirado(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean estaExpirado(String token) {
        return extraerClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extraerClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return resolver.apply(claims);
    }
}
```

---

CLASE 12 - PASO E.2: crear `JwtAuthFilter.java` en este paquete (`security/`) copiando el bloque de abajo. Requiere haber hecho antes el PASO E.1.

```java
package com.ufide.cursosapp.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Filtro que corre UNA VEZ por request, ANTES de que Spring Security decida
// si autoriza o no. Si viene un header "Authorization: Bearer <token>"
// valido, arma un Authentication "a mano" y lo deja en el SecurityContext -
// es el equivalente, sin sesion, de lo que formLogin() hace con la cookie de
// sesion para las vistas HTML.
//
// Importante: este filtro es ADITIVO. Si no viene header Authorization (o
// no es valido), simplemente deja pasar el request sin tocar nada - no
// rompe el login por formulario de las vistas HTML.
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            String username = jwtService.extraerUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.esValido(token, username)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token corrupto, vencido o de un usuario que ya no existe: lo
            // dejamos pasar sin autenticar - si el endpoint requiere estar
            // autenticado, la falta de Authentication resulta en 401/403
            // mas adelante en la cadena.
        }

        filterChain.doFilter(request, response);
    }
}
```

Nota: este paquete ya existe (tiene `CustomUserDetailsService.java` de S10) - solo hace falta agregar los archivos nuevos al lado.
