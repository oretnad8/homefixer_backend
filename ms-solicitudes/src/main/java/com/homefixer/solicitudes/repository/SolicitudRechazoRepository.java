package com.homefixer.solicitudes.repository;

import com.homefixer.solicitudes.entity.SolicitudRechazo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitudRechazoRepository extends JpaRepository<SolicitudRechazo, Long> {
    boolean existsBySolicitudIdAndTecnicoId(Long solicitudId, Long tecnicoId);
}
