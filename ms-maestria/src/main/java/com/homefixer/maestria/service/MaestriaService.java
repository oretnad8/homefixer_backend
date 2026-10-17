package com.homefixer.maestria.service;

import com.homefixer.maestria.client.UsuariosFeignClient;
import com.homefixer.maestria.entity.Certificacion;
import com.homefixer.maestria.entity.EstadoCertificacion;
import com.homefixer.maestria.entity.InsigniaMaestria;
import com.homefixer.maestria.repository.CertificacionRepository;
import com.homefixer.maestria.repository.InsigniaMaestriaRepository;
import com.homefixer.shared.dto.NotificacionEventDTO;
import com.homefixer.shared.messaging.EventPublisher;
import com.homefixer.shared.messaging.MessagingConstants;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Crecimiento profesional del técnico (diagramas de actividades y de procesos):
 * <ol>
 *   <li>Con nivel de reputación suficiente (homefixer.maestria.nivel-minimo) sube una certificación -> PENDIENTE.</li>
 *   <li>Un administrador la valida: APROBADA otorga la insignia "Maestro en &lt;oficio&gt;"; RECHAZADA guarda el motivo.</li>
 *   <li>La decisión se publica (certificacion.aprobada / certificacion.rechazada) y ms-notificacion avisa al técnico.</li>
 * </ol>
 * Los roles viven en ms-usuarios: quien llama se identifica con GET /api/usuarios/me usando su token.
 */
@Slf4j
@Service
public class MaestriaService {

    /** Orden de los niveles de reputación de ms-usuarios. */
    private static final List<String> NIVELES = List.of("NUEVO", "BUENO", "EXPERTO");

    private static final Map<String, String> OFICIOS = Map.of(
            "plomeria", "Plomería",
            "electricidad", "Electricidad",
            "carpinteria", "Carpintería",
            "pintura", "Pintura",
            "cerrajeria", "Cerrajería",
            "limpieza", "Limpieza",
            "jardineria", "Jardinería",
            "refrigeracion", "Refrigeración",
            "otros", "Otros oficios");

    private final CertificacionRepository certificacionRepository;
    private final InsigniaMaestriaRepository insigniaMaestriaRepository;
    private final UsuariosFeignClient usuariosFeignClient;
    private final EventPublisher eventPublisher;
    private final String nivelMinimo;

    public MaestriaService(CertificacionRepository certificacionRepository,
                           InsigniaMaestriaRepository insigniaMaestriaRepository,
                           UsuariosFeignClient usuariosFeignClient, EventPublisher eventPublisher,
                           @Value("${homefixer.maestria.nivel-minimo:BUENO}") String nivelMinimo) {
        this.certificacionRepository = certificacionRepository;
        this.insigniaMaestriaRepository = insigniaMaestriaRepository;
        this.usuariosFeignClient = usuariosFeignClient;
        this.eventPublisher = eventPublisher;
        this.nivelMinimo = nivelMinimo.trim().toUpperCase();
    }

    /** Nivel de reputación necesario para postular, para mostrarlo en el perfil. */
    public String getNivelMinimo() {
        return nivelMinimo;
    }

    // ---------- Técnico ----------

