package com.ufide.cursosapp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// Envoltorio simple sobre JavaMailSender, que Spring Boot autoconfigura solo
// a partir de las propiedades spring.mail.* en application.properties (no
// hace falta un @Bean manual - por eso alcanza con @Autowired directo).
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Un solo metodo: mandar el link de recuperacion de contrasena. En un
    // sistema real se usarian plantillas HTML (Thymeleaf tiene soporte para
    // esto), pero para el objetivo de la clase alcanza con texto plano.
    public void enviarLinkRecuperacion(String destinatario, String enlace) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setTo(destinatario);
        mensaje.setSubject("SC-403 - Recuperar contrasena");
        mensaje.setText(
                "Recibimos una solicitud para restablecer tu contrasena.\n\n" +
                "Hace click en el siguiente enlace (valido por 30 minutos):\n" +
                enlace + "\n\n" +
                "Si no pediste este cambio, ignora este correo - tu contrasena " +
                "actual sigue funcionando normalmente."
        );
        mailSender.send(mensaje);
    }
}
