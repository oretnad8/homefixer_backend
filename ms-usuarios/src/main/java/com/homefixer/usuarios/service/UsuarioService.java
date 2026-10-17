package com.homefixer.usuarios.service;

import com.homefixer.usuarios.entity.Cliente;
import com.homefixer.usuarios.entity.Tecnico;
import com.homefixer.usuarios.entity.Usuario;

import java.util.List;

public interface UsuarioService {
    Usuario getUsuarioById(Long id);
    Tecnico getTecnicoById(Long id);
    Cliente createCliente(Cliente cliente);
    Tecnico createTecnico(Tecnico tecnico);
    Cliente updateCliente(Long id, Cliente cliente);
    Tecnico updateTecnico(Long id, Tecnico tecnico);
    void validateTecnico(Long id);
    void actualizarDisponibilidad(Long id, boolean disponible);
    Tecnico actualizarPerfilProfesional(Long id, String especialidad, Double precioBase);
    List<Tecnico> getTecnicosByIds(List<Long> ids);
    /** Guarda el nuevo promedio y recalcula el nivel de reputación (NUEVO, BUENO, EXPERTO). */
    void actualizarCalificacion(Long id, Double calificacion, Integer totalValoraciones);
    List<Tecnico> getAllTecnicosValidados();
    List<Usuario> getAllUsuarios();
}
