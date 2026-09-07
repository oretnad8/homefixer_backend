package com.homefixer.valoraciones.repository;

import com.homefixer.valoraciones.entity.Valoracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ValoracionRepository extends JpaRepository<Valoracion, Long> {
    List<Valoracion> findByTecnicoId(Long tecnicoId);
}
