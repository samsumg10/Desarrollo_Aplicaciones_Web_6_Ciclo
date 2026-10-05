package org.mq.controller;

import org.mq.model.RabbitMQModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/envio")
public class ProductorController {

    private final RabbitMQModel rabbitMQModel;

    @GetMapping("/rabbit")
    public String Envio(@RequestParam String mensaje) {
    	rabbitMQModel.Mensaje(mensaje);
        return "Mensaje Procesado";
    }
    
    @GetMapping
    public String Aviso() {
    	return "Aviso";
    }
}