package com.homefixer.usuarios.service;

import com.homefixer.usuarios.client.SolicitudesFeignClient;
import com.homefixer.usuarios.repository.ClienteRepository;
import com.homefixer.usuarios.repository.TecnicoRepository;
import com.homefixer.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** Cambio de rol cliente ⇄ técnico (los roles son excluyentes). */
@Slf4j
@Service
@RequiredArgsConstructor
public class CambioRolService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final TecnicoRepository tecnicoRepository;
    private final SolicitudesFeignClient solicitudesFeignClient;

    public Map<String, Object> obtenerRoles(Long usuarioId) {
        verificarExiste(usuarioId);
        return Map.of(
                "usuarioId", usuarioId,
                "esCliente", clienteRepository.existsById(usuarioId),
                "esTecnico", tecnicoRepository.existsById(usuarioId));
    }

    @Transactional
    public void postularComoTecnico(Long usuarioId) {
        verificarExiste(usuarioId);
        // Idempotente: si ya era técnico (p. ej. cuentas con ambos roles) solo se quita el de cliente
        if (!tecnicoRepository.existsById(usuarioId)) {
            tecnicoRepository.agregarRolTecnico(usuarioId);
        }
        tecnicoRepository.quitarRolCliente(usuarioId); // ya no puede entrar como cliente
        log.info("Usuario {} registrado como técnico (pendiente de validación)", usuarioId);
    }

    @Transactional
    public void renunciarComoTecnico(Long usuarioId) {
        if (!tecnicoRepository.existsById(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No estás registrado como técnico.");
        }

        long activas;
        try {
            activas = solicitudesFeignClient.contarSolicitudesActivas(usuarioId);
        } catch (Exception e) {
            log.error("No se pudo consultar ms-solicitudes para el técnico {}: {}", usuarioId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudieron verificar tus trabajos activos. Intenta más tarde.");
        }
        if (activas > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tienes " + activas + " trabajo(s) activo(s). Finalízalos antes de renunciar.");
        }

        tecnicoRepository.quitarRolTecnico(usuarioId);
        tecnicoRepository.asegurarRolCliente(usuarioId); // vuelve a entrar como cliente
        log.info("Usuario {} renunció al rol técnico", usuarioId);
    }

    private void verificarExiste(Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado.");
        }
    }
}
