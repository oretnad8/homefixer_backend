package com.homefixer.usuarios.repository;

import com.homefixer.usuarios.entity.Tecnico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TecnicoRepository extends JpaRepository<Tecnico, Long> {
    List<Tecnico> findByValidadoTrue();
    List<Tecnico> findByValidadoTrueAndEspecialidad(String especialidad);

    // Los roles son excluyentes: un usuario tiene fila en clientes O en tecnicos, nunca en ambas.
    // El cambio de rol mueve solo la fila hija (SQL nativo) para no tocar la fila de usuarios.

    @Modifying
    @Query(value = "INSERT INTO tecnicos (id, validado, calificacion_promedio, nivel_reputacion, especialidad, precio_base) "
            + "VALUES (:id, false, 0.0, 'NUEVO', 'otros', 25000.0)", nativeQuery = true)
    void agregarRolTecnico(@Param("id") Long usuarioId);

    @Modifying
    @Query(value = "DELETE FROM tecnicos WHERE id = :id", nativeQuery = true)
    void quitarRolTecnico(@Param("id") Long usuarioId);

    @Modifying
    @Query(value = "INSERT INTO clientes (id) VALUES (:id) ON CONFLICT (id) DO NOTHING", nativeQuery = true)
    void asegurarRolCliente(@Param("id") Long usuarioId);

    @Modifying
    @Query(value = "DELETE FROM clientes WHERE id = :id", nativeQuery = true)
    void quitarRolCliente(@Param("id") Long usuarioId);
}
