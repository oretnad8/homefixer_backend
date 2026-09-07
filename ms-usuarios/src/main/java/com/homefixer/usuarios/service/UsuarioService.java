package com.homefixer.usuarios.service;

import com.homefixer.usuarios.entity.Cliente;
import com.homefixer.usuarios.entity.Tecnico;
import com.homefixer.usuarios.entity.Usuario;

import java.util.List;

public interface UsuarioService {
    Usuario getUsuarioById(Long id);
    Cliente createCliente(Cliente cliente);
    Tecnico createTecnico(Tecnico tecnico);
    Cliente updateCliente(Long id, Cliente cliente);
    Tecnico updateTecnico(Long id, Tecnico tecnico);
    void validateTecnico(Long id);
    List<Tecnico> getAllTecnicosValidados();
}
