package com.homefixer.kafkaadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** DTO de entrada (validados con Bean Validation) y de salida del administrador Kafka. */
public final class AdminDtos {

    private AdminDtos() {
    }

    // ---------------- Entrada ----------------

    public record CrearTopicoRequest(
            @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{1,249}", message = "nombre: solo letras, números, '.', '_' y '-' (máx. 249)")
            String nombre,
            @NotNull @Min(value = 1, message = "particiones debe ser >= 1") @Max(value = 50, message = "particiones debe ser <= 50")
            Integer particiones,
            @NotNull @Min(value = 1, message = "replicas debe ser >= 1") @Max(value = 3, message = "replicas no puede superar los 3 brokers")
            Short replicas,
            Map<String, String> configs) {
    }

    /** {@code set}: claves a fijar; {@code eliminar}: claves que vuelven al valor por defecto del broker. */
    public record ActualizarConfigRequest(Map<String, String> set, List<String> eliminar) {
    }

    public record AmpliarParticionesRequest(
            @NotNull @Min(value = 2, message = "total debe ser >= 2") @Max(value = 50, message = "total debe ser <= 50")
            Integer total) {
    }

    public enum EstrategiaReset { EARLIEST, LATEST, TIMESTAMP, OFFSET }

    /**
     * Reset de offsets de un consumer group (el grupo debe estar sin consumidores activos).
     * {@code topicos} vacío = todos los tópicos con offsets del grupo. TIMESTAMP usa {@code timestamp} (ISO-8601);
     * OFFSET usa {@code offset} (se ajusta al rango válido de cada partición).
     */
    public record ResetOffsetsRequest(
            @NotNull EstrategiaReset estrategia,
            List<String> topicos,
            Instant timestamp,
            @Min(0) Long offset) {
    }

    public record EliminarGruposRequest(@NotEmpty List<String> grupos) {
    }

    // ---------------- Salida ----------------

    public record BrokerInfo(int id, String host, int puerto, String rack, boolean controller) {
    }

    public record ClusterInfo(String clusterId, int controllerId, List<BrokerInfo> brokers) {
    }

    public record ParticionInfo(int particion, Integer lider, List<Integer> replicas, List<Integer> isr,
                                boolean subReplicada, long offsetInicial, long offsetFinal, long mensajes) {
    }

    public record TopicoResumen(String nombre, int particiones, int replicas, boolean interno, boolean protegido,
                                boolean subReplicado) {
    }

    public record TopicoDetalle(String nombre, String topicId, boolean interno, boolean protegido, int particiones,
                                int replicas, long mensajes, List<ParticionInfo> detalleParticiones,
                                Map<String, ConfigValor> configs) {
    }

    public record ConfigValor(String valor, String origen, boolean soloLectura) {
    }

    public record GrupoResumen(String grupo, String estado, boolean simple) {
    }

    public record MiembroGrupo(String consumerId, String clientId, String host, List<String> particiones) {
    }

    public record LagParticion(String topico, int particion, Long offsetComprometido, long offsetFinal, Long lag,
                               String consumerId) {
    }

    public record GrupoDetalle(String grupo, String estado, String asignador, Integer coordinador,
                               List<MiembroGrupo> miembros, long lagTotal, Map<String, Long> lagPorTopico,
                               List<LagParticion> particiones) {
    }

    public record ResultadoReset(String grupo, EstrategiaReset estrategia, List<LagParticion> nuevosOffsets) {
    }

    public record MensajeDlt(String topico, int particion, long offset, String key, String valor,
                             String topicoOriginal, String particionOriginal, String offsetOriginal,
                             String grupoOriginal, String excepcion, String mensajeError, Instant recibidoEn) {
    }

    public record ResultadoSimulacion(int eventosEnviados, String topico, String mensaje) {
    }
}
