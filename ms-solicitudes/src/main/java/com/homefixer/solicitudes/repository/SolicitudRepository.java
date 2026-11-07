package com.homefixer.solicitudes.repository;

import com.homefixer.shared.enums.EstadoSolicitud;
import com.homefixer.solicitudes.entity.Solicitud;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    List<Solicitud> findByClienteIdOrderByFechaDesc(Long clienteId);

    Optional<Solicitud> findByClaveIdempotencia(String claveIdempotencia);

    /**
     * Bloquea la fila hasta el fin de la transacción: si dos técnicos aceptan a la vez,
     * el segundo espera y luego ve la solicitud ya ACEPTADA (no se pisan).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Solicitud s WHERE s.id = :id")
    Optional<Solicitud> findByIdParaActualizar(@Param("id") Long id);
    List<Solicitud> findByTecnicoIdOrderByFechaDesc(Long tecnicoId);
    long countByTecnicoIdAndEstadoSolicitudIn(Long tecnicoId, Collection<EstadoSolicitud> estados);

    /** Solicitudes sin técnico que el técnico no ha descartado. */
    @Query("SELECT s FROM Solicitud s WHERE s.estadoSolicitud = com.homefixer.shared.enums.EstadoSolicitud.CREADA "
            + "AND s.tecnicoId IS NULL AND s.clienteId <> :tecnicoId "
            + "AND NOT EXISTS (SELECT r FROM SolicitudRechazo r WHERE r.solicitudId = s.id AND r.tecnicoId = :tecnicoId) "
            + "ORDER BY s.fecha DESC")
    List<Solicitud> findDisponiblesPara(@Param("tecnicoId") Long tecnicoId);

    @Query("SELECT s.estadoSolicitud, COUNT(s) FROM Solicitud s GROUP BY s.estadoSolicitud")
    List<Object[]> contarPorEstado();
}
