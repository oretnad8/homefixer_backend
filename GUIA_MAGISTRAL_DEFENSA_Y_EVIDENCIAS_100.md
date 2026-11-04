# 🛡️ GUÍA MAGISTRAL DE DEFENSA, EVIDENCIAS Y CUMPLIMIENTO AL 100%
## Evaluación Parcial N° 3 (Encargo) & Evaluación Parcial N° 4 (Presentación)
**Asignatura:** Desarrollo Cloud Native I (DSY1107)  
**Proyecto:** HomeFixer — Ecosistema de Microservicios Cloud Native  
**Ponderación Total:** EP3 (12%) + EP4 (18%) = 30% de la Asignatura  

---

## 📌 Resumen Ejecutivo de la Estrategia de Evaluación

Esta guía consolida **cada línea de código, endpoint, configuración y comando** necesarios para defender el proyecto HomeFixer ante la comisión docente, garantizando la calificación máxima (**Muy Buen Desempeño - 100%**) en cada indicador de la rúbrica oficial y respondiendo con precisión quirúrgica a las exigencias verbales del profesor:
1. **Mensajes Síncronos y Asíncronos**
2. **ACKs respectivos** (Publisher Confirms + Consumer Manual ACK)
3. **Durabilidad** (Exchanges, Colas, Mensajes persistentes y volúmenes)
4. **Publish / Subscribe** (Topic Exchange y colas desacopladas)
5. **Dead Letter Exchange (DLX) y Dead Letter Queues (DLQ)**
6. **Políticas de Retención** (TTL, límites de tamaño, desborde)
7. **Alertas operacionales** (Monitoreo proactivo, logs y alarmas del broker)
8. **Administración REST de RabbitMQ** (`RabbitAdminService` con validación DTO)
9. **Clúster de Alta Disponibilidad** (2 nodos RabbitMQ)
10. **Seguridad IDaaS & API Gateway** (OAuth 2.0 / OIDC / JWT con Supabase)

---

## 📊 1. Matriz de Trazabilidad Rúbrica 100% (Indicador por Indicador)

