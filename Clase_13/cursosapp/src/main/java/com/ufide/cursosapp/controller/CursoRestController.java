package com.ufide.cursosapp.controller;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
// Mismo dominio (Curso), mismos datos, dos formas distintas de exponerlos:
// HTML para navegador (CursoController) y JSON para cualquier cliente
// (esta clase) - conviven en el mismo proyecto sin pisarse.
@RestController
@RequestMapping("/api/cursos")
public class CursoRestController {

    @Autowired
    private CursoService cursoService;

    // GET /api/cursos -> 200 OK + JSON con la lista de cursos.
    // Usamos listarConProfesor() (JOIN FETCH, de S9) y no listar() a proposito:
    // profesor es LAZY, y si Jackson intenta serializar un proxy LAZY sin
    // resolver fuera de una transaccion activa, tira error. JOIN FETCH ya
    // trae el profesor real, sin proxy, listo para convertir a JSON.
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
    // El body esperado (JSON):
    // { "nombre": "...", "descripcion": "...", "creditos": 4, "profesor": { "id": 1 } }
    // Alcanza con mandar el id del profesor - Hibernate no necesita el resto
    // de sus datos para guardar la relacion (no hay cascade configurado).
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
    @PutMapping("/{id}")
    public ResponseEntity<Curso> actualizar(@PathVariable Long id, @Valid @RequestBody Curso curso) {
        if (cursoService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        curso.setId(id);
        return ResponseEntity.ok(cursoService.guardar(curso));
    }

    // DELETE /api/cursos/{id} -> 204 No Content si se borro, 404 si no existia.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (cursoService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        cursoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
