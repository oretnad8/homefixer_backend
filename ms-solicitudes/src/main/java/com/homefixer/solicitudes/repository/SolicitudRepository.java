package com.homefixer.solicitudes.repository;

import com.homefixer.solicitudes.entity.Solicitud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    List<Solicitud> findByClienteId(Long clienteId);
    List<Solicitud> findByTecnicoId(Long tecnicoId);
}
