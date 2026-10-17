package com.homefixer.solicitudes.controller;

import com.homefixer.shared.dto.NotificacionEventDTO;
import com.homefixer.shared.messaging.EventPublisher;
import com.homefixer.shared.messaging.MessagingConstants;
import com.homefixer.solicitudes.entity.MensajeChat;
import com.homefixer.solicitudes.entity.Solicitud;
import com.homefixer.solicitudes.repository.MensajeChatRepository;
import com.homefixer.solicitudes.service.SolicitudService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/** Chat cliente ↔ técnico de una solicitud. */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final MensajeChatRepository mensajeChatRepository;
    private final SolicitudService solicitudService;
    private final EventPublisher eventPublisher;

    @GetMapping("/{solicitudId}/mensajes")
    public ResponseEntity<List<MensajeChat>> listar(@PathVariable("solicitudId") Long solicitudId) {
        return ResponseEntity.ok(mensajeChatRepository.findBySolicitudIdOrderByCreadoEnAsc(solicitudId));
    }

    @PostMapping("/{solicitudId}/mensajes")
    public ResponseEntity<MensajeChat> enviar(@PathVariable("solicitudId") Long solicitudId,
                                              @RequestBody MensajeChat mensaje) {
        Solicitud solicitud = solicitudService.getSolicitudById(solicitudId);
        Long remitente = mensaje.getRemitenteId();
        if (remitente == null || (!remitente.equals(solicitud.getClienteId()) && !remitente.equals(solicitud.getTecnicoId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el cliente y el técnico de la solicitud pueden escribir.");
        }
        if (mensaje.getTexto() == null || mensaje.getTexto().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El mensaje está vacío.");
        }
        MensajeChat nuevo = MensajeChat.builder()
                .solicitudId(solicitudId)
                .remitenteId(remitente)
                .remitenteNombre(mensaje.getRemitenteNombre() != null ? mensaje.getRemitenteNombre() : "Usuario")
                .esCliente(remitente.equals(solicitud.getClienteId()))
                .texto(mensaje.getTexto().trim())
                .creadoEn(LocalDateTime.now())
                .build();
        MensajeChat guardado = mensajeChatRepository.save(nuevo);

        // Evento asíncrono: ms-notificacion avisa a la otra parte del chat
        Long destinatario = guardado.getEsCliente() ? solicitud.getTecnicoId() : solicitud.getClienteId();
        if (destinatario != null) {
            eventPublisher.publicar(MessagingConstants.RK_CHAT_MENSAJE, NotificacionEventDTO.builder()
                    .solicitudId(solicitudId)
                    .clienteId(solicitud.getClienteId())
                    .destinatariosIds(List.of(destinatario))
                    .tipoServicio("CHAT_MENSAJE")
                    .estado(solicitud.getEstadoSolicitud().name())
                    .mensaje(guardado.getRemitenteNombre() + ": " + recortar(guardado.getTexto()))
                    .build());
        }
        return new ResponseEntity<>(guardado, HttpStatus.CREATED);
    }

    private static String recortar(String texto) {
        return texto.length() > 120 ? texto.substring(0, 117) + "..." : texto;
    }
}