    @Transactional
    public Certificacion subirCertificacion(String authorization, String titulo, String institucion, String categoria,
                                            LocalDate fechaEmision, String urlDocumento) {
        Map<String, Object> usuario = usuarioActual(authorization);
        if (!Boolean.TRUE.equals(usuario.get("esTecnico"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo los técnicos pueden subir certificaciones.");
        }
        Long tecnicoId = ((Number) usuario.get("id")).longValue();

        String oficio = categoria == null ? "" : categoria.trim().toLowerCase();
        if (!OFICIOS.containsKey(oficio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Oficio no válido.");
        }
        if (vacio(titulo) || vacio(institucion) || vacio(urlDocumento)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completa el título, la institución y el documento.");
        }

        String nivel = nivelDe(tecnicoId);
        if (NIVELES.indexOf(nivel) < NIVELES.indexOf(nivelMinimo)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Necesitas nivel de reputación " + nivelMinimo
                    + " para postular certificaciones (tu nivel actual es " + nivel + ").");
        }
        if (insigniaMaestriaRepository.existsByTecnicoIdAndCategoria(tecnicoId, oficio)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya tienes la insignia de maestría en " + OFICIOS.get(oficio) + ".");
        }
        if (certificacionRepository.existsByTecnicoIdAndCategoriaAndEstado(tecnicoId, oficio, EstadoCertificacion.PENDIENTE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya tienes una certificación de " + OFICIOS.get(oficio) + " en revisión.");
        }

        Certificacion guardada = certificacionRepository.save(Certificacion.builder()
                .tecnicoId(tecnicoId)
                .tecnicoNombre(String.valueOf(usuario.get("nombre")))
                .titulo(titulo.trim())
                .institucion(institucion.trim())
                .categoria(oficio)
                .fechaEmision(fechaEmision)
                .urlDocumento(urlDocumento)
                .estado(EstadoCertificacion.PENDIENTE)
                .verificado(false)
                .fechaSolicitud(LocalDateTime.now())
                .build());
        log.info("Técnico {} subió la certificación {} ({})", tecnicoId, guardada.getId(), oficio);
        return guardada;
    }

    public List<Certificacion> getCertificaciones(Long tecnicoId) {
        return certificacionRepository.findByTecnicoIdOrderByFechaSolicitudDesc(tecnicoId);
    }

    public List<InsigniaMaestria> getInsignias(Long tecnicoId) {
        return insigniaMaestriaRepository.findByTecnicoIdOrderByFechaOtorgadaDesc(tecnicoId);
    }

    /** Insignias de varios técnicos (búsqueda de ms-ubicacion). */
    public List<InsigniaMaestria> getInsigniasDe(Collection<Long> tecnicoIds) {
        return tecnicoIds.isEmpty() ? List.of() : insigniaMaestriaRepository.findByTecnicoIdIn(tecnicoIds);
    }

    // ---------- Administrador ----------

    public List<Certificacion> getPendientes(String authorization) {
        exigirAdmin(authorization);
        return certificacionRepository.findByEstadoOrderByFechaSolicitudAsc(EstadoCertificacion.PENDIENTE);
    }

    @Transactional
    public Certificacion aprobar(String authorization, Long certificacionId) {
        exigirAdmin(authorization);
        Certificacion c = pendiente(certificacionId);
        c.setEstado(EstadoCertificacion.APROBADA);
        c.setVerificado(true);
        c.setMotivoRechazo(null);
        c.setFechaRevision(LocalDateTime.now());
        Certificacion guardada = certificacionRepository.save(c);

        String oficio = OFICIOS.getOrDefault(c.getCategoria(), "Otros oficios");
        if (!insigniaMaestriaRepository.existsByTecnicoIdAndCategoria(c.getTecnicoId(), c.getCategoria())) {
            insigniaMaestriaRepository.save(InsigniaMaestria.builder()
                    .tecnicoId(c.getTecnicoId())
                    .nombre("Maestro en " + oficio)
                    .descripcion(c.getTitulo() + " · " + c.getInstitucion())
                    .iconoUrl("categoria:" + c.getCategoria())
                    .categoria(c.getCategoria())
                    .certificacionId(c.getId())
                    .fechaOtorgada(LocalDateTime.now())
                    .build());
        }
        publicar(MessagingConstants.RK_CERTIFICACION_APROBADA, "CERTIFICACION_APROBADA", c,
                "Tu certificación \"" + c.getTitulo() + "\" fue aprobada. Obtuviste la insignia Maestro en " + oficio + ".");
        return guardada;
    }

    @Transactional
    public Certificacion rechazar(String authorization, Long certificacionId, String motivo) {
        exigirAdmin(authorization);
        if (vacio(motivo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica el motivo del rechazo.");
        }
        Certificacion c = pendiente(certificacionId);
        c.setEstado(EstadoCertificacion.RECHAZADA);
        c.setVerificado(false);
        c.setMotivoRechazo(motivo.trim());
        c.setFechaRevision(LocalDateTime.now());
        Certificacion guardada = certificacionRepository.save(c);
        publicar(MessagingConstants.RK_CERTIFICACION_RECHAZADA, "CERTIFICACION_RECHAZADA", c,
                "Tu certificación \"" + c.getTitulo() + "\" fue rechazada: " + c.getMotivoRechazo());
        return guardada;
    }

    // ---------- helpers ----------

    private Certificacion pendiente(Long certificacionId) {
        Certificacion c = certificacionRepository.findById(certificacionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificación no encontrada."));
        if (c.getEstado() != EstadoCertificacion.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta certificación ya fue revisada.");
        }
        return c;
    }

    private void publicar(String routingKey, String tipo, Certificacion c, String mensaje) {
        eventPublisher.publicar(routingKey, NotificacionEventDTO.builder()
                .tecnicosIds(List.of(c.getTecnicoId()))
                .destinatariosIds(List.of(c.getTecnicoId()))
                .tipoServicio(tipo)
                .categoria(c.getCategoria())
                .mensaje(mensaje)
                .build());
    }

    private void exigirAdmin(String authorization) {
        if (!Boolean.TRUE.equals(usuarioActual(authorization).get("esAdmin"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un administrador puede revisar certificaciones.");
        }
    }

    private Map<String, Object> usuarioActual(String authorization) {
        if (authorization == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token faltante.");
        }
        try {
            return usuariosFeignClient.usuarioActual(authorization);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario no registrado.");
        } catch (Exception e) {
            log.error("No se pudo consultar ms-usuarios: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo verificar tu cuenta. Intenta más tarde.");
        }
    }

    private String nivelDe(Long tecnicoId) {
        try {
            Object nivel = usuariosFeignClient.obtenerTecnico(tecnicoId).get("nivelReputacion");
            return nivel != null && NIVELES.contains(nivel.toString()) ? nivel.toString() : "NUEVO";
        } catch (Exception e) {
            log.error("No se pudo consultar el nivel del técnico {}: {}", tecnicoId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo verificar tu nivel. Intenta más tarde.");
        }
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }
}
