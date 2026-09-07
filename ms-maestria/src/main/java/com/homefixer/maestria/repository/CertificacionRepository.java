package com.homefixer.maestria.repository;

import com.homefixer.maestria.entity.Certificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CertificacionRepository extends JpaRepository<Certificacion, Long> {
    List<Certificacion> findByTecnicoId(Long tecnicoId);
}
