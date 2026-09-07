package com.homefixer.usuarios.repository;

import com.homefixer.usuarios.entity.Tecnico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TecnicoRepository extends JpaRepository<Tecnico, Long> {
    List<Tecnico> findByValidadoTrue();
}
