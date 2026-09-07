package com.homefixer.valoraciones.service;

import com.homefixer.valoraciones.client.UsuarioFeignClient;
import com.homefixer.valoraciones.entity.Valoracion;
import com.homefixer.valoraciones.repository.ValoracionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ValoracionService {
    private final ValoracionRepository valoracionRepository;
    private final UsuarioFeignClient usuarioFeignClient;

    public Valoracion registrarValoracion(Valoracion valoracion) {
        Valoracion guardada = valoracionRepository.save(valoracion);
        
        // Recalcular promedio del tecnico
        List<Valoracion> valoraciones = valoracionRepository.findByTecnicoId(valoracion.getTecnicoId());
        double promedio = valoraciones.stream()
                .mapToInt(Valoracion::getPuntuacion)
                .average()
                .orElse(0.0);
                
        // Actualizar tecnico vía Feign
        try {
            usuarioFeignClient.actualizarCalificacion(valoracion.getTecnicoId(), promedio);
        } catch (Exception e) {
            // Log error
        }
        
        return guardada;
    }

    public List<Valoracion> getValoracionesByTecnico(Long tecnicoId) {
        return valoracionRepository.findByTecnicoId(tecnicoId);
    }
}
