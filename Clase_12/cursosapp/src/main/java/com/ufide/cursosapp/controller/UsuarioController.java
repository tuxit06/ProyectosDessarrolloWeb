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
        // Autenticacion inyectada directo en el metodo: Spring Security la
        // arma solo a partir de la sesion activa. auth.getName() es el
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
