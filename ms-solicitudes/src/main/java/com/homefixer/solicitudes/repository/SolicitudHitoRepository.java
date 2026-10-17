package com.homefixer.solicitudes.repository;

import com.homefixer.solicitudes.entity.SolicitudHito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudHitoRepository extends JpaRepository<SolicitudHito, Long> {
    List<SolicitudHito> findBySolicitudIdOrderByFechaAsc(Long solicitudId);
    boolean existsBySolicitudIdAndHito(Long solicitudId, String hito);
}
