package com.ufide.cursosapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model modelo,
            @RequestParam(defaultValue = "Estudiante") String nombre) {
        modelo.addAttribute("nombre", nombre);
        modelo.addAttribute("curso", "SC-403 Desarrollo de Aplicaciones Web");
        return "home";
    }

    // Pagina de "acceso denegado" - a donde SecurityConfig redirige cuando
    // @PreAuthorize bloquea a un usuario autenticado que no tiene el rol
    // necesario (ej. USER intentando eliminar un curso). Ver PASO C.2.
    @GetMapping("/403")
    public String accesoDenegado() {
        return "403";
    }
}
