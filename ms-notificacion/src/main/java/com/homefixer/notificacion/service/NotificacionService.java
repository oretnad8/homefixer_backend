package com.homefixer.notificacion.service;

import com.homefixer.notificacion.entity.Notificacion;
import com.homefixer.notificacion.repository.NotificacionRepository;
import com.homefixer.shared.dto.NotificacionEventDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;

    /** Crea una notificación por destinatario del evento. Idempotente por (eventoId, usuarioId). */
    @Transactional
    public int registrar(NotificacionEventDTO evento) {
        int creadas = 0;
        for (Long usuarioId : destinatarios(evento)) {
            if (notificacionRepository.existsByEventoIdAndUsuarioId(evento.getEventoId(), usuarioId)) {
                continue;
            }
            notificacionRepository.save(Notificacion.builder()
                    .usuarioId(usuarioId)
                    .eventoId(evento.getEventoId())
                    .solicitudId(evento.getSolicitudId())
                    .tipo(evento.getTipoServicio() != null ? evento.getTipoServicio() : "GENERAL")
                    .titulo(titulo(evento))
                    .detalle(evento.getMensaje() != null ? evento.getMensaje() : "")
                    .leida(false)
                    .creadoEn(evento.getFechaEvento() != null ? evento.getFechaEvento() : LocalDateTime.now())
                    .build());
            creadas++;
        }
        return creadas;
    }

    public List<Notificacion> deUsuario(Long usuarioId) {
        return notificacionRepository.findTop50ByUsuarioIdOrderByCreadoEnDesc(usuarioId);
    }

    @Transactional
    public void marcarLeida(Long id) {
        Notificacion n = notificacionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificación no encontrada."));
        n.setLeida(true);
        notificacionRepository.save(n);
    }

    @Transactional
    public int marcarTodasLeidas(Long usuarioId) {
        return notificacionRepository.marcarTodasLeidas(usuarioId);
    }

    /** Eventos antiguos sin destinatarios explícitos: cliente y técnicos. */
    private static Set<Long> destinatarios(NotificacionEventDTO e) {
        Set<Long> ids = new LinkedHashSet<>();
        if (e.getDestinatariosIds() != null) {
            ids.addAll(e.getDestinatariosIds());
        } else {
            if (e.getClienteId() != null) ids.add(e.getClienteId());
            if (e.getTecnicosIds() != null) ids.addAll(e.getTecnicosIds());
        }
        ids.remove(null);
        return ids;
    }

    private static String titulo(NotificacionEventDTO e) {
        String tipo = e.getTipoServicio() != null ? e.getTipoServicio() : "";
        return switch (tipo) {
            case "NUEVA_SOLICITUD" -> "Nueva solicitud cerca de ti";
            case "TECNICO_ASIGNADO" -> "Técnico asignado";
            case "TECNICO_LLEGO" -> "Tu técnico llegó";
            case "CAMBIO_ESTADO" -> switch (e.getEstado() != null ? e.getEstado() : "") {
                case "EN_PROCESO" -> "Trabajo iniciado";
                case "COMPLETADA" -> "Servicio completado";
                case "CANCELADA" -> "Solicitud cancelada";
                default -> "Actualización de tu solicitud";
            };
            case "PAGO_PROCESADO" -> "Pago recibido";
            case "VALORACION_TECNICO", "VALORACION_CLIENTE" -> "Nueva valoración";
            case "CHAT_MENSAJE" -> "Nuevo mensaje";
            case "CERTIFICACION_SOLICITADA" -> "Certificación por revisar";
            case "CERTIFICACION_APROBADA" -> "¡Nueva insignia de maestría!";
            case "CERTIFICACION_RECHAZADA" -> "Certificación rechazada";
            default -> "HomeFixer";
        };
    }
}
