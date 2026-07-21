CLASE 12 - PASO A.1: crear `CursoRestController.java` en este paquete (`controller/`) copiando el bloque de abajo.

```java
package com.ufide.cursosapp.controller;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;

import com.ufide.cursosapp.entity.Curso;
import com.ufide.cursosapp.service.CursoService;

// @RestController = @Controller + @ResponseBody en cada metodo.
// Cada retorno se serializa directo a JSON (via Jackson) - no busca una
// vista Thymeleaf como hace CursoController (el @Controller "de siempre").
@RestController
@RequestMapping("/api/cursos")
public class CursoRestController {

    @Autowired
    private CursoService cursoService;

    // GET /api/cursos -> 200 OK + JSON con la lista de cursos.
    // Usamos listarConProfesor() (JOIN FETCH, de S9) y no listar() a proposito:
    // profesor es LAZY, y hace falta traerlo resuelto para serializar bien a JSON.
    @GetMapping
    public List<Curso> listar() {
        return cursoService.listarConProfesor();
    }

    // GET /api/cursos/{id} -> 200 OK + curso, o 404 si no existe.
    @GetMapping("/{id}")
    public ResponseEntity<Curso> detalle(@PathVariable Long id) {
        return cursoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST /api/cursos -> 201 Created + Location header + el curso creado.
    // Body esperado (JSON):
    // { "nombre": "...", "descripcion": "...", "creditos": 4, "profesor": { "id": 1 } }
    //
    // CLASE 12 - PASO E.5: cuando llegues a la Parte E (JWT), descomenta la
    // linea @PreAuthorize de abajo - es EXACTAMENTE la misma anotacion que
    // usa CursoController (S11) para las vistas HTML. No le importa si el
    // Authentication vino de una sesion o de un JWT valido.
    // @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Curso> crear(@Valid @RequestBody Curso curso) {
        Curso guardado = cursoService.guardar(curso);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(guardado.getId())
                .toUri();
        return ResponseEntity.created(location).body(guardado);
    }

    // PUT /api/cursos/{id} -> 200 OK + curso actualizado, o 404 si no existe.
    // CLASE 12 - PASO E.5: descomentar junto con la de arriba.
    // @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Curso> actualizar(@PathVariable Long id, @Valid @RequestBody Curso curso) {
        if (cursoService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        curso.setId(id);
        return ResponseEntity.ok(cursoService.guardar(curso));
    }

    // DELETE /api/cursos/{id} -> 204 No Content si se borro, 404 si no existia.
    // CLASE 12 - PASO E.5: descomentar junto con las dos de arriba.
    // @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (cursoService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        cursoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

CLASE 12 - PASO D.2: crear `RolRestController.java` en este paquete (`controller/`) copiando el bloque de abajo. Requiere haber hecho antes el PASO D.1 (`security/Rol.java`).

```java
package com.ufide.cursosapp.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ufide.cursosapp.security.Rol;

// GET /api/roles -> 200 + JSON con los roles disponibles en el sistema,
// leidos directo del enum Rol (una sola fuente de verdad, en vez de tener
// la lista de roles repetida en varios lugares).
//
// Requiere estar autenticado (cualquier JWT valido alcanza - no hace falta
// ser ADMIN).
@RestController
@RequestMapping("/api/roles")
public class RolRestController {

    @GetMapping
    public List<String> listar() {
        return Arrays.stream(Rol.values())
                .map(Enum::name)
                .toList();
    }
}
```

---

CLASE 12 - PASO E.3: crear `AuthController.java` en este paquete (`controller/`) copiando el bloque de abajo. Requiere haber hecho antes los PASO E.1 (`security/JwtService.java`) y el bean `authenticationManager` del PASO E.4 en `SecurityConfig.java`.

```java
package com.ufide.cursosapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ufide.cursosapp.security.JwtService;

// Unico endpoint publico de /api/** (junto con el propio login por
// formulario de las vistas HTML, que no toca esta clase). Recibe
// username+password, reutiliza el AuthenticationManager que Spring Security
// ya usa por debajo para el login de sesion, y si las credenciales son
// validas devuelve un JWT en vez de crear una sesion.
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    // POST /api/auth/login -> 200 + { "token": "..." }
    // Si las credenciales son invalidas, authenticationManager.authenticate()
    // lanza una excepcion (BadCredentialsException) que Spring Security
    // convierte automaticamente en un 401 - no hace falta capturarla a mano.
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        String rol = auth.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("");

        String token = jwtService.generarToken(request.username(), rol);
        return ResponseEntity.ok(new LoginResponse(token));
    }

    public record LoginRequest(String username, String password) {}

    public record LoginResponse(String token) {}
}
```

Nota: este paquete ya existe (tiene `CursoController.java`, `HomeController.java`, `UsuarioController.java`, `PasswordResetController.java` de clases anteriores) - solo hace falta agregar los archivos nuevos al lado.
