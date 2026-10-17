package com.homefixer.usuarios.repository;

import com.homefixer.usuarios.entity.Administrador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdministradorRepository extends JpaRepository<Administrador, Long> {

    // Igual que los otros roles: solo se agrega la fila hija (SQL nativo), la de usuarios no se toca.
    @Modifying
    @Query(value = "INSERT INTO administradores (id) VALUES (:id) ON CONFLICT (id) DO NOTHING", nativeQuery = true)
    void asegurarRolAdmin(@Param("id") Long usuarioId);
}
