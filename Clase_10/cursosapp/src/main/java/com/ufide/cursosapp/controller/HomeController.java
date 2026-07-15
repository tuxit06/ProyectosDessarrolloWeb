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

    // CLASE 10 - PASO C.2: descomentar este metodo.
    // SecurityConfig ya usa .loginPage("/login"), pero eso solo le dice a
    // Spring Security A DONDE redirigir - alguien tiene que servir esa vista.
    // Sin este mapping, /login responde 404 (Whitelabel Error Page).
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
