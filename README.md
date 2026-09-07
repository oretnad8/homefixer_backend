# HomeFixer Backend

HomeFixer es una plataforma integral diseñada bajo una **arquitectura de microservicios** que conecta a clientes que necesitan reparaciones u obras en el hogar con técnicos especializados. 

Este repositorio contiene todo el ecosistema backend implementado con Java y Spring Boot, diseñado para ser escalable, modular y cloud-native.

## 🚀 Tecnologías Principales
- **Lenguaje:** Java 17+
- **Framework Core:** Spring Boot 3.2.x
- **Cloud & Discovery:** Spring Cloud 2023.0.x (Netflix Eureka, Spring Cloud Gateway, OpenFeign)
- **Persistencia:** Spring Data JPA / Hibernate
- **Base de Datos:** MySQL
- **Seguridad:** JWT (JSON Web Tokens) en API Gateway y soporte preparado para Azure AD (MSAL)
- **Documentación API:** SpringDoc OpenAPI 3 (Swagger UI)
- **Herramientas Adicionales:** Lombok, Maven (Multi-módulo)

---

## 🏗 Arquitectura del Sistema

El sistema utiliza un **API Gateway** como único punto de entrada para los clientes. El Gateway se encarga del enrutamiento y la validación de seguridad (CORS y JWT). Todos los microservicios se registran dinámicamente en el **Servidor Eureka**, permitiendo el balanceo de carga interno y la resolución de nombres estática y transparente (ej. mediante `OpenFeign`).

### 📦 Módulos y Puertos Asignados

| Módulo / Microservicio | Puerto | Descripción | Base de Datos |
| ---------------------- | :----: | ----------- | :-----------: |
| **`eureka-server`** | `8761` | Servidor de descubrimiento de servicios. | No |
| **`api-gateway`** | `8080` | Punto de entrada, validación de JWT, Gateway (WebFlux). | No |
| **`ms-usuarios`** | `8081` | Gestión de perfiles, roles (Cliente, Técnico) y reputación. | Sí |
| **`ms-solicitudes`** | `8082` | Gestión de solicitudes de servicio, estados y asignaciones. | Sí |
| **`ms-autenticacion`**| `8083` | Generación de tokens JWT, login y registro (preparado Azure AD). | No |
| **`ms-maestria`** | `8084` | Certificaciones e insignias para los técnicos. | Sí |
| **`ms-ubicacion`** | `8085` | Lógica geoespacial y proximidad de técnicos. | No |
| **`ms-notificacion`** | `8086` | Envío de alertas, emails y SMS. | No |
| **`ms-pagos`** | `8087` | Procesamiento de pagos, recibos y transacciones. | Sí |
| **`ms-valoraciones`** | `8088` | Reseñas y calificaciones de técnicos y clientes. | Sí |
| **`ms-promociones`** | `8089` | Gestión de cupones y planes premium. | No |
| **`ms-reportes`** | `8090` | Consolidación de métricas y analíticas. | No |
| **`shared`** | `N/A` | Módulo común (Librería) con Enums, DTOs compartidos y Swagger.| No |

---

## 🔒 Seguridad y Configuración

El proyecto está diseñado para desplegarse fácilmente en servicios Cloud (como Azure MySQL Flexible Server), por lo tanto **no existen credenciales hardcodeadas**. Se utilizan Variables de Entorno:

### Variables de Base de Datos
- `SPRING_DATASOURCE_URL` (Ej: `jdbc:mysql://localhost:3306/homefixer_bd?useSSL=false`)
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

*(Por defecto, si no se definen las variables, el sistema intentará conectarse localmente a `localhost:3306` bajo los esquemas individuales de cada microservicio, usando usuario `root`).*

### Variables de Autenticación (Azure / MSAL)
- `AZURE_CLIENT_ID`
- `AZURE_TENANT_ID`
- `JWT_SECRET` (Firma HMAC para la validación en el Gateway).

---

## 📖 Documentación de APIs (Swagger)

**SpringDoc OpenAPI 3** se encuentra integrado transversalmente en todo el ecosistema. 
Para acceder a la documentación interactiva de cualquier servicio de manera local, inicia la aplicación y navega a:

`http://localhost:<PUERTO>/swagger-ui/index.html`

*Ejemplo para el microservicio de usuarios:* [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)

---

## 🛠️ Cómo Ejecutar el Proyecto (Paso a Paso)

Dado que todos los módulos conforman un ecosistema interdependiente, se recomienda el siguiente orden de ejecución:

1. **Base de Datos:** Asegúrate de tener una instancia de MySQL corriendo en el puerto `3306`.
2. **Build Global:** Desde la carpeta raíz del proyecto, instala todos los módulos ejecutando:
   ```bash
   mvn clean install -DskipTests
   ```
   *(Esto es crucial para que el módulo `shared` se compile y quede disponible para el resto).*
3. **Servidor Eureka:** Inicia primero `eureka-server` (Puerto 8761).
4. **API Gateway:** Inicia `api-gateway` (Puerto 8080).
5. **Microservicios de Negocio:** Una vez que Eureka esté vivo, puedes levantar cualquiera de los demás microservicios (`ms-usuarios`, `ms-solicitudes`, etc.). Automáticamente se registrarán en Eureka en unos pocos segundos.

---

## 🔄 Comunicación Interna (Síncrona)
El ecosistema implementa comunicación de microservicio a microservicio utilizando **Spring Cloud OpenFeign**. 
- Ejemplo implementado: `ms-valoraciones` utiliza un `UsuarioFeignClient` para comunicarse directamente con `ms-usuarios` y actualizar la reputación de un técnico al recibir una nueva reseña.
- Ejemplo implementado: `ms-solicitudes` utiliza un `UbicacionFeignClient` para solicitar la lista de técnicos cercanos a `ms-ubicacion`.

---
*Documentación autogenerada - Arquitectura Fase 4.*
