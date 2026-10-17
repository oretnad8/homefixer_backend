package com.homefixer.pagos.repository;

import com.homefixer.pagos.entity.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    Optional<Pago> findFirstBySolicitudIdOrderByIdDesc(Long solicitudId);
    List<Pago> findByClienteIdOrderByFechaDesc(Long clienteId);
}
