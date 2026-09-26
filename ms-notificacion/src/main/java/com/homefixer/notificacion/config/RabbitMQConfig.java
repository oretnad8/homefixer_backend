package com.homefixer.notificacion.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "homefixer.exchange";
    public static final String NOTIFICACION_QUEUE = "notificacion.queue";
    public static final String NOTIFICACION_ROUTING_KEY = "notificacion.routing.key";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue notificacionQueue() {
        return new Queue(NOTIFICACION_QUEUE, true);
    }

    @Bean
    public Binding notificacionBinding(Queue notificacionQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificacionQueue).to(exchange).with(NOTIFICACION_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
