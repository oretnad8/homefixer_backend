package com.homefixer.valoraciones.repository;

import com.homefixer.valoraciones.entity.Valoracion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ValoracionRepository extends JpaRepository<Valoracion, Long> {
    List<Valoracion> findByTecnicoId(Long tecnicoId);
    List<Valoracion> findBySolicitudId(Long solicitudId);

    /** Valoraciones hechas por clientes a un técnico (autorRol null = filas antiguas, de clientes). */
    @Query("SELECT v FROM Valoracion v WHERE v.tecnicoId = :tecnicoId AND (v.autorRol IS NULL OR v.autorRol = 'cliente')")
    List<Valoracion> findDeClientesAlTecnico(@Param("tecnicoId") Long tecnicoId);

    @Query("SELECT COUNT(v) > 0 FROM Valoracion v WHERE v.solicitudId = :solicitudId "
            + "AND COALESCE(v.autorRol, 'cliente') = :autorRol")
    boolean yaValorada(@Param("solicitudId") Long solicitudId, @Param("autorRol") String autorRol);
}
