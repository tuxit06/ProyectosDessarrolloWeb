package com.ufide.cursosapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ufide.cursosapp.entity.Profesor;

public interface ProfesorRepository
        extends JpaRepository<Profesor, Long> {
}
