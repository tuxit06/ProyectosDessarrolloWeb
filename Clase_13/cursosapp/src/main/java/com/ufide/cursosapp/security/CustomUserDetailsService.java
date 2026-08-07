package com.ufide.cursosapp.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ufide.cursosapp.entity.Usuario;
import com.ufide.cursosapp.repository.UsuarioRepository;

// Spring Security necesita saber COMO buscar un usuario y armar su UserDetails.
// Esta clase es el puente entre nuestra tabla "usuarios" y lo que Security entiende.
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "No existe un usuario con username: " + username));

        // Spring Security espera el rol prefijado con "ROLE_" para que
        // hasRole("ADMIN") funcione en S11. Si solo tuvieramos hasAuthority(),
        // no haria falta el prefijo - ver bibliografia_clase11.md.
        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword())
                .roles(usuario.getRol())
                .build();
    }
}
