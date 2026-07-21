package com.ufide.cursosapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
// CLASE 11 - PASO B.1: descomentar este import.
// import org.springframework.security.access.prepost.PreAuthorize;
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

    private final CursoService cursoService;
    private final ProfesorService profesorService;

    public CursoController(CursoService cursoServiceI, ProfesorService profesorServiceI) {
        this.cursoService = cursoServiceI;
        this.profesorService = profesorServiceI;
    }

    // Sin @PreAuthorize: cualquier usuario autenticado (ADMIN o USER) puede
    // listar y ver el detalle de un curso. La restriccion es solo para
    // las operaciones que MODIFICAN datos (crear, editar, eliminar) - ver
    // los metodos de abajo.
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
    // CLASE 11 - PASO B.2: descomentar la linea @PreAuthorize de abajo.
    // hasRole("ADMIN") revisa que el usuario tenga la authority "ROLE_ADMIN"
    // (Spring agrega el prefijo ROLE_ solo). Si un USER intenta entrar aca,
    // Spring Security lanza AccessDeniedException y SecurityConfig lo
    // redirige a /403 (una vez que hiciste la Parte C del lab).
    // @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/nuevo")
    public String mostrarFormNuevo(Model modelo) {
        modelo.addAttribute("curso", new Curso());
        modelo.addAttribute("profesores", profesorService.listar());
        return "cursos/form";
    }

    // CLASE 11 - PASO B.3: descomentar.
    // @PreAuthorize("hasRole('ADMIN')")
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
    // CLASE 11 - PASO B.4: descomentar.
    // @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/editar")
    public String mostrarFormEditar(@PathVariable Long id, Model modelo) {
        Curso curso = cursoService.buscarPorId(id).orElseThrow();
        modelo.addAttribute("curso", curso);
        modelo.addAttribute("profesores", profesorService.listar());
        return "cursos/form";
    }

    // CLASE 11 - PASO B.4 (continua): descomentar.
    // @PreAuthorize("hasRole('ADMIN')")
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
    // CLASE 11 - PASO B.5: descomentar. Ojo: esta vez usa hasAuthority en vez
    // de hasRole - a proposito, para comparar ambas sintaxis. Logran
    // EXACTAMENTE el mismo resultado (ver bibliografia_clase11.md).
    // @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        cursoService.eliminar(id);
        ra.addFlashAttribute("ok", "Curso eliminado correctamente");
        return "redirect:/cursos";
    }
}
