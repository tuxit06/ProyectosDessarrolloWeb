package com.ufide.clase4base.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

// CLASE 6: descomentar imports cuando se vayan necesitando
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// CLASE 6 - PARTE A.4 (validaciones): descomentar al agregar @Valid
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

// CLASE 6 - PARTE E (bonus Firebase): descomentar al integrar upload
// import org.springframework.web.bind.annotation.RequestParam;
// import org.springframework.web.multipart.MultipartFile;
// import com.ufide.clase4base.service.FirebaseService;

import com.ufide.clase4base.entity.Curso;
import com.ufide.clase4base.service.CursoService;

/**
 * Controlador de cursos.
 *
 * Estado actual (post Clase 4): listar y detalle leen desde MySQL via JPA.
 *
 * CLASE 6 (CRUD completo): durante la clase vamos a descomentar y completar
 * los endpoints de Crear, Editar y Eliminar siguiendo las partes del lab:
 * - PARTE A: CREATE (formulario nuevo + POST guardar)
 * - PARTE B: UPDATE (formulario editar + POST actualizar)
 * - PARTE C: DELETE con modal de confirmacion
 * - PARTE D: Toast feedback (no toca el controller, solo redirecciones)
 * - PARTE E: Bonus Firebase Storage para subir imagen
 */
@Controller
@RequestMapping("/cursos")
public class CursoController {

        // ---------- DATOS EN MEMORIA (provisorio - se reemplaza en clase 4) ----------
        private final List<Curso> cursos = new ArrayList<>();

        /*
         * public CursoController() {
         * cursos.add(new Curso(1L, "Fundamentos Web",
         * "Introduccion a HTML5, CSS3 y arquitectura cliente/servidor.",
         * 3, "Esteban Ortega"));
         * cursos.add(new Curso(2L, "Spring Boot",
         * "Backend con Java 25, MVC, Thymeleaf y Bootstrap.",
         * 4, "Esteban Ortega"));
         * cursos.add(new Curso(3L, "Bases de Datos",
         * "MySQL, Workbench, JPA, Hibernate y modelo relacional.",
         * 4, "Esteban Ortega"));
         * cursos.add(new Curso(4L, "Patrones de Diseno",
         * "MVC, Repository, Service, Inyeccion de Dependencias.",
         * 3, "Esteban Ortega"));
         * cursos.add(new Curso(5L, "Seguridad Web",
         * "Autenticacion, autorizacion y buenas practicas con Spring Security.",
         * 4, "Esteban Ortega"));
         * cursos.add(new Curso(6L, "APIs REST",
         * "Servicios JSON, consumo desde clientes, despliegue en la nube.",
         * 3, "Esteban Ortega"));
         * }
         */
        // -----------------------------------------------------------------------------

        @Autowired
        private CursoService cursoService;

        // CLASE 6 - PARTE E (bonus): descomentar para inyectar el servicio de Firebase
        // @Autowired
        // private FirebaseService firebaseService;

        @GetMapping
        public String listar(Model modelo) {
                modelo.addAttribute("cursos", cursoService.listar());
                return "cursos";
        }

        @GetMapping("/{id}")
        public String detalle(Model modelo, @PathVariable Long id) {
                /*
                 * Curso encontrado = cursos.stream()
                 * .filter(c -> c.getId().equals(id))
                 * .findFirst()
                 * .orElse(null);
                 */
                Curso encontrado = cursoService.buscarPorId(id).orElse(null);
                modelo.addAttribute("curso", encontrado);
                return "curso";
        }

        // =====================================================================
        // CLASE 6 - PARTE A: CREATE (formulario + POST guardar)
        // =====================================================================

        // PASO A.1 - descomentar para mostrar el formulario vacio de nuevo curso.
        // El template templates/cursos/form.html lo creas en el paso A.2.

        @GetMapping("/nuevo")
        public String mostrarFormNuevo(Model modelo) {
                modelo.addAttribute("curso", new Curso());
                return "cursos/form";
        }

        // PASO A.3 - descomentar para recibir el POST del formulario y guardar.
        // En PARTE A se usa sin @Valid; en PARTE A.4 reemplazas la firma por la version
        // validada (abajo).

        // @PostMapping
        // public String guardar(@ModelAttribute("curso") Curso curso,
        // RedirectAttributes ra) {
        // cursoService.guardar(curso);
        // ra.addFlashAttribute("ok", "Curso guardado correctamente");
        // return "redirect:/cursos";
        // }
        //

        // PASO A.4 - VERSION CON VALIDACIONES.
        // Cuando termines la PARTE A.4 (validaciones en Curso),
        // BORRA el guardar() de arriba y descomenta este:

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

        // =====================================================================
        // CLASE 6 - PARTE B: UPDATE (formulario editar + POST actualizar)
        // =====================================================================

        // PASO B.1 - descomentar para mostrar el formulario precargado con el curso a
        // editar.
        // Reutiliza el mismo template cursos/form.html que el CREATE.

        @GetMapping("/{id}/editar")
        public String mostrarFormEditar(@PathVariable Long id, Model modelo) {
                Curso curso = cursoService.buscarPorId(id).orElseThrow();
                modelo.addAttribute("curso", curso);
                return "cursos/form";
        }

        // PASO B.2 - descomentar para recibir el POST del editar y actualizar.
        // Truco: save() de JPA distingue INSERT vs UPDATE por el ID.

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

        // =====================================================================
        // CLASE 6 - PARTE C: DELETE con modal de confirmacion
        // =====================================================================

        // PASO C.1 - descomentar para procesar el borrado.
        // El boton + modal en la vista los agregas en los pasos C.2 y C.3.

        @PostMapping("/{id}/eliminar")
        public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
                cursoService.eliminar(id);
                ra.addFlashAttribute("ok", "Curso eliminado correctamente");
                return "redirect:/cursos";
        }

        // =====================================================================
        // CLASE 6 - PARTE E (BONUS): subir imagen del curso a Firebase Storage
        // =====================================================================

        // Cuando hagas el bonus de Firebase:
        // 1. Crea el archivo service/FirebaseService.java (mira referencia en
        // Recursos_Profesor/_referencia/FirebaseService.java.md)
        // 2. Descomenta el @Autowired private FirebaseService firebaseService arriba.
        // 3. Reemplaza los @PostMapping("/" y "/{id}") por las versiones de abajo
        // que aceptan un MultipartFile y suben la imagen antes de guardar.
        /*
         * @PostMapping
         * public String guardarConImagen(@Valid @ModelAttribute("curso") Curso curso,
         * BindingResult result,
         * 
         * @RequestParam(value = "imagen", required = false) MultipartFile imagen,
         * RedirectAttributes ra) {
         * if (result.hasErrors()) {
         * return "cursos/form";
         * }
         * if (imagen != null && !imagen.isEmpty() && firebaseService.isActivo()) {
         * try {
         * String url = firebaseService.subir(imagen);
         * curso.setImagenUrl(url);
         * } catch (Exception e) {
         * ra.addFlashAttribute("error", "No se pudo subir la imagen: " +
         * e.getMessage());
         * }
         * }
         * cursoService.guardar(curso);
         * ra.addFlashAttribute("ok", "Curso guardado correctamente");
         * return "redirect:/cursos";
         * }
         */
}
