package org.mq.component;

import org.mq.configuration.RabbitMQConfiguration;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ConsumidorComponent {

    @RabbitListener(queues = RabbitMQConfiguration.QUEUE)
    public void Procesar(String mensaje) {
        System.out.println("Consumiendo: " + mensaje);
    }
}