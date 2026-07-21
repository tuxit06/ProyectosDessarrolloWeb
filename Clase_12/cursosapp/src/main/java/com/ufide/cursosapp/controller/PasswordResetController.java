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
