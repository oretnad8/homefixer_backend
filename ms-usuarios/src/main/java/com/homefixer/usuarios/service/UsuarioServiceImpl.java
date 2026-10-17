package com.homefixer.usuarios.service;

import com.homefixer.usuarios.client.SupabaseAdminClient;
import com.homefixer.usuarios.entity.Cliente;
import com.homefixer.usuarios.entity.Tecnico;
import com.homefixer.usuarios.entity.Usuario;
import com.homefixer.usuarios.repository.ClienteRepository;
import com.homefixer.usuarios.repository.TecnicoRepository;
import com.homefixer.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    /** Mismas categorías que las solicitudes (ms-solicitudes / front). */
    private static final Set<String> ESPECIALIDADES = Set.of(
            "plomeria", "electricidad", "carpinteria", "pintura", "cerrajeria", "limpieza", "jardineria", "refrigeracion", "otros");

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final TecnicoRepository tecnicoRepository;
    private final SupabaseAdminClient supabaseAdminClient;

    @Override
    public Usuario getUsuarioById(Long id) {
        return usuarioRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
    }

    @Override
    public Tecnico getTecnicoById(Long id) {
        return tecnicoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario " + id + " no es técnico."));
    }

    @Override
    @Transactional
    public Cliente createCliente(Cliente cliente) {
        String password = cliente.getPassword();
        prepararCredenciales(cliente);
        Cliente guardado = clienteRepository.save(cliente);
        crearCuentaSupabase(guardado, password);
        return guardado;
    }

    @Override
    @Transactional
    public Tecnico createTecnico(Tecnico tecnico) {
        tecnico.setValidado(false);
        tecnico.setCalificacionPromedio(0.0);
        tecnico.setNivelReputacion("NUEVO");
        String password = tecnico.getPassword();
        prepararCredenciales(tecnico);
        Tecnico guardado = tecnicoRepository.save(tecnico);
        crearCuentaSupabase(guardado, password);
        return guardado;
    }

    /** Si Supabase está configurado, la contraseña no se guarda en la BD local. */
    private void prepararCredenciales(Usuario usuario) {
        usuario.setEmail(usuario.getEmail().trim().toLowerCase());
        if (supabaseAdminClient.isConfigurado()) {
            usuario.setPassword(SupabaseAdminClient.PASSWORD_GESTIONADA_POR_SUPABASE);
        }
    }

    /** Crea la cuenta en Supabase Auth; si falla, la excepción revierte el insert local. */
    private void crearCuentaSupabase(Usuario usuario, String password) {
        if (!supabaseAdminClient.isConfigurado()) {
            log.warn("Usuario {} creado solo en la BD local (Supabase no configurado)", usuario.getEmail());
            return;
        }
        supabaseAdminClient.crearCuenta(usuario.getEmail(), password,
                Map.of("nombre", usuario.getNombre(), "telefono", usuario.getTelefono()));
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
    public void actualizarDisponibilidad(Long id, boolean disponible) {
        Tecnico tecnico = tecnicoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Técnico no encontrado."));
        tecnico.setDisponible(disponible);
        tecnicoRepository.save(tecnico);
    }

    @Override
    public Tecnico actualizarPerfilProfesional(Long id, String especialidad, Double precioBase) {
        Tecnico tecnico = tecnicoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Técnico no encontrado."));
        if (especialidad != null) {
            String valor = especialidad.trim().toLowerCase();
            if (!ESPECIALIDADES.contains(valor)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Especialidad no válida: " + especialidad);
            }
            tecnico.setEspecialidad(valor);
        }
        if (precioBase != null) {
            if (precioBase < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El precio base no puede ser negativo.");
            tecnico.setPrecioBase(precioBase);
        }
        return tecnicoRepository.save(tecnico);
    }

    @Override
    public List<Tecnico> getTecnicosByIds(List<Long> ids) {
        return ids == null || ids.isEmpty() ? List.of() : tecnicoRepository.findAllById(ids);
    }

    @Override
    public void actualizarCalificacion(Long id, Double calificacion, Integer totalValoraciones) {
        Tecnico tecnico = tecnicoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Técnico no encontrado."));
        tecnico.setCalificacionPromedio(Math.round(calificacion * 10) / 10.0);
        // Eventos antiguos sin el total: se mantiene el nivel actual
        if (totalValoraciones != null) {
            tecnico.setNivelReputacion(nivelReputacion(calificacion, totalValoraciones));
        }
        tecnicoRepository.save(tecnico);
    }

    /**
     * Nivel de reputación; desde BUENO el técnico puede postular certificaciones de maestría.
     * EXPERTO: 10+ valoraciones y promedio >= 4.5 · BUENO: 3+ valoraciones y promedio >= 4.0 · NUEVO: el resto.
     */
    static String nivelReputacion(double promedio, int total) {
        if (total >= 10 && promedio >= 4.5) return "EXPERTO";
        if (total >= 3 && promedio >= 4.0) return "BUENO";
        return "NUEVO";
    }

    @Override
    public List<Tecnico> getAllTecnicosValidados() {
        return tecnicoRepository.findByValidadoTrue();
    }

    @Override
    public List<Usuario> getAllUsuarios() {
        return usuarioRepository.findAll();
    }
}
