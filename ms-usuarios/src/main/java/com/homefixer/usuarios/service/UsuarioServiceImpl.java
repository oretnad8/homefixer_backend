package com.homefixer.usuarios.service;

import com.homefixer.usuarios.entity.Cliente;
import com.homefixer.usuarios.entity.Tecnico;
import com.homefixer.usuarios.entity.Usuario;
import com.homefixer.usuarios.repository.ClienteRepository;
import com.homefixer.usuarios.repository.TecnicoRepository;
import com.homefixer.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final TecnicoRepository tecnicoRepository;

    @Override
    public Usuario getUsuarioById(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    @Override
    public Cliente createCliente(Cliente cliente) {
        return clienteRepository.save(cliente);
    }

    @Override
    public Tecnico createTecnico(Tecnico tecnico) {
        tecnico.setValidado(false);
        tecnico.setCalificacionPromedio(0.0);
        tecnico.setNivelReputacion("NUEVO");
        return tecnicoRepository.save(tecnico);
    }

    @Override
    public Cliente updateCliente(Long id, Cliente clienteDetails) {
        Cliente cliente = clienteRepository.findById(id).orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        cliente.setNombre(clienteDetails.getNombre());
        cliente.setTelefono(clienteDetails.getTelefono());
        // Do not update email or password directly here for security reasons, unless explicitly required
        return clienteRepository.save(cliente);
    }

    @Override
    public Tecnico updateTecnico(Long id, Tecnico tecnicoDetails) {
        Tecnico tecnico = tecnicoRepository.findById(id).orElseThrow(() -> new RuntimeException("Tecnico no encontrado"));
        tecnico.setNombre(tecnicoDetails.getNombre());
        tecnico.setTelefono(tecnicoDetails.getTelefono());
        return tecnicoRepository.save(tecnico);
    }

    @Override
    public void validateTecnico(Long id) {
        Tecnico tecnico = tecnicoRepository.findById(id).orElseThrow(() -> new RuntimeException("Tecnico no encontrado"));
        tecnico.setValidado(true);
        tecnicoRepository.save(tecnico);
    }

    @Override
    public List<Tecnico> getAllTecnicosValidados() {
        return tecnicoRepository.findByValidadoTrue();
    }
}
