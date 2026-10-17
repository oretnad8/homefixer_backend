package com.homefixer.maestria.repository;

import com.homefixer.maestria.entity.Certificacion;
import com.homefixer.maestria.entity.EstadoCertificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CertificacionRepository extends JpaRepository<Certificacion, Long> {
    List<Certificacion> findByTecnicoIdOrderByFechaSolicitudDesc(Long tecnicoId);
    List<Certificacion> findByEstadoOrderByFechaSolicitudAsc(EstadoCertificacion estado);
    boolean existsByTecnicoIdAndCategoriaAndEstado(Long tecnicoId, String categoria, EstadoCertificacion estado);
}
