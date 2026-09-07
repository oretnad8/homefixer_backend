package com.homefixer.usuarios.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "administradores")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Administrador extends Usuario {
    // Admin specific fields
}
