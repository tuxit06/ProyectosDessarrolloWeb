CLASE 11 - PARTE F.5: crear el archivo `UsuarioController.java` en este mismo paquete (`controller/`), copiando el bloque de abajo.

```java
package com.ufide.cursosapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

import com.ufide.cursosapp.entity.Usuario;
import com.ufide.cursosapp.service.UsuarioService;

// Mismo patron MVC + @PreAuthorize que CursoController (Parte B) - la
// diferencia es donde se pone la anotacion: aca va UNA VEZ a nivel de CLASE,
// asi que aplica a los 6 metodos de golpe. En CursoController se puso
// metodo por metodo porque listar()/detalle() quedaban abiertos a cualquier
// autenticado; aca ni siquiera LISTAR usuarios deberia ser publico para un
// USER - es informacion sensible del sistema, no del dominio del curso.
@Controller
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping
    public String listar(Model modelo) {
        modelo.addAttribute("usuarios", usuarioService.listar());
        return "usuarios";
    }

    @GetMapping("/nuevo")
    public String mostrarFormNuevo(Model modelo) {
        modelo.addAttribute("usuario", new Usuario());
        return "usuarios/form";
    }

    @PostMapping
    public String guardar(@Valid @ModelAttribute("usuario") Usuario usuario,
                          BindingResult result,
                          RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "usuarios/form";
        }
        usuarioService.crear(usuario);
        ra.addFlashAttribute("ok", "Usuario creado correctamente");
        return "redirect:/usuarios";
    }

    @GetMapping("/{id}/editar")
    public String mostrarFormEditar(@PathVariable Long id, Model modelo) {
        Usuario usuario = usuarioService.buscarPorId(id).orElseThrow();
        // Nunca se muestra el hash en el formulario: se deja vacio. Si el
        // admin no escribe una contrasena nueva, el service conserva la
        // actual (ver UsuarioService.actualizar()).
        usuario.setPassword("");
        modelo.addAttribute("usuario", usuario);
        return "usuarios/form";
    }

    // Sin @Valid a proposito: el password puede llegar en blanco (el admin
    // no quiso cambiarla) y @NotBlank en la entidad lo rechazaria.
    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @ModelAttribute("usuario") Usuario usuario,
                             RedirectAttributes ra) {
        usuarioService.actualizar(id, usuario);
        ra.addFlashAttribute("ok", "Usuario actualizado correctamente");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        // Authentication inyectada directo en el metodo: Spring Security la
        // arma sola a partir de la sesion activa. auth.getName() es el
        // username del usuario logueado AHORA MISMO (el admin haciendo clic).
        Usuario usuario = usuarioService.buscarPorId(id).orElseThrow();
        if (usuario.getUsername().equals(auth.getName())) {
            ra.addFlashAttribute("error", "No podes eliminar tu propio usuario mientras estas logueado con el.");
            return "redirect:/usuarios";
        }
        usuarioService.eliminar(id);
        ra.addFlashAttribute("ok", "Usuario eliminado correctamente");
        return "redirect:/usuarios";
    }
}
```

Reglas:

- Requiere `UsuarioService.java` ya creado (ver `package-info.md` de `service/`).
- El `@PreAuthorize("hasRole('ADMIN')")` de clase reemplaza tener que repetirlo en cada metodo - probalo comentandolo un momento y entrando como `profesor`/`estudiante` para ver que CUALQUIER autenticado entraria a `/usuarios`.

---

CLASE 11 - PARTE G.9: crear el archivo `PasswordResetController.java` en este mismo paquete (`controller/`), copiando el bloque de abajo.

```java
package com.ufide.cursosapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ufide.cursosapp.entity.Usuario;
import com.ufide.cursosapp.service.EmailService;
import com.ufide.cursosapp.service.UsuarioService;

// Estas rutas van SIN @PreAuthorize y publicas en SecurityConfig (permitAll)
// a proposito: alguien que olvido su contrasena, por definicion, no puede
// estar logueado todavia.
@Controller
public class PasswordResetController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private EmailService emailService;

    @GetMapping("/olvide-password")
    public String mostrarFormOlvide() {
        return "olvide-password";
    }

    @PostMapping("/olvide-password")
    public String procesarOlvide(@RequestParam String email, RedirectAttributes ra) {
        // IMPORTANTE (seguridad): el mensaje de exito es EXACTAMENTE el mismo
        // exista o no ese email en la base de datos. Si dijeramos "ese email
        // no existe" estariamos regalando una forma de averiguar que
        // usuarios estan registrados en el sistema (enumeracion de cuentas).
        usuarioService.buscarPorEmail(email).ifPresent(usuario -> {
            String token = usuarioService.generarTokenReset(usuario);
            String enlace = "http://localhost:8080/restablecer-password?token=" + token;
            emailService.enviarLinkRecuperacion(usuario.getEmail(), enlace);
        });
        ra.addFlashAttribute("ok",
                "Si el correo existe en el sistema, te enviamos un enlace para restablecer tu contrasena.");
        return "redirect:/olvide-password";
    }

    @GetMapping("/restablecer-password")
    public String mostrarFormRestablecer(@RequestParam String token, Model modelo) {
        Usuario usuario = usuarioService.buscarPorTokenValido(token).orElse(null);
        if (usuario == null) {
            modelo.addAttribute("tokenInvalido", true);
            return "restablecer-password";
        }
        modelo.addAttribute("token", token);
        return "restablecer-password";
    }

    @PostMapping("/restablecer-password")
    public String procesarRestablecer(@RequestParam String token,
                                      @RequestParam String password,
                                      Model modelo,
                                      RedirectAttributes ra) {
        Usuario usuario = usuarioService.buscarPorTokenValido(token).orElse(null);
        if (usuario == null) {
            modelo.addAttribute("tokenInvalido", true);
            return "restablecer-password";
        }
        usuarioService.cambiarPassword(usuario, password);
        ra.addFlashAttribute("ok", "Contrasena actualizada. Ya podes iniciar sesion.");
        return "redirect:/login";
    }
}
```

Reglas:

- Requiere `/olvide-password` y `/restablecer-password` en `permitAll()` de `SecurityConfig.java` (PASO G.3) - si no, Spring Security les va a pedir login antes de mostrarlas, lo cual rompe el flujo completo.
