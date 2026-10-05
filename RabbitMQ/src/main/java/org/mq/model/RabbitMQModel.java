package org.mq.model;

import org.mq.configuration.RabbitMQConfiguration;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class RabbitMQModel {

    private final RabbitTemplate rabbitTemplate;

    public void Mensaje(String mensaje) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfiguration.EXCHANGE,
                RabbitMQConfiguration.ROUTING_KEY,
                mensaje
        );

        System.out.println("Envio: " + mensaje);
    }
}