package com.ufide.cursosapp.controller;

import org.springframework.beans.factory.annotation.Autowired;
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
    @GetMapping("/nuevo")
    public String mostrarFormNuevo(Model modelo) {
        modelo.addAttribute("curso", new Curso());
        modelo.addAttribute("profesores", profesorService.listar());
        return "cursos/form";
    }

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
    @GetMapping("/{id}/editar")
    public String mostrarFormEditar(@PathVariable Long id, Model modelo) {
        Curso curso = cursoService.buscarPorId(id).orElseThrow();
        modelo.addAttribute("curso", curso);
        modelo.addAttribute("profesores", profesorService.listar());
        return "cursos/form";
    }

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
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        cursoService.eliminar(id);
        ra.addFlashAttribute("ok", "Curso eliminado correctamente");
        return "redirect:/cursos";
    }
}