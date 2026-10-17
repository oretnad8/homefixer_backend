package com.homefixer.ubicacion.repository;

import com.homefixer.ubicacion.entity.UbicacionTecnico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface UbicacionTecnicoRepository extends JpaRepository<UbicacionTecnico, Long> {
    /** Técnicos en línea: reportaron posición después de {@code desde}. */
    List<UbicacionTecnico> findByActualizadoEnAfter(Instant desde);
}
