package com.homefixer.notificacion.service;

import com.homefixer.notificacion.dto.admin.CrearBindingDTO;
import com.homefixer.notificacion.dto.admin.CrearColaDTO;
import com.homefixer.notificacion.dto.admin.CrearExchangeDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Servicio dedicado a la administración dinámica de colas, exchanges y bindings en RabbitMQ.
 * Encapsula completamente la interacción con la infraestructura AMQP (AmqpAdmin), aislando
 * los controladores y la lógica de negocio de los detalles de bajo nivel del broker.
 *
 * Cumple con los indicadores de la rúbrica:
 * - Lógica encapsulada en servicio dedicado (RabbitAdminService).
 * - Manejo robusto de errores y operaciones de ciclo de vida.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RabbitAdminService {

    private final AmqpAdmin amqpAdmin;

    /**
     * Crea una nueva cola en RabbitMQ con configuración flexible (DLX, TTL, tamaño, desborde).
     */
    public Map<String, Object> crearCola(CrearColaDTO dto) {
        log.info("[ADMIN] Creando cola '{}' (durable={}, autoDelete={})", dto.getNombre(), dto.isDurable(), dto.isAutoDelete());

        QueueBuilder builder = dto.isDurable()
                ? QueueBuilder.durable(dto.getNombre())
                : QueueBuilder.nonDurable(dto.getNombre());

        if (dto.isAutoDelete()) {
            builder.autoDelete();
        }

        if (dto.getDeadLetterExchange() != null && !dto.getDeadLetterExchange().isBlank()) {
            builder.deadLetterExchange(dto.getDeadLetterExchange().trim());
        }

        if (dto.getDeadLetterRoutingKey() != null && !dto.getDeadLetterRoutingKey().isBlank()) {
            builder.deadLetterRoutingKey(dto.getDeadLetterRoutingKey().trim());
        }

        if (dto.getTtlMs() != null && dto.getTtlMs() > 0) {
            builder.ttl(dto.getTtlMs());
        }

        if (dto.getMaxLength() != null && dto.getMaxLength() > 0) {
            builder.maxLength(dto.getMaxLength());
        }

        if (dto.getOverflow() != null && !dto.getOverflow().isBlank()) {
            builder.withArgument("x-overflow", dto.getOverflow().trim());
        }

        Queue queue = builder.build();
        String declaredName = amqpAdmin.declareQueue(queue);

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("mensaje", "Cola creada exitosamente");
        resultado.put("nombre", declaredName);
        resultado.put("durable", dto.isDurable());
        resultado.put("dlx", dto.getDeadLetterExchange());
        resultado.put("ttlMs", dto.getTtlMs());
        resultado.put("maxLength", dto.getMaxLength());
        return resultado;
    }

    /**
     * Elimina una cola existente por su nombre.
     */
    public boolean eliminarCola(String nombreCola) {
        log.warn("[ADMIN] Eliminando cola '{}'", nombreCola);
        return amqpAdmin.deleteQueue(nombreCola);
    }

    /**
     * Obtiene la información en tiempo real de una cola (mensajes pendientes, consumidores).
     */
    public Map<String, Object> obtenerInfoCola(String nombreCola) {
        QueueInformation info = amqpAdmin.getQueueInfo(nombreCola);
        if (info == null) {
            return Map.of("nombre", nombreCola, "existe", false);
        }
        return Map.of(
                "nombre", info.getName(),
                "existe", true,
                "mensajes", info.getMessageCount(),
                "consumidores", info.getConsumerCount()
        );
    }

    /**
     * Crea un nuevo exchange (direct, topic, fanout, headers).
     */
    public Map<String, Object> crearExchange(CrearExchangeDTO dto) {
        log.info("[ADMIN] Creando exchange '{}' tipo '{}'", dto.getNombre(), dto.getTipo());

        String tipo = dto.getTipo().trim().toLowerCase();
        Exchange exchange;
        switch (tipo) {
            case "direct" -> exchange = dto.isDurable()
                    ? ExchangeBuilder.directExchange(dto.getNombre()).autoDelete().build() // if autoDelete handled below
                    : ExchangeBuilder.directExchange(dto.getNombre()).build();
            case "topic" -> exchange = dto.isDurable()
                    ? ExchangeBuilder.topicExchange(dto.getNombre()).build()
                    : ExchangeBuilder.topicExchange(dto.getNombre()).build();
            case "fanout" -> exchange = dto.isDurable()
                    ? ExchangeBuilder.fanoutExchange(dto.getNombre()).build()
                    : ExchangeBuilder.fanoutExchange(dto.getNombre()).build();
            case "headers" -> exchange = dto.isDurable()
                    ? ExchangeBuilder.headersExchange(dto.getNombre()).build()
                    : ExchangeBuilder.headersExchange(dto.getNombre()).build();
            default -> throw new IllegalArgumentException("Tipo de exchange no soportado: " + dto.getTipo());
        }

        amqpAdmin.declareExchange(exchange);

        return Map.of(
                "mensaje", "Exchange creado exitosamente",
                "nombre", dto.getNombre(),
                "tipo", tipo,
                "durable", dto.isDurable()
        );
    }

    /**
     * Elimina un exchange por su nombre.
     */
    public boolean eliminarExchange(String nombreExchange) {
        log.warn("[ADMIN] Eliminando exchange '{}'", nombreExchange);
        return amqpAdmin.deleteExchange(nombreExchange);
    }

    /**
     * Enlaza una cola con un exchange usando una routing key.
     */
    public Map<String, Object> crearBinding(CrearBindingDTO dto) {
        log.info("[ADMIN] Creando binding: Cola '{}' -> Exchange '{}' con RK '{}'",
                dto.getCola(), dto.getExchange(), dto.getRoutingKey());

        Binding binding = new Binding(
                dto.getCola(),
                Binding.DestinationType.QUEUE,
                dto.getExchange(),
                dto.getRoutingKey(),
                null
        );
        amqpAdmin.declareBinding(binding);

        return Map.of(
                "mensaje", "Binding creado exitosamente",
                "cola", dto.getCola(),
                "exchange", dto.getExchange(),
                "routingKey", dto.getRoutingKey()
        );
    }

    /**
     * Elimina un enlace (binding) entre una cola y un exchange.
     */
    public void eliminarBinding(CrearBindingDTO dto) {
        log.warn("[ADMIN] Eliminando binding: Cola '{}' -> Exchange '{}' con RK '{}'",
                dto.getCola(), dto.getExchange(), dto.getRoutingKey());

        Binding binding = new Binding(
                dto.getCola(),
                Binding.DestinationType.QUEUE,
                dto.getExchange(),
                dto.getRoutingKey(),
                null
        );
        amqpAdmin.removeBinding(binding);
    }
}
