#!/bin/bash
# Tópicos de HomeFixer. Se ejecuta dentro del contenedor kafka-init (docker-compose.kafka.yml).
# Idempotente: --if-not-exists permite relanzarlo sin errores.
set -e

BS="kafka1:29092,kafka2:29092,kafka3:29092"

create() {
  kafka-topics --bootstrap-server "$BS" --create --if-not-exists "$@"
}

echo ">> Creando tópicos HomeFixer..."

# 1) Eventos de negocio del ciclo de vida de una solicitud (key = solicitudId → orden por solicitud).
#    Historia que no se puede perder y que se relee (auditoría / replay de métricas): delete, 7 días.
create --topic homefixer.solicitudes.eventos --partitions 3 --replication-factor 3 \
  --config cleanup.policy=delete \
  --config retention.ms=604800000 \
  --config min.insync.replicas=2 \
  --config compression.type=lz4 \
  --config max.message.bytes=1048576

# 2) Posición GPS de técnicos, flujo continuo (key = tecnicoId).
#    Solo importa la última posición por técnico: compact, y lo antiguo se borra a las 24 h.
create --topic homefixer.tecnicos.ubicacion --partitions 3 --replication-factor 3 \
  --config cleanup.policy=compact,delete \
  --config retention.ms=86400000 \
  --config min.insync.replicas=2 \
  --config segment.ms=3600000 \
  --config min.cleanable.dirty.ratio=0.1 \
  --config min.compaction.lag.ms=60000

# 3) Dead Letter Topics: mismas 3 particiones (el recoverer conserva la partición de origen),
#    retención de 14 días para analizar los mensajes fallidos.
create --topic homefixer.solicitudes.eventos.DLT --partitions 3 --replication-factor 3 \
  --config cleanup.policy=delete --config retention.ms=1209600000 --config min.insync.replicas=2
create --topic homefixer.tecnicos.ubicacion.DLT --partitions 3 --replication-factor 3 \
  --config cleanup.policy=delete --config retention.ms=1209600000 --config min.insync.replicas=2

echo ">> Tópicos creados:"
kafka-topics --bootstrap-server "$BS" --describe --exclude-internal