| Indicador de Rúbrica | Ponderación | Nivel Esperado (100%) | Dónde se Evidencia en Código | Cómo Demostrarlo en Vivo |
|---|:---:|---|---|---|
| **1. Configuración Centralizada** | **12%** | Todos los nombres de colas, exchanges y bindings definidos de forma centralizada en clase `@Configuration` o `application.yml`. Sin valores sueltos en código. | [`shared/.../MessagingConstants.java`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/shared/src/main/java/com/homefixer/shared/messaging/MessagingConstants.java)<br/>[`shared/.../EventQueues.java`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/shared/src/main/java/com/homefixer/shared/messaging/EventQueues.java) | Abrir `MessagingConstants.java` y mostrar constantes públicas `final` utilizadas por los 10 microservicios a través del módulo compartido. |
| **2. Declaración de Beans RabbitMQ** | **13%** | Clases de configuración que declaran beans de `Queue`, `Exchange` y `Binding` para cada caso de uso del sistema. | [`ms-solicitudes/.../RabbitMQConfig.java`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/ms-solicitudes/src/main/java/com/homefixer/solicitudes/config/RabbitMQConfig.java)<br/>[`ms-notificacion/.../RabbitMQConfig.java`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/ms-notificacion/src/main/java/com/homefixer/notificacion/config/RabbitMQConfig.java)<br/>`ms-pagos`, `ms-usuarios`, `ms-reportes` | Mostrar `@Bean public Declarables cola...()` en los archivos de configuración y la consola web `http://localhost:15672/#/queues` con los bindings creados. |
| **3. Aislamiento Lógica de Negocio vs Mensajería** | **10%** | Configuración completamente aislada en clases dedicadas. Servicios de negocio solo consumen interfaces/componentes ya configurados. Cero configuración en controladores/dominio. | [`shared/.../EventPublisher.java`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/shared/src/main/java/com/homefixer/shared/messaging/EventPublisher.java)<br/>[`SolicitudService.java`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/ms-solicitudes/src/main/java/com/homefixer/solicitudes/service/SolicitudService.java) | Mostrar que `SolicitudService` solo inyecta `EventPublisher` y llama a `eventPublisher.publicar(key, dto)`. No hay imports de AMQP en el servicio de dominio. |
| **4. Consumidores `@RabbitListener` por Dominio** | **15%** | Todos los consumidores con `@RabbitListener`, agrupados claramente por dominio funcional y contexto de negocio. | Paquetes `listener/` dedicados:<br/>• [`NotificacionEventListener.java`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/ms-notificacion/src/main/java/com/homefixer/notificacion/listener/NotificacionEventListener.java)<br/>• [`CobroSolicitudListener.java`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/ms-pagos/src/main/java/com/homefixer/pagos/listener/CobroSolicitudListener.java)<br/>• [`ValoracionEventListener.java`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/ms-usuarios/src/main/java/com/homefixer/usuarios/listener/ValoracionEventListener.java)<br/>• [`SolicitudEventListener.java`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/ms-reportes/src/main/java/com/homefixer/reportes/listener/SolicitudEventListener.java) | Mostrar la estructura del proyecto en el árbol de carpetas: cada microservicio tiene su paquete `listener` con su responsabilidad de dominio única. |
| **5. Confirmación de Mensajes (ACK) y Errores** | **20%** | Manejo de confirmación completamente implementado. ACK explícito/manual, rutas diferenciadas entre reintentos, NACK y envío a DLQ. | [`NotificacionEventListener.java#L40-L64`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/ms-notificacion/src/main/java/com/homefixer/notificacion/listener/NotificacionEventListener.java#L40-L64)<br/>[`application.yml`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/ms-notificacion/src/main/resources/application.yml) (`acknowledge-mode: manual`) | Forzar fallo con mensaje `"FORZAR_ERROR"`. Mostrar en log los 3 reintentos con backoff, el `basicNack(requeue=false)` y el mensaje cayendo en la DLQ con `x-death`. |
| **6. Microservicio Administrador con Endpoints REST** | **13%** | Microservicio expone API REST clara y coherente: `POST /queues`, `DELETE /queues/{name}`, etc. Buenas prácticas REST y documentada. | [`RabbitAdminController.java`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/ms-notificacion/src/main/java/com/homefixer/notificacion/controller/RabbitAdminController.java) | Ejecutar `curl -X POST http://localhost:8080/api/admin/queues` para crear una cola y `DELETE http://localhost:8080/api/admin/queues/nombre` para eliminarla. |
| **7. Lógica Encapsulada en Servicio Dedicado** | **10%** | Toda la lógica de creación, actualización y eliminación de colas y exchanges encapsulada en servicio (ej. `RabbitAdminService`). Controladores no conocen AMQP interno. | [`RabbitAdminService.java`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/ms-notificacion/src/main/java/com/homefixer/notificacion/service/RabbitAdminService.java) | Abrir `RabbitAdminService.java` y mostrar que utiliza `AmqpAdmin`, encapsulando constructores de Queue/Exchange y devolviendo DTOs/Maps limpios al controller. |
| **8. Validación de Parámetros de Entrada** | **7%** | La API valida los parámetros de entrada evitando colas vacías o configuraciones inválidas. DTOs con `@Valid`, mensajes de error claros. | [`CrearColaDTO.java`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/ms-notificacion/src/main/java/com/homefixer/notificacion/dto/admin/CrearColaDTO.java)<br/>[`ApiExceptionHandler.java`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/ms-notificacion/src/main/java/com/homefixer/notificacion/controller/ApiExceptionHandler.java) | Enviar `POST` con body `{"nombre": ""}`. Mostrar respuesta **HTTP 400 Bad Request** con JSON: `"Error de validación: El nombre de la cola es obligatorio"`. |
| **TOTAL** | **100%** | **DESEMPEÑO DESTACADO (7.0)** | Todo el código se encuentra implementado, compilando y probado. | Demostración fluida en vivo. |

---

## 🎯 2. Los Requisitos Fundamentales del Profesor Explicados en Profundidad

### 2.1 Mensajes Síncronos
En HomeFixer la comunicación síncrona se utiliza cuando un microservicio **requiere obligatoriamente un dato en tiempo real** para continuar su ejecución:
1. **Request/Reply sobre RabbitMQ (RPC):**
   - **Mecanismo:** El cliente invoca `rabbitTemplate.convertSendAndReceiveAsType(...)` publicando en el exchange direct `homefixer.rpc`. RabbitMQ crea internamente una cola de respuesta efímera (`reply_to`) y asocia un identificador unívoco de correlación (`correlation_id`). El hilo del cliente se bloquea en espera del resultado.
   - **Casos de Uso:**
     - `ms-reportes` solicita resumen a `ms-solicitudes` (`rpc.solicitudes.resumen`).
     - `ms-pagos` solicita detalle a `ms-solicitudes` (`rpc.solicitudes.detalle`).
     - `ms-valoraciones` valida que la solicitud esté `PAGADA` antes de registrar la calificación.
   - **Timeout de Resiliencia:** Se configuró `reply-timeout: 5000` (5 segundos). Si el servidor no responde dentro de este plazo, se genera una entrada `[ALERTA][RPC]` en el log y se devuelve un estado HTTP 503 Service Unavailable, evitando el congelamiento de hilos (thread starvation).
2. **HTTP Síncrono con OpenFeign y Netflix Eureka:**
   - Consultas de baja latencia con balanceo de carga en cliente (ej. `ms-solicitudes` consulta `buscarTecnicosCercanos` en `ms-ubicacion`).

### 2.2 Mensajes Asíncronos
La comunicación asíncrona es el núcleo de la arquitectura reactiva basada en eventos (*Event-Driven Architecture*):
- **Mecanismo:** El microservicio productor ensambla un `NotificacionEventDTO` con UUID único (`eventoId`), marca de tiempo (`fechaEvento`) e identificadores de dominio, y lo envía al exchange `homefixer.events`. La llamada retorna de inmediato (no bloqueante); el productor continúa su flujo sin depender de la disponibilidad o lentitud de los consumidores.
- **Flujo Completo de Negocio:**
  ```mermaid
  sequenceDiagram
      autonumber
      actor T as Técnico
      actor C as Cliente
      participant S as ms-solicitudes
      participant E as RabbitMQ (homefixer.events)
      participant P as ms-pagos
      participant U as ms-usuarios
      participant N as ms-notificacion

      T->>S: Finalizar Servicio
      S->>E: Publica solicitud.estado-actualizado (COMPLETADA)
      par Notificar
          E->>N: Cola notificacion.solicitudes.queue
          N->>C: Alerta in-app: "Servicio completado"
      and Cobrar
          E->>P: Cola pagos.solicitudes.queue
          P->>P: Genera Cobro Pendiente
      end

      C->>P: Pagar Servicio
      P->>E: Publica pago.procesado
      par Notificar
          E->>N: Cola notificacion.solicitudes.queue
          N->>T: Alerta: "Pago recibido"
      and Actualizar Solicitud
          E->>S: Cola solicitudes.pagos.queue
          S->>S: Pasa estado a PAGADA
      end
  ```

### 2.3 ACKs Respectivos (Confirmaciones de Doble Vía)
La fiabilidad se garantiza en ambos extremos del canal:
1. **ACK del Broker al Productor (Publisher Confirms & Returns):**
   - Configurado en `application.yml`: `spring.rabbitmq.publisher-confirm-type: correlated` y `publisher-returns: true`.
   - Implementado en `EventPublisher.rabbitTemplate(...)`: Cada mensaje viaja con `CorrelationData(eventoId)`.
   - Cuando RabbitMQ escribe el mensaje en disco o lo enruta a colas durables, devuelve un **ACK** al productor (`log.info("[CONFIRM] Broker confirmó (ACK)...")`).
   - Si no puede enrutarlo (ej. no hay colas vinculadas), `setMandatory(true)` dispara `ReturnsCallback` registrando `[ALERTA][RETURN]`.
2. **ACK del Consumidor al Broker (Manual Acknowledgement):**
   - Configurado con `spring.rabbitmq.listener.simple.acknowledge-mode: manual` y `prefetch: 10`.
   - Los consumidores no delegan la confirmación al contenedor; la realizan explícitamente:
     - **Éxito:** `channel.basicAck(deliveryTag, false)`: RabbitMQ borra el mensaje de la cola.
     - **Fallo:** Tras agotar reintentos con retroceso lineal (*linear backoff*), ejecuta `channel.basicNack(deliveryTag, false, false)` con `requeue=false`. Esto instruye al broker a enviar el mensaje directamente al DLX sin reencolarlo infinitamente.
3. **Consumer Timeout:** En `rabbitmq.conf`, `consumer_timeout = 1800000` (30 minutos). Si un consumidor se cuelga sin enviar ACK ni NACK, el broker fuerza el cierre del canal y reentrega el mensaje a otro nodo activo.

### 2.4 Durabilidad en Todas las Capas
Para evitar cualquier pérdida de información ante cortes eléctricos o reinicios de contenedores:
1. **Exchanges Durables:** Declarados con `ExchangeBuilder.topicExchange(...).durable(true).build()` (`homefixer.events`, `homefixer.dlx`, `homefixer.rpc`).
2. **Colas Durables:** Declaradas con `QueueBuilder.durable(...)`.
3. **Mensajes Persistentes:** En `EventPublisher.java`, cada mensaje se etiqueta con `MessageDeliveryMode.PERSISTENT` (`delivery_mode = 2`), forzando al motor de RabbitMQ a escribirlos en disco.
4. **Persistencia del Broker en Disco:** En `docker-compose.yml`, el volumen Docker `rabbitmq-data:/var/lib/rabbitmq` se combina con `hostname: homefixer-rabbitmq`, asegurando que el identificador del nodo Erlang coincida entre reinicios y no se pierdan los archivos de índice de mensajes.

### 2.5 Publish / Subscribe
- **Exchange Tipo Topic (`homefixer.events`):** Permite desacoplar totalmente al emisor de los receptores mediante patrones de routing key jerárquicos (`solicitud.#`, `pago.#`, `valoracion.#`, `chat.#`, `certificacion.#`).
- **Colas Propias por Servicio:** Cada microservicio suscriptor crea su propia cola (`notificacion.solicitudes.queue`, `reportes.solicitudes.queue`, `pagos.solicitudes.queue`, `usuarios.valoraciones.queue`, `solicitudes.pagos.queue`).
- **Independencia Operacional:** Un solo evento `solicitud.estado-actualizado` es clonado y entregado a 3 colas distintas simultáneamente. Si `ms-reportes` está detenido por mantenimiento, `ms-pagos` y `ms-notificacion` continúan consumiendo sus copias sin interrupción.

### 2.6 Dead Letter Exchange (DLX) y Dead Letter Queues (DLQ)
- **Topología:** Exchange direct durable `homefixer.dlx`. Cada cola principal tiene una cola hermana `*.dlq` unida por routing key idéntico al nombre de la DLQ.
- **Las 3 Causas de Envío a DLQ (Argumentos en `EventQueues.java`):**
  1. **Rechazo Explícito:** `basicNack(deliveryTag, false, false)` o `basicReject` con `requeue = false`.
  2. **Expiración de Tiempo de Vida (TTL):** El mensaje permaneció 24 horas en la cola sin ser tomado por ningún consumidor.
  3. **Desborde de Capacidad:** Al superar 10.000 mensajes, el argumento `x-overflow: reject-publish-dlx` envía los mensajes excedentes a la DLQ en lugar de descartarlos silenciosamente.
- **Trazabilidad Forense (`x-death`):** La DLQ preserva las cabeceras `x-death` inyectadas por RabbitMQ, indicando la cola original de procedencia, la estampa de tiempo y el motivo del desvío (`rejected`, `expired`, `maxlen`).

### 2.7 Políticas de Retención
Definidas formalmente en `MessagingConstants.java`:
- **Colas Principales de Negocio:**
  - `x-message-ttl = 86.400.000 ms (24 horas)`: Garantiza que mensajes acumulados por caídas prolongadas no queden obsoletos.
  - `x-max-length = 10.000 mensajes`: Protege la memoria del servidor ante picos de tráfico.
- **Colas de Mensajes Muertos (DLQ):**
  - `x-message-ttl = 604.800.000 ms (7 días)`: Ventana de tiempo para que los desarrolladores o administradores analicen, depuren o reprocesen los mensajes fallidos.
  - `x-max-length = 50.000 mensajes`: Con política de descarte de los más antiguos (`drop-head`).
- **Colas RPC (Request/Reply):**
  - `x-message-ttl = 10.000 ms (10 segundos)`: Si una solicitud síncrona vence, se destruye sin ir a DLQ porque el cliente HTTP ya cerró la conexión por timeout.

### 2.8 Alertas Operacionales
1. **Componente `ColasMonitor` (`ms-notificacion`):** Tarea `@Scheduled` periódica cada 60 segundos que inspecciona el estado de todas las colas mediante `AmqpAdmin`:
   - Emite `[ALERTA][DLQ]` cuando cualquier DLQ tiene $\ge 1$ mensajes fallidos.
   - Emite `[ALERTA]` cuando una cola principal supera los 1.000 mensajes (consumidor lento).
   - Emite `[ALERTA]` cuando una cola principal tiene 0 consumidores conectados (microservicio caído).
2. **Endpoint REST de Monitoreo:** `GET /api/notificaciones/mensajeria/colas` expone en JSON el conteo exacto de mensajes y consumidores en tiempo real.
3. **Alarmas de Hardware en `rabbitmq.conf`:**
   - `vm_memory_high_watermark.relative = 0.6`: Si RabbitMQ supera el 60% de la RAM del servidor, bloquea la recepción de productores para evitar una caída por *Out Of Memory*.
   - `disk_free_limit.absolute = 1GB`: Si el disco libre baja de 1 GB, se activa la alarma y se suspende la persistencia temporalmente.

---

## 🏛️ 3. Microservicio Administrador de RabbitMQ (Indicadores 6, 7 y 8)

Para cumplir al 100% con los indicadores 6 (13%), 7 (10%) y 8 (7%) de la rúbrica, se implementó en `ms-notificacion` una capa completa de administración dinámica de infraestructura de mensajería:

### Arquitectura de Clases:
- **Controlador REST:** [`RabbitAdminController.java`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/ms-notificacion/src/main/java/com/homefixer/notificacion/controller/RabbitAdminController.java) (expuesto en `/api/admin` y `/api/notificaciones/admin`).
- **Servicio Dedicado:** [`RabbitAdminService.java`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/ms-notificacion/src/main/java/com/homefixer/notificacion/service/RabbitAdminService.java) (encapsula `AmqpAdmin`).
- **DTOs de Entrada Validados:**
  - [`CrearColaDTO.java`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/ms-notificacion/src/main/java/com/homefixer/notificacion/dto/admin/CrearColaDTO.java): Valida `@NotBlank`, `@Pattern` de nombres seguros, `@PositiveOrZero` en TTL y `@Pattern` en tipo de desborde (`reject-publish-dlx`).
  - [`CrearExchangeDTO.java`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/ms-notificacion/src/main/java/com/homefixer/notificacion/dto/admin/CrearExchangeDTO.java): Valida tipo de exchange (`direct|topic|fanout|headers`).
  - [`CrearBindingDTO.java`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/ms-notificacion/src/main/java/com/homefixer/notificacion/dto/admin/CrearBindingDTO.java): Valida enlaces entre colas y exchanges.
- **Manejador de Errores:** [`ApiExceptionHandler.java`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/ms-notificacion/src/main/java/com/homefixer/notificacion/controller/ApiExceptionHandler.java) captura violaciones de validación y retorna **HTTP 400 Bad Request** con el motivo estructurado.

---

## 🚀 4. Guía Paso a Paso de los 11 Puntos de la Presentación EP4

A continuación se detalla exactamente qué mostrar y explicar para cada uno de los 11 puntos exigidos en las instrucciones de la presentación:

### Punto 1: Instancia de API Manager en funcionamiento
- **Qué mostrar:** Abrir la terminal o navegador mostrando el **API Gateway** corriendo en el puerto 8080 (o la URL de Cloudflare Tunnel si está expuesto a la nube: `cloudflared tunnel --url http://localhost:8080`).
- **Explicación técnica:** *"Contamos con un Spring Cloud Gateway reactivo que actúa como único punto de entrada unificado para el frontend web y móvil, centralizando el enrutamiento, la seguridad CORS y la validación de tokens JWT."*

### Punto 2: Configuración del API Manager hacia endpoints del backend
- **Qué mostrar:** Abrir [`api-gateway/src/main/resources/application.yml`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/api-gateway/src/main/resources/application.yml).
- **Explicación técnica:** Mostrar los bloques `spring.cloud.gateway.routes`. Indicar que los URIs utilizan el prefijo `lb://<microservicio>` (Load Balanced) gracias a la integración con el Service Discovery Netflix Eureka en el puerto 8761.

### Punto 3: Frontend consumiendo endpoints a través del API Gateway
- **Qué mostrar:** Abrir [`fronend-home/lib/api.ts`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/fronend-home/lib/api.ts) y la consola Network del navegador (F12).
- **Explicación técnica:** Mostrar que `baseURL` apunta a `http://localhost:8080/api`. En la pestaña Network, mostrar que todas las peticiones van al puerto 8080 (`/api/solicitudes`, `/api/pagos`, etc.) y no directamente a los puertos internos de los microservicios.

### Punto 4: API Gateway validando JWT (Rechazar inválido / Aceptar correcto)
- **Qué mostrar:**
  1. Enviar una petición sin token o con token alterado:
     ```bash
     curl -i http://localhost:8080/api/solicitudes -H "Authorization: Bearer token_falso"
     ```
     *Resultado:* **HTTP 401 Unauthorized**.
  2. Enviar una petición con token válido obtenido en el login:
     *Resultado:* **HTTP 200 OK**.
- **Explicación técnica:** Mostrar [`SecurityConfig.java`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/api-gateway/src/main/java/com/homefixer/gateway/config/SecurityConfig.java) en el Gateway, donde se valida el `issuer-uri`, la firma criptográfica RSA y el claim de audiencia `aud: authenticated` usando `NimbusReactiveJwtDecoder`.

### Punto 5: Tenant en IDaaS y usuarios registrados
- **Qué mostrar:** Abrir el panel web de **Supabase Auth** (`https://supabase.com/dashboard/project/qglwohwurvsxkxttrddr/auth/users`).
- **Explicación técnica:** Mostrar los usuarios creados (tanto Clientes como Técnicos), el ID del Tenant y el proveedor de identidad configurado en la nube AWS Oregon.

### Punto 6: Frontend utilizando OAuth 2.0 / OpenID Connect para login y obtención de JWT
- **Qué mostrar:** En la aplicación web (`http://localhost:3000`), ingresar credenciales de cliente o técnico. Abrir *Application > Local Storage* o *Cookies* y mostrar el token JWT almacenado (`hf_token`). Inspeccionar el token en `jwt.io` mostrando los claims estándar OIDC (`sub`, `iss`, `aud`, `exp`).

### Punto 7: Backend y Frontend desplegados, activos e integrados
- **Qué mostrar:** Realizar una acción interactiva en el Frontend (ej. crear una solicitud de reparación o navegar por el catálogo de técnicos). Mostrar en los logs de la terminal cómo el Gateway rutea la petición y el microservicio correspondiente responde en tiempo real con datos de la base de datos cloud PostgreSQL en Supabase.

### Punto 8: Existencia de dos nodos RabbitMQ en clúster
- **Qué mostrar:**
  1. Mostrar el archivo [`docker-compose.cluster.yml`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/docker-compose.cluster.yml) y [`rabbitmq/rabbitmq-cluster.conf`](file:///c:/Users/Pc%20Tester/Desktop/6to/cloud.native/homefixer/rabbitmq/rabbitmq-cluster.conf).
  2. Levantar el clúster con:
     ```bash
     docker compose -f docker-compose.cluster.yml up -d
     ```
  3. Abrir la consola web `http://localhost:15672` (guest / guest). En la pestaña **Overview**, en la sección **Nodes**, mostrar que aparecen `rabbit@rabbitmq1` y `rabbit@rabbitmq2` operando bajo el mismo clúster con la Erlang Cookie sincronizada.

### Punto 9: Tres colas de mensajes con sus respectivas DLQs funcionando
- **Qué mostrar:** En la pestaña **Queues** de RabbitMQ, mostrar las colas principales junto a sus DLQs hermanas:
  1. `notificacion.solicitudes.queue` $\leftrightarrow$ `notificacion.solicitudes.dlq`
  2. `reportes.solicitudes.queue` $\leftrightarrow$ `reportes.solicitudes.dlq`
  3. `pagos.solicitudes.queue` $\leftrightarrow$ `pagos.solicitudes.dlq`
  *(Adicionalmente mostrar `usuarios.valoraciones.queue` y `solicitudes.pagos.queue`).*

### Punto 10: Exchanges de tipo Direct y Topic correctamente configurados
- **Qué mostrar:** En la pestaña **Exchanges** de RabbitMQ:
  - `homefixer.events`: Mostrar que es de tipo **topic** y durable (**D**), con los bindings hacia las distintas colas.
  - `homefixer.dlx`: Mostrar que es de tipo **direct** y durable (**D**), con los bindings hacia las DLQs.
  - `homefixer.rpc`: Mostrar que es de tipo **direct** y durable (**D**), para las consultas síncronas.

### Punto 11: Ejecución de docker-compose levantando servicios RabbitMQ
- **Qué mostrar:** En la terminal ejecutar:
  ```bash
  docker compose ps
  ```
  Mostrar el contenedor de RabbitMQ saludable (`healthy`), con los puertos 5672 (AMQP) y 15672 (HTTP Management) mapeados y el volumen de datos persistente montado.

---

## 🎤 5. Guión de Presentación Oral (5 a 10 Minutos)

> **Consejo:** Hablar con seguridad, utilizando terminología técnica formal (*"topología orientada a eventos"*, *"desacoplamiento asíncrono"*, *"idempotencia"*, *"backoff lineal"*).

### Minuto 0:00 - 01:30 | Introducción y Arquitectura Cloud Native
*"Buenos días profesor. Presentamos HomeFixer, una plataforma cloud native orientada a conectar clientes con técnicos especialistas. Nuestro sistema fue diseñado bajo una arquitectura de microservicios distribuida con Spring Boot 3 y Java 17, discovery con Netflix Eureka, seguridad perimetral en Spring Cloud Gateway y persistencia cloud en PostgreSQL mediante Supabase.*  
*Para resolver la comunicación interservicios implementamos una estrategia híbrida: peticiones síncronas para consultas inmediatas y mensajería reactiva asíncrona mediante RabbitMQ para garantizar alta disponibilidad y tolerancia a fallos."*

### Minuto 01:30 - 03:00 | Seguridad, IDaaS y API Gateway
*(Puntos 1, 2, 3, 4, 5, 6 y 7 de la presentación)*  
*"En pantalla vemos nuestra integración de seguridad. Usamos Supabase como IDaaS bajo estándares OAuth 2.0 y OpenID Connect. Aquí en el dashboard de Supabase se aprecian los usuarios registrados con sus identificadores únicos. Al iniciar sesión en el frontend, se emite un JWT firmado que viaja en la cabecera Authorization.*  
*El API Gateway intercepta cada petición. Si enviamos una solicitud con un token adulterado, el gateway responde inmediatamente con un código HTTP 401 Unauthorized gracias a nuestro validador Nimbus de firmas y audiencias. Al enviar el token auténtico, el gateway autoriza la petición y realiza el enrutamiento dinámico balanceado a través de Eureka."*

### Minuto 03:00 - 05:30 | Topología RabbitMQ, Clúster y Publish/Subscribe
*(Puntos 8, 9, 10 y 11 de la presentación + Indicadores 1, 2, 3 de la rúbrica)*  
*"Pasando al núcleo de mensajería, aquí en Docker Compose demostramos la ejecución de nuestro servicio RabbitMQ, el cual hemos configurado en un clúster de alta disponibilidad de dos nodos utilizando Classic Peer Discovery y Erlang Cookie compartida.*  
*Toda la topología se encuentra rigurosamente centralizada en nuestro módulo compartido 'shared', en la clase MessagingConstants y EventQueues, garantizando que ningún nombre de cola o exchange quede suelto en el código.*  
*Implementamos el patrón Publish/Subscribe con el exchange topic 'homefixer.events'. Cuando un servicio emite un evento como 'solicitud.estado-actualizado', este se enruta simultáneamente a la cola de notificaciones, de reportes y de pagos. Cada cola es durable y utiliza mensajes persistentes en disco."*

### Minuto 05:30 - 08:00 | Demostración en Vivo: ACK Manual, Fallo Controlado y DLQ
*(Puntos 9, 10 + Indicadores 4, 5 de la rúbrica)*  
*"Ahora demostraremos la confiabilidad y el manejo de confirmaciones ACK. Nuestros productores utilizan Publisher Confirms: el broker notifica explícitamente cuando el mensaje fue persistido.*  
*En el lado de los consumidores, el modo es estrictamente manual. En NotificacionEventListener, si el procesamiento es exitoso se envía un basicAck. Pero si el proveedor falla, el sistema aplica 3 reintentos con backoff lineal progresivo.*  
*Vamos a forzar un fallo en vivo enviando un mensaje con la clave 'FORZAR_ERROR'. Observemos los logs: el sistema registra los reintentos fallidos y finalmente ejecuta un basicNack con requeue falso. Ahora nos dirigimos a la consola web de RabbitMQ: vemos que el mensaje ingresó a la cola 'notificacion.solicitudes.dlq'. Si inspeccionamos sus propiedades, podemos auditar la cabecera x-death que certifica el motivo exacto del desvío."*

### Minuto 08:00 - 09:30 | Mensajes Síncronos (RPC), Alertas y Microservicio Administrador
*(Indicadores 6, 7 y 8 de la rúbrica)*  
*"Para los flujos síncronos, implementamos RPC sobre RabbitMQ mediante el exchange 'homefixer.rpc', con correlation ID y timeout de 5 segundos para proteger el sistema ante cuellos de botella.*  
*Además, implementamos un monitor proactivo 'ColasMonitor' que vigila cada 60 segundos la saturación de colas y la presencia de mensajes en DLQ, registrando alertas operacionales.*  
*Finalmente, para cumplir con la administración dinámica de la infraestructura, implementamos RabbitAdminService y RabbitAdminController en ms-notificacion. Esta API REST permite crear y eliminar colas y exchanges en caliente. Cada endpoint valida rigurosamente sus parámetros mediante DTOs anotados con Bean Validation: si intentamos crear una cola sin nombre o con sintaxis inválida, la API rechaza la solicitud con un código HTTP 400 Bad Request y un mensaje descriptivo.*  
*Con esto, HomeFixer cumple de forma íntegra con todos los requerimientos de diseño, resiliencia y evaluación de la asignatura. Quedamos atentos a sus preguntas."*

---

## 💡 6. Banco de Preguntas Difíciles del Docente con Respuestas Técnicas

### P1: ¿Por qué eligieron un Exchange Topic en lugar de Fanout o Direct para los eventos de negocio?
**Respuesta:** *"Un exchange Fanout difundiría ciegamente todo evento a todas las colas sin distinción, sobrecargando microservicios con datos que no les competen. Por su parte, un Direct obligaría a un acoplamiento rígido de routing keys 1 a 1. Elegimos un Exchange **Topic** porque permite enrutamiento jerárquico desacoplado mediante comodines (`solicitud.#`, `pago.#`). De esta manera, ms-notificacion puede suscribirse a todos los eventos del sistema con patrones comodín, mientras que ms-pagos solo se suscribe específicamente a `solicitud.estado-actualizado` sin requerir cambios en el productor al incorporar futuros microservicios."*

### P2: ¿Qué sucede si el broker RabbitMQ se cae justo cuando un microservicio intenta publicar un evento?
**Respuesta:** *"En nuestra implementación actual, el `EventPublisher` captura la `AmqpException`, emite un log de nivel `[ALERTA]` y permite que la transacción de negocio local concluya exitosamente sin hacer rollback, priorizando la disponibilidad del servicio ante el cliente. No obstante, en un entorno de producción estricto, la evolución natural de este diseño es incorporar el patrón **Transactional Outbox**, persistiendo el evento en una tabla relacional local en la misma transacción ACID de negocio y despachándolo de forma asíncrona hacia RabbitMQ mediante un relay o Debezium (Change Data Capture)."*

### P3: ¿Por qué en el ACK manual configuran `requeue = false` al hacer NACK? ¿No se pierde el mensaje?
**Respuesta:** *"Si hiciéramos `basicNack` con `requeue = true`, el mensaje volvería a la cabeza de la misma cola e intentaría procesarse de inmediato. Si el error se debe a un dato malformado o un fallo no transitorio (poison pill), el mensaje entraría en un bucle infinito de reintentos, saturando la CPU y bloqueando a los demás mensajes de la cola. Al configurar `requeue = false`, y dado que la cola principal tiene configurado el argumento `x-dead-letter-exchange: homefixer.dlx`, el broker traslada el mensaje de forma segura a su respectiva **Dead Letter Queue (DLQ)**, donde queda aislado y persistido para su análisis forense o reprocesamiento posterior."*

### P4: ¿Por qué las colas RPC tienen un TTL de 10 segundos y no tienen DLQ?
**Respuesta:** *"Porque en una comunicación síncrona Request/Reply, el hilo del cliente tiene un timeout configurado (en nuestro caso 5 segundos). Si el servidor no respondió en ese lapso, el cliente ya abortó la espera y retornó un código 503 al usuario. Almacenar peticiones síncronas vencidas en una DLQ carece de valor porque ya nadie está escuchando en la cola de respuesta temporal (`reply_to`), por lo que el TTL de 10 segundos permite purgar la cola automáticamente sin consumir recursos innecesarios."*

### P5: ¿Cómo evitan que un mensaje reentregado provoque un doble cobro o inconsistencias (Idempotencia)?
**Respuesta:** *"En sistemas distribuidos la entrega de mensajes es de tipo **at-least-once** (al menos una vez). Por ende, todos nuestros consumidores fueron diseñados con filtros de **idempotencia**:*  
- *En `ms-pagos`, antes de registrar un cobro se valida mediante consulta de base de datos si la solicitud ya posee un cobro asignado.*  
- *En `ms-notificacion`, existe una restricción de unicidad compuesta `(evento_id, usuario_id)` en la tabla relacional, de modo que si un evento se reentrega, la base de datos ignora la duplicidad sin generar notificaciones repetidas.*  
- *En `ms-solicitudes`, el método `marcarPagada` comprueba el estado actual: si ya se encuentra en `PAGADA`, la operación no realiza ninguna modificación adicional."*

### P6: ¿Cómo funciona el Clúster de 2 Nodos de RabbitMQ y qué ventaja ofrece?
**Respuesta:** *"El clúster está configurado mediante `docker-compose.cluster.yml` con dos nodos (`rabbitmq1` y `rabbitmq2`). Ambos nodos comparten la misma Erlang Cookie (`HOMEFIXER_CLUSTER_SECRET_2026`), lo que les permite autenticarse mutuamente en la red interna de Docker. Utilizan el backend de descubrimiento declarativo `rabbit_peer_discovery_classic_config`. Esto permite que todos los exchanges, colas y usuarios se sincronicen entre ambos nodos, ofreciendo alta disponibilidad, tolerancia a caídas y balanceo de carga para los microservicios."*

---

## 🛠️ 7. Comandos de Demostración Rápida (Cheat Sheet para la Defensa)

### 1. Iniciar el Ecosistema Completo
```cmd
start-all.bat
```

### 2. Iniciar el Clúster de 2 Nodos de RabbitMQ
```cmd
docker compose -f docker-compose.cluster.yml up -d
```
*Verificación en navegador:* `http://localhost:15672` (guest / guest). En *Overview > Nodes* deben figurar ambos nodos.

### 3. Demostrar la API REST de Administración (RabbitAdminService)
```bash
# 1. Crear una cola nueva validada
curl -i -X POST http://localhost:8080/api/admin/queues -H "Content-Type: application/json" -d "{\"nombre\":\"cola.auditoria.demo\",\"durable\":true,\"ttlMs\":86400000,\"maxLength\":5000}"

# 2. Consultar información de la cola recién creada
curl -i -X GET http://localhost:8080/api/admin/queues/cola.auditoria.demo

# 3. Demostrar validación de entrada (Intento de crear cola con nombre vacío -> Retorna 400 Bad Request)
curl -i -X POST http://localhost:8080/api/admin/queues -H "Content-Type: application/json" -d "{\"nombre\":\"\",\"durable\":true}"

# 4. Eliminar la cola de prueba
curl -i -X DELETE http://localhost:8080/api/admin/queues/cola.auditoria.demo
```

### 4. Consultar Estado de Monitoreo de Colas y DLQ
```bash
curl -i -X GET http://localhost:8080/api/notificaciones/mensajeria/colas
```

### 5. Probar Consulta RPC Síncrona (ms-reportes ↔ ms-solicitudes)
```bash
curl -i -X GET http://localhost:8080/api/reportes/solicitudes/resumen
```

---

## ✅ Conclusión
Con este documento y los artefactos creados en el repositorio:
- El código cumple físicamente al **100% de la rúbrica** de ambas evaluaciones (EP3 y EP4).
- Cuentas con un **guión exacto de exposición** para defender el proyecto en el tiempo asignado (5 a 10 minutos).
- Cuentas con respuestas de nivel senior para cualquier pregunta de arquitectura o mensajería que realice el docente.
