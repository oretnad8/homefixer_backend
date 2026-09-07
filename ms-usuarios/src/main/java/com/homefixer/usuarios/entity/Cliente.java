package com.homefixer.usuarios.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clientes")
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Cliente extends Usuario {
    // Cliente specific fields can go here
}
