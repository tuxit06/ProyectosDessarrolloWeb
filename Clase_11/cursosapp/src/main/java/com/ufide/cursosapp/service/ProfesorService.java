package com.ufide.cursosapp.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ufide.cursosapp.entity.Profesor;
import com.ufide.cursosapp.repository.ProfesorRepository;

@Service
public class ProfesorService {

    @Autowired
    private ProfesorRepository repo;

    public List<Profesor> listar() {
        return repo.findAll();
    }
}
