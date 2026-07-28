package com.ufide.cursosapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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

import com.ufide.cursosapp.entity.Curso;
import com.ufide.cursosapp.service.CursoService;
import com.ufide.cursosapp.service.ProfesorService;

@Controller
@RequestMapping("/cursos")
public class CursoController {

    @Autowired
    private CursoService cursoService;

    @Autowired
    private ProfesorService profesorService;

    // Sin @PreAuthorize: cualquier usuario autenticado (ADMIN o USER) puede
    // listar y ver el detalle de un curso. La restriccion es solo para
    // las operaciones que MODIFICAN datos (crear, editar, eliminar).
    @GetMapping
    public String listar(Model modelo) {
        modelo.addAttribute("cursos", cursoService.listarConProfesor());
        return "cursos";
    }

    @GetMapping("/{id}")
    public String detalle(Model modelo, @PathVariable Long id) {
        Curso encontrado = cursoService.buscarPorId(id).orElse(null);
        modelo.addAttribute("curso", encontrado);
        return "curso";
    }

    // ===== CREATE =====
    // hasRole("ADMIN") revisa que el usuario tenga la authority "ROLE_ADMIN"
    // (Spring agrega el prefijo ROLE_ solo). Es equivalente a escribir
    // @PreAuthorize("hasAuthority('ROLE_ADMIN')") - mismo resultado, distinta
    // sintaxis. Si un USER intenta entrar aca, Spring Security lanza
    // AccessDeniedException y SecurityConfig lo redirige a /403.
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/nuevo")
    public String mostrarFormNuevo(Model modelo) {
        modelo.addAttribute("curso", new Curso());
        modelo.addAttribute("profesores", profesorService.listar());
        return "cursos/form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public String guardar(@Valid @ModelAttribute("curso") Curso curso,
                          BindingResult result,
                          RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "cursos/form";
        }
        cursoService.guardar(curso);
        ra.addFlashAttribute("ok", "Curso guardado correctamente");
        return "redirect:/cursos";
    }

    // ===== UPDATE =====
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/editar")
    public String mostrarFormEditar(@PathVariable Long id, Model modelo) {
        Curso curso = cursoService.buscarPorId(id).orElseThrow();
        modelo.addAttribute("curso", curso);
        modelo.addAttribute("profesores", profesorService.listar());
        return "cursos/form";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("curso") Curso curso,
                             BindingResult result,
                             RedirectAttributes ra) {
        if (result.hasErrors()) {
            return "cursos/form";
        }
        curso.setId(id);
        cursoService.guardar(curso);
        ra.addFlashAttribute("ok", "Curso actualizado correctamente");
        return "redirect:/cursos";
    }

    // ===== DELETE =====
    // hasAuthority("ROLE_ADMIN") logra exactamente lo mismo que hasRole("ADMIN")
    // de arriba - se deja este metodo con la forma explicita a proposito, para
    // comparar ambas sintaxis lado a lado (ver bibliografia_clase11.md).
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        cursoService.eliminar(id);
        ra.addFlashAttribute("ok", "Curso eliminado correctamente");
        return "redirect:/cursos";
    }
}
