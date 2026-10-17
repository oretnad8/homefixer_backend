package com.homefixer.shared.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;

import java.util.ArrayList;
import java.util.List;

/**
 * Declara la cola de un suscriptor de homefixer.events con la misma política en todos los servicios:
 * <ul>
 *   <li>Durabilidad: exchanges y colas durables.</li>
 *   <li>DLX: rechazos (nack sin requeue), expirados y desbordes van a homefixer.dlx -> {@code dlq}.</li>
 *   <li>Retención: TTL 24 h y máx. 10.000 mensajes en la cola; la DLQ guarda 7 días / 50.000.</li>
 * </ul>
 */
public final class EventQueues {

    private EventQueues() {
    }

    public static TopicExchange eventsExchange() {
        return ExchangeBuilder.topicExchange(MessagingConstants.EVENTS_EXCHANGE).durable(true).build();
    }

    public static DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(MessagingConstants.DLX_EXCHANGE).durable(true).build();
    }

    public static DirectExchange rpcExchange() {
        return ExchangeBuilder.directExchange(MessagingConstants.RPC_EXCHANGE).durable(true).build();
    }

    /** Cola de suscriptor + su DLQ + bindings a cada routing key indicada. */
    public static Declarables suscriptor(String cola, String dlq, String... routingKeys) {
        TopicExchange events = eventsExchange();
        DirectExchange dlx = deadLetterExchange();

        Queue principal = QueueBuilder.durable(cola)
                .deadLetterExchange(MessagingConstants.DLX_EXCHANGE)
                .deadLetterRoutingKey(dlq)
                .ttl(MessagingConstants.MAIN_QUEUE_TTL_MS)
                .maxLength(MessagingConstants.MAIN_QUEUE_MAX_LENGTH)
                .withArgument("x-overflow", "reject-publish-dlx")
                .build();
        Queue muerta = QueueBuilder.durable(dlq)
                .ttl(MessagingConstants.DLQ_TTL_MS)
                .maxLength(MessagingConstants.DLQ_MAX_LENGTH)
                .build();

        List<Declarable> declarables = new ArrayList<>(List.of(events, dlx, principal, muerta,
                BindingBuilder.bind(muerta).to(dlx).with(dlq)));
        for (String routingKey : routingKeys) {
            Binding binding = BindingBuilder.bind(principal).to(events).with(routingKey);
            declarables.add(binding);
        }
        return new Declarables(declarables);
    }

    /** Cola RPC (request/reply): durable y con TTL corto, sin DLQ (una petición vencida no se reprocesa). */
    public static Declarables colaRpc(String cola, String routingKey) {
        DirectExchange rpc = rpcExchange();
        Queue queue = QueueBuilder.durable(cola).ttl(MessagingConstants.RPC_QUEUE_TTL_MS).build();
        return new Declarables(rpc, queue, BindingBuilder.bind(queue).to(rpc).with(routingKey));
    }
}
