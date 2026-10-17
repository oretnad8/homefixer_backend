package com.homefixer.solicitudes.service;

import com.homefixer.shared.dto.NotificacionEventDTO;
import com.homefixer.shared.enums.EstadoSolicitud;
import com.homefixer.shared.messaging.EventPublisher;
import com.homefixer.shared.messaging.MessagingConstants;
import com.homefixer.solicitudes.client.UbicacionFeignClient;
import com.homefixer.solicitudes.client.UsuariosFeignClient;
import com.homefixer.solicitudes.entity.Solicitud;
import com.homefixer.solicitudes.entity.SolicitudRechazo;
import com.homefixer.solicitudes.repository.SolicitudRechazoRepository;
import com.homefixer.solicitudes.repository.SolicitudRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitudServiceImpl implements SolicitudService {

    private static final Set<EstadoSolicitud> ACTIVAS = EnumSet.of(EstadoSolicitud.ACEPTADA, EstadoSolicitud.EN_PROCESO);
    /** Trabajos terminados (pagados o a la espera del pago) */
    private static final Set<EstadoSolicitud> TERMINADAS = EnumSet.of(EstadoSolicitud.COMPLETADA, EstadoSolicitud.PAGADA);
    private static final String[] DIAS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

    private final SolicitudRepository solicitudRepository;
    private final SolicitudRechazoRepository rechazoRepository;
    private final UbicacionFeignClient ubicacionFeignClient;
    private final UsuariosFeignClient usuariosFeignClient;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public Solicitud createSolicitud(Solicitud solicitud) {
        LocalDateTime ahora = LocalDateTime.now();
        solicitud.setId(null);
        solicitud.setTecnicoId(null);
        solicitud.setEstadoSolicitud(EstadoSolicitud.CREADA);
        solicitud.setFecha(ahora);
        solicitud.setFechaActualizacion(ahora);
        if (solicitud.getCategoria() == null) solicitud.setCategoria("otros");
        if (solicitud.getUrgencia() == null) solicitud.setUrgencia("normal");
        if (solicitud.getPrecioEstimado() == null) solicitud.setPrecioEstimado(0.0);
        Solicitud guardada = solicitudRepository.save(solicitud);

        List<Long> tecnicosIds = new ArrayList<>();
        try {
            tecnicosIds = ubicacionFeignClient.buscarTecnicosCercanos(guardada.getCoordenadas(), guardada.getCategoria());
        } catch (Exception e) {
            log.warn("No se pudieron buscar técnicos cercanos: {}", e.getMessage());
        }

        publicar(MessagingConstants.RK_SOLICITUD_CREADA, guardada, tecnicosIds, "NUEVA_SOLICITUD",
                "Nueva solicitud de " + etiqueta(guardada.getCategoria()) + ": " + guardada.getDescripcion());
        return guardada;
    }

    @Override
    public Solicitud getSolicitudById(Long id) {
        return solicitudRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud no encontrada."));
    }

    @Override
    public List<Solicitud> getSolicitudesByCliente(Long clienteId) {
        return solicitudRepository.findByClienteIdOrderByFechaDesc(clienteId);
    }

    @Override
    public List<Solicitud> getSolicitudesByTecnico(Long tecnicoId) {
        return solicitudRepository.findByTecnicoIdOrderByFechaDesc(tecnicoId);
    }

    @Override
    public List<Solicitud> getSolicitudesDisponibles(Long tecnicoId) {
        // Solo las solicitudes del oficio del técnico
        String especialidad = texto(obtenerTecnico(tecnicoId).get("especialidad"));
        return solicitudRepository.findDisponiblesPara(tecnicoId).stream()
                .filter(s -> mismaEspecialidad(especialidad, s.getCategoria()))
                .toList();
    }

    @Override
    public long countSolicitudesActivasByTecnico(Long tecnicoId) {
        return solicitudRepository.countByTecnicoIdAndEstadoSolicitudIn(tecnicoId, ACTIVAS);
    }

    @Override
    @Transactional
    public Solicitud assignTecnico(Long solicitudId, Long tecnicoId) {
        Solicitud solicitud = getSolicitudById(solicitudId);
        exigirEstado(solicitud, EstadoSolicitud.CREADA, "Esta solicitud ya tiene un técnico asignado.");
        if (tecnicoId.equals(solicitud.getClienteId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes tomar tu propia solicitud.");
        }

        Map<String, Object> tecnico = obtenerTecnico(tecnicoId);
        String especialidad = texto(tecnico.get("especialidad"));
        if (!mismaEspecialidad(especialidad, solicitud.getCategoria())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este técnico atiende " + etiqueta(especialidad)
                    + " y la solicitud es de " + etiqueta(solicitud.getCategoria()) + ".");
        }
        solicitud.setTecnicoId(tecnicoId);
        solicitud.setTecnicoNombre(texto(tecnico.get("nombre")));
        solicitud.setTecnicoTelefono(texto(tecnico.get("telefono")));
        solicitud.setTecnicoEspecialidad(texto(tecnico.get("especialidad")));
        solicitud.setTecnicoCalificacion(numero(tecnico.get("calificacionPromedio")));
        Double precioBase = numero(tecnico.get("precioBase"));
        if (precioBase != null && (solicitud.getPrecioEstimado() == null || solicitud.getPrecioEstimado() == 0)) {
            solicitud.setPrecioEstimado(precioBase);
        }
        Solicitud guardada = cambiarEstado(solicitud, EstadoSolicitud.ACEPTADA);

        publicar(MessagingConstants.RK_SOLICITUD_TECNICO_ASIGNADO, guardada, List.of(tecnicoId), "TECNICO_ASIGNADO",
                guardada.getTecnicoNombre() + " atenderá tu solicitud de " + etiqueta(guardada.getCategoria()) + ".");
        return guardada;
    }

    @Override
    @Transactional
    public void rechazar(Long solicitudId, Long tecnicoId) {
        getSolicitudById(solicitudId);
        if (!rechazoRepository.existsBySolicitudIdAndTecnicoId(solicitudId, tecnicoId)) {
            rechazoRepository.save(new SolicitudRechazo(null, solicitudId, tecnicoId, LocalDateTime.now()));
        }
    }

    @Override
    @Transactional
    public Solicitud iniciar(Long solicitudId) {
        Solicitud solicitud = getSolicitudById(solicitudId);
        exigirEstado(solicitud, EstadoSolicitud.ACEPTADA, "Solo se puede iniciar un trabajo aceptado.");
        Solicitud guardada = cambiarEstado(solicitud, EstadoSolicitud.EN_PROCESO);
        publicarCambioEstado(guardada, "El técnico " + guardada.getTecnicoNombre() + " comenzó el trabajo.");
        return guardada;
    }

    @Override
    @Transactional
    public Solicitud finalizar(Long solicitudId, String notas) {
        Solicitud solicitud = getSolicitudById(solicitudId);
        exigirEstado(solicitud, EstadoSolicitud.EN_PROCESO, "Solo se puede finalizar un trabajo en curso.");
        solicitud.setNotasCierre(notas);
        Solicitud guardada = cambiarEstado(solicitud, EstadoSolicitud.COMPLETADA);
        publicarCambioEstado(guardada, "Tu servicio fue completado. Ya puedes pagarlo.");
        return guardada;
    }

    @Override
    @Transactional
    public void marcarPagada(Long solicitudId) {
        Solicitud solicitud = getSolicitudById(solicitudId);
        if (solicitud.getEstadoSolicitud() == EstadoSolicitud.PAGADA) return; // evento reentregado
        exigirEstado(solicitud, EstadoSolicitud.COMPLETADA, "La solicitud " + solicitudId + " no está completada.");
        // Sin evento propio: pago.procesado ya notificó al cliente y al técnico
        cambiarEstado(solicitud, EstadoSolicitud.PAGADA);
    }

    @Override
    @Transactional
    public Solicitud cancelar(Long solicitudId) {
        Solicitud solicitud = getSolicitudById(solicitudId);
        if (solicitud.getEstadoSolicitud() != EstadoSolicitud.CREADA
                && solicitud.getEstadoSolicitud() != EstadoSolicitud.ACEPTADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede cancelar un trabajo ya iniciado o terminado.");
        }
        Solicitud guardada = cambiarEstado(solicitud, EstadoSolicitud.CANCELADA);
        publicarCambioEstado(guardada, "La solicitud #" + guardada.getId() + " fue cancelada.");
        return guardada;
    }

    @Override
    @Transactional
    public Solicitud updateEstado(Long solicitudId, EstadoSolicitud estado) {
        Solicitud guardada = cambiarEstado(getSolicitudById(solicitudId), estado);
        publicarCambioEstado(guardada, "El estado de tu solicitud cambió a: " + estado.name());
        return guardada;
    }

    @Override
    public Map<String, Object> estadisticasTecnico(Long tecnicoId) {
        List<Solicitud> trabajos = solicitudRepository.findByTecnicoIdOrderByFechaDesc(tecnicoId);
        LocalDate hoy = LocalDate.now();
        LocalDate lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        double[] porDia = new double[7];
        long trabajosHoy = 0;
        long completados = 0;

        for (Solicitud s : trabajos) {
            LocalDate dia = (s.getFechaActualizacion() != null ? s.getFechaActualizacion() : s.getFecha()).toLocalDate();
            boolean vigente = s.getEstadoSolicitud() != EstadoSolicitud.CANCELADA;
            if (vigente && dia.equals(hoy)) trabajosHoy++;
            if (TERMINADAS.contains(s.getEstadoSolicitud())) {
                completados++;
                if (!dia.isBefore(lunes) && !dia.isAfter(lunes.plusDays(6)) && s.getPrecioEstimado() != null) {
                    porDia[dia.getDayOfWeek().getValue() - 1] += s.getPrecioEstimado();
                }
            }
        }

        List<Map<String, Object>> gananciasPorDia = new ArrayList<>();
        double semana = 0;
        for (int i = 0; i < 7; i++) {
            gananciasPorDia.add(Map.of("dia", DIAS[i], "monto", porDia[i]));
            semana += porDia[i];
        }

        Double calificacion = null;
        try {
            calificacion = numero(usuariosFeignClient.obtenerTecnico(tecnicoId).get("calificacionPromedio"));
        } catch (Exception e) {
            log.warn("No se pudo obtener la calificación del técnico {}: {}", tecnicoId, e.getMessage());
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("trabajosHoy", trabajosHoy);
        stats.put("gananciasSemana", semana);
        stats.put("calificacion", calificacion != null ? calificacion : 0.0);
        stats.put("trabajosCompletados", completados);
        stats.put("gananciasPorDia", gananciasPorDia);
        return stats;
    }

    @Override
    public List<Long> findNearbyTecnicos(Long solicitudId) {
        Solicitud solicitud = getSolicitudById(solicitudId);
        try {
            return ubicacionFeignClient.buscarTecnicosCercanos(solicitud.getCoordenadas(), etiqueta(solicitud.getCategoria()));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Error al comunicarse con ms-ubicacion.");
        }
    }

    // ---------- helpers ----------

    private Solicitud cambiarEstado(Solicitud solicitud, EstadoSolicitud estado) {
        solicitud.setEstadoSolicitud(estado);
        solicitud.setFechaActualizacion(LocalDateTime.now());
        return solicitudRepository.save(solicitud);
    }

    private static void exigirEstado(Solicitud solicitud, EstadoSolicitud esperado, String mensaje) {
        if (solicitud.getEstadoSolicitud() != esperado) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, mensaje);
        }
    }

    private Map<String, Object> obtenerTecnico(Long tecnicoId) {
        try {
            return usuariosFeignClient.obtenerTecnico(tecnicoId);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario " + tecnicoId + " no es técnico.");
        } catch (Exception e) {
            log.error("No se pudo consultar ms-usuarios para el técnico {}: {}", tecnicoId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo validar al técnico. Intenta más tarde.");
        }
    }

    private void publicarCambioEstado(Solicitud s, String mensaje) {
        List<Long> tecnicos = s.getTecnicoId() != null ? List.of(s.getTecnicoId()) : List.of();
        publicar(MessagingConstants.RK_SOLICITUD_ESTADO_ACTUALIZADO, s, tecnicos, "CAMBIO_ESTADO", mensaje);
    }

    /**
     * Destinatarios de la notificación: en una solicitud nueva, los técnicos cercanos;
     * en el resto, el cliente y el técnico asignado.
     */
    private void publicar(String routingKey, Solicitud s, List<Long> tecnicosIds, String tipo, String mensaje) {
        List<Long> destinatarios = new ArrayList<>(tecnicosIds);
        if (!MessagingConstants.RK_SOLICITUD_CREADA.equals(routingKey)) {
            destinatarios.add(s.getClienteId());
        }
        eventPublisher.publicar(routingKey, NotificacionEventDTO.builder()
                .solicitudId(s.getId())
                .clienteId(s.getClienteId())
                .tecnicosIds(tecnicosIds)
                .destinatariosIds(destinatarios)
                .tipoServicio(tipo)
                .estado(s.getEstadoSolicitud().name())
                .categoria(s.getCategoria())
                .monto(s.getPrecioEstimado())
                .mensaje(mensaje)
                .build());
    }

    /** La especialidad del técnico debe coincidir con la categoría de la solicitud. */
    private static boolean mismaEspecialidad(String especialidad, String categoria) {
        return etiqueta(especialidad).equalsIgnoreCase(etiqueta(categoria));
    }

    private static String etiqueta(String categoria) {
        return categoria != null ? categoria : "otros";
    }

    private static String texto(Object valor) {
        return valor != null ? valor.toString() : null;
    }

    private static Double numero(Object valor) {
        return valor instanceof Number n ? n.doubleValue() : null;
    }
}
