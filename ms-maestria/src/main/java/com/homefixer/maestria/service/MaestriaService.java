package com.homefixer.maestria.service;

import com.homefixer.maestria.entity.Certificacion;
import com.homefixer.maestria.entity.InsigniaMaestria;
import com.homefixer.maestria.repository.CertificacionRepository;
import com.homefixer.maestria.repository.InsigniaMaestriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaestriaService {
    private final CertificacionRepository certificacionRepository;
    private final InsigniaMaestriaRepository insigniaMaestriaRepository;

    public Certificacion agregarCertificacion(Certificacion certificacion) {
        certificacion.setVerificado(false); // Por defecto requiere validación manual
        return certificacionRepository.save(certificacion);
    }

    public InsigniaMaestria otorgarInsignia(InsigniaMaestria insignia) {
        return insigniaMaestriaRepository.save(insignia);
    }

    public List<Certificacion> getCertificaciones(Long tecnicoId) {
        return certificacionRepository.findByTecnicoId(tecnicoId);
    }

    public List<InsigniaMaestria> getInsignias(Long tecnicoId) {
        return insigniaMaestriaRepository.findByTecnicoId(tecnicoId);
    }
}
