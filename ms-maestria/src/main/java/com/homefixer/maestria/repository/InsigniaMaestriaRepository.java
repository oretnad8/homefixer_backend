package com.homefixer.maestria.repository;

import com.homefixer.maestria.entity.InsigniaMaestria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InsigniaMaestriaRepository extends JpaRepository<InsigniaMaestria, Long> {
    List<InsigniaMaestria> findByTecnicoId(Long tecnicoId);
}
