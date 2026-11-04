package com.homefixer.shared.kafka;

import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.DeserializationException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejo de errores común a los consumidores Kafka. Cada servicio lo registra como bean
 * ({@code CommonErrorHandler}) y Spring Boot lo aplica a todos sus {@code @KafkaListener}:
 * <ol>
 *   <li>Error transitorio: 3 reintentos con backoff exponencial (1 s, 2 s, 4 s), registrados como [KAFKA][RETRY].</li>
 *   <li>Payload ilegible ({@link DeserializationException}) o inválido ({@link EventoInvalidoException}):
 *       no se reintenta.</li>
 *   <li>Agotados los reintentos: el mensaje va a {@code <topic>.DLT} en la misma partición, con los headers
 *       kafka_dlt-* (excepción, offset original…), y se hace commit del offset: el consumidor sigue con el
 *       siguiente mensaje sin bloquear la partición.</li>
 * </ol>
 * Con ErrorHandlingDeserializer, un mensaje ilegible conserva sus bytes originales; se publican al DLT con un
 * template de byte[] para no alterarlos.
 */
public final class KafkaErrorHandling {

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorHandling.class);

    private KafkaErrorHandling() {
    }

    public static DefaultErrorHandler errorHandler(ProducerFactory<?, ?> producerFactory) {
        Map<String, Object> props = producerFactory.getConfigurationProperties();
        KafkaTemplate<String, byte[]> bytesTemplate = new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new ByteArraySerializer()));
        @SuppressWarnings("unchecked")
        KafkaTemplate<Object, Object> jsonTemplate = new KafkaTemplate<>((ProducerFactory<Object, Object>) producerFactory);

        Map<Class<?>, KafkaOperations<?, ?>> templates = new LinkedHashMap<>();
        templates.put(byte[].class, bytesTemplate);
        templates.put(Object.class, jsonTemplate);

        DeadLetterPublishingRecoverer dlt = new DeadLetterPublishingRecoverer(templates,
                (record, ex) -> new TopicPartition(record.topic() + KafkaTopics.DLT_SUFFIX, record.partition()));

        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(4000L);

        DefaultErrorHandler handler = new DefaultErrorHandler((record, ex) -> {
            log.error("[KAFKA][DLT] Enviando a {}{} topic={} partition={} offset={} key={} causa={}",
                    record.topic(), KafkaTopics.DLT_SUFFIX, record.topic(), record.partition(), record.offset(),
                    record.key(), causaRaiz(ex).toString());
            dlt.accept(record, ex);
        }, backOff);
        handler.addNotRetryableExceptions(EventoInvalidoException.class, DeserializationException.class,
                ClassCastException.class, IllegalArgumentException.class);
        handler.setRetryListeners((record, ex, intento) -> log.warn(
                "[KAFKA][RETRY] intento={} topic={} partition={} offset={} key={} error={}",
                intento, record.topic(), record.partition(), record.offset(), record.key(), causaRaiz(ex).getMessage()));
        return handler;
    }

    private static Throwable causaRaiz(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        return t;
    }
}
