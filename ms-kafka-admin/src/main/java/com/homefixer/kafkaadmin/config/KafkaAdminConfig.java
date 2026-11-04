package com.homefixer.kafkaadmin.config;

import com.homefixer.shared.kafka.KafkaEventPublisher;
import com.homefixer.shared.kafka.KafkaTopics;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Infraestructura del administrador:
 * <ul>
 *   <li>{@link AdminClient} creado desde la configuración de {@link KafkaAdmin} (spring.kafka.admin.*).</li>
 *   <li>Tópicos del caso declarados como código ({@link NewTopic}): KafkaAdmin los crea al arrancar si
 *       faltan, con la misma configuración que kafka/create-topics.sh (despliegue replicable).</li>
 * </ul>
 */
@Configuration
public class KafkaAdminConfig {

    private static final int PARTICIONES = 3;
    private static final int REPLICAS = 3;
    private static final String SIETE_DIAS_MS = "604800000";
    private static final String UN_DIA_MS = "86400000";
    private static final String CATORCE_DIAS_MS = "1209600000";

    @Bean(destroyMethod = "close")
    public AdminClient adminClient(KafkaAdmin kafkaAdmin) {
        return AdminClient.create(kafkaAdmin.getConfigurationProperties());
    }

    @Bean
    public KafkaEventPublisher kafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                                   @Value("${spring.application.name}") String aplicacion) {
        return new KafkaEventPublisher(kafkaTemplate, aplicacion);
    }

    @Bean
    public KafkaAdmin.NewTopics topicosHomeFixer() {
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name(KafkaTopics.SOLICITUDES_EVENTOS)
                        .partitions(PARTICIONES).replicas(REPLICAS)
                        .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                        .config(TopicConfig.RETENTION_MS_CONFIG, SIETE_DIAS_MS)
                        .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                        .config(TopicConfig.COMPRESSION_TYPE_CONFIG, "lz4")
                        .build(),
                TopicBuilder.name(KafkaTopics.TECNICOS_UBICACION)
                        .partitions(PARTICIONES).replicas(REPLICAS)
                        .config(TopicConfig.CLEANUP_POLICY_CONFIG,
                                TopicConfig.CLEANUP_POLICY_COMPACT + "," + TopicConfig.CLEANUP_POLICY_DELETE)
                        .config(TopicConfig.RETENTION_MS_CONFIG, UN_DIA_MS)
                        .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                        .config(TopicConfig.SEGMENT_MS_CONFIG, "3600000")
                        .config(TopicConfig.MIN_CLEANABLE_DIRTY_RATIO_CONFIG, "0.1")
                        .config(TopicConfig.MIN_COMPACTION_LAG_MS_CONFIG, "60000")
                        .build(),
                dlt(KafkaTopics.SOLICITUDES_EVENTOS_DLT),
                dlt(KafkaTopics.TECNICOS_UBICACION_DLT));
    }

    private static NewTopic dlt(String nombre) {
        return TopicBuilder.name(nombre)
                .partitions(PARTICIONES).replicas(REPLICAS)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, CATORCE_DIAS_MS)
                .config(TopicConfig.MIN_IN_SYNC_REPLICAS_CONFIG, "2")
                .build();
    }
}
