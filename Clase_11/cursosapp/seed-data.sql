-- ============================================================
-- Seed data - cursosapp (S11: Spring Security - @PreAuthorize + roles + 403)
-- ============================================================
-- Mismos datos que S10 - la tabla usuarios ya tiene el campo rol listo,
-- en esta clase se usa de verdad con @PreAuthorize.
-- ============================================================
-- Ejecutar en MySQL Workbench DESPUES de:
--   1) Haber creado la BD cursoswebdb
--   2) Haber arrancado la app con las entidades Curso, Profesor y Usuario
--      (Hibernate crea las tablas "profesores", "cursos" y "usuarios"
--       automaticamente)
-- ============================================================

USE cursoswebdb;

-- Si vienen de una version anterior, limpiar todo:
-- SET FOREIGN_KEY_CHECKS = 0;
-- TRUNCATE TABLE cursos;
-- TRUNCATE TABLE profesores;
-- TRUNCATE TABLE usuarios;
-- SET FOREIGN_KEY_CHECKS = 1;

-- 1) Insertar profesores primero (Curso depende de Profesor)
INSERT INTO profesores (nombre, email, especialidad) VALUES
('Esteban Ortega', 'esteban.ortega@ufide.ac.cr', 'Desarrollo Web y Patrones'),
('Maria Elena Vargas', 'maria.vargas@ufide.ac.cr', 'Bases de Datos'),
('Carlos Solano', 'carlos.solano@ufide.ac.cr', 'Seguridad Informatica');

-- Verificar
SELECT * FROM profesores;

-- 2) Insertar cursos referenciando el id del profesor (profesor_id)
INSERT INTO cursos (nombre, descripcion, creditos, profesor_id) VALUES
('Fundamentos Web',
 'Introduccion a HTML5, CSS3 y arquitectura cliente/servidor.',
 3, 1),

('Spring Boot',
 'Backend con Java 25, MVC, Thymeleaf y Bootstrap.',
 4, 1),

('Bases de Datos',
 'MySQL, Workbench, JPA, Hibernate y modelo relacional.',
 4, 2),

('Patrones de Diseno',
 'MVC, Repository, Service, Inyeccion de Dependencias.',
 3, 1),

('Seguridad Web',
 'Autenticacion, autorizacion y buenas practicas con Spring Security.',
 4, 3),

('APIs REST',
 'Servicios JSON, consumo desde clientes, despliegue en la nube.',
 3, 1);

-- Verificar
SELECT c.id, c.nombre, c.creditos, p.nombre AS profesor
FROM cursos c
JOIN profesores p ON p.id = c.profesor_id;

-- 3) Insertar usuarios de prueba.
-- IMPORTANTE: estos hashes ya vienen procesados con BCrypt (cost 10).
-- NUNCA insertar la contrasena en texto plano.
-- Contrasena real de cada uno (solo para el lab):
--   admin      -> admin123
--   profesor   -> profesor123
--   estudiante -> estudiante123
INSERT INTO usuarios (username, password, rol) VALUES
('admin', '$2b$10$Qj/x8JpJMw7eOP7tPSqyRO9.IqRQQSyDWMQUUy/1mI0IJTPy5eSIq', 'ADMIN'),
('profesor', '$2b$10$aCb2CqrheWlQh7h0KM9/3etUtNdfqWgzByyBjCtOAsIIS0SmBlkXe', 'USER'),
('estudiante', '$2b$10$2Ki/2UdNHxrBaMkKF/ITVOmVvsFxtvh1mVKH1Udma5InIWb7NdsKC', 'USER');

-- Verificar (el password se ve como hash, eso es lo esperado)
SELECT id, username, rol FROM usuarios;

-- ============================================================
-- CLASE 11 - PASO F.1 (continua): despues de descomentar el campo "email"
-- en Usuario.java y reiniciar la app (Hibernate agrega la columna sola con
-- ddl-auto=update), completa los emails de los 3 usuarios de arriba:
-- UPDATE usuarios SET email = 'admin@ufide.ac.cr' WHERE username = 'admin';
-- UPDATE usuarios SET email = 'profesor@ufide.ac.cr' WHERE username = 'profesor';
-- UPDATE usuarios SET email = 'estudiante@ufide.ac.cr' WHERE username = 'estudiante';
--
-- Verificar (descomentar junto con los UPDATE de arriba):
-- SELECT id, username, rol, email FROM usuarios;
-- ============================================================

-- ============================================================
-- Queries utiles para demostrar en clase (JPQL / SQL)
-- ============================================================

-- Ver el SQL real que genera el JOIN FETCH del CursoRepository:
-- (activar spring.jpa.show-sql=true y comparar contra esto)
-- SELECT c.*, p.* FROM cursos c JOIN profesores p ON p.id = c.profesor_id;

-- Cursos de un profesor especifico
-- SELECT * FROM cursos WHERE profesor_id = 1;

-- Contar cursos por profesor
-- SELECT p.nombre, COUNT(c.id) AS total_cursos
-- FROM profesores p LEFT JOIN cursos c ON c.profesor_id = p.id
-- GROUP BY p.nombre;

-- Borrar un curso (para demostrar deleteById)
-- DELETE FROM cursos WHERE id = 6;

-- Ver el rol de un usuario especifico
-- SELECT username, rol FROM usuarios WHERE username = 'admin';