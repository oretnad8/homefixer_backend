package com.homefixer.usuarios.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homefixer.usuarios.client.SupabaseAdminClient;
import com.homefixer.usuarios.entity.Cliente;
import com.homefixer.usuarios.entity.Usuario;
import com.homefixer.usuarios.repository.AdministradorRepository;
import com.homefixer.usuarios.repository.ClienteRepository;
import com.homefixer.usuarios.repository.TecnicoRepository;
import com.homefixer.usuarios.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Crea en la BD local el usuario autenticado en Supabase (email/contraseña o Google)
 * la primera vez que inicia sesión, y devuelve su id numérico y sus roles.
 *
 * La firma del JWT ya la valida el api-gateway (oauth2 resource server), por eso aquí
 * solo se leen los claims del payload.
 *
 * Los correos de homefixer.admin.emails (ADMIN_EMAILS en .env) quedan solo con el rol administrador.
 */
@Slf4j
@Service
public class SincronizacionService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final TecnicoRepository tecnicoRepository;
    private final AdministradorRepository administradorRepository;
    private final ObjectMapper objectMapper;
    private final Set<String> emailsAdmin;

    public SincronizacionService(UsuarioRepository usuarioRepository, ClienteRepository clienteRepository,
                                 TecnicoRepository tecnicoRepository, AdministradorRepository administradorRepository,
                                 ObjectMapper objectMapper, @Value("${homefixer.admin.emails:}") String emailsAdmin) {
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.tecnicoRepository = tecnicoRepository;
        this.administradorRepository = administradorRepository;
        this.objectMapper = objectMapper;
        this.emailsAdmin = Arrays.stream(emailsAdmin.split(","))
                .map(String::trim).map(String::toLowerCase).filter(e -> !e.isEmpty())
                .collect(Collectors.toSet());
    }

    @Transactional
    public Map<String, Object> sincronizar(String authorizationHeader) {
        JsonNode claims = leerClaims(authorizationHeader);
        String email = emailDe(claims);
        JsonNode metadata = claims.path("user_metadata");

        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        if (usuario == null) {
            Cliente nuevo = new Cliente();
            nuevo.setEmail(email);
            nuevo.setNombre(nombreCompleto(metadata, email));
            nuevo.setTelefono(valorOVacio(texto(metadata, "telefono")));
            nuevo.setPassword(SupabaseAdminClient.PASSWORD_GESTIONADA_POR_SUPABASE);
            usuario = clienteRepository.save(nuevo);

            // Registro desde el portal del técnico: queda solo con el rol técnico (pendiente de validación)
            if ("tecnico".equals(texto(metadata, "rol_solicitado"))) {
                tecnicoRepository.agregarRolTecnico(usuario.getId());
                tecnicoRepository.quitarRolCliente(usuario.getId());
            }
            log.info("Usuario {} ({}) creado desde Supabase", usuario.getId(), email);
        }

        Long id = usuario.getId();
        if (emailsAdmin.contains(email)) {
            // Cuenta de administración: solo ese rol
            administradorRepository.asegurarRolAdmin(id);
            tecnicoRepository.quitarRolTecnico(id);
            tecnicoRepository.quitarRolCliente(id);
        } else if (clienteRepository.existsById(id) && tecnicoRepository.existsById(id)) {
            // Usuarios anteriores a la exclusividad de roles con ambas filas: prevalece el rol técnico
            tecnicoRepository.quitarRolCliente(id);
            log.info("Usuario {} tenía ambos roles; se deja solo como técnico", id);
        }
        return datosYRoles(usuario);
    }

    /**
     * Usuario dueño del token y sus roles, sin crearlo. Lo usan otros servicios
     * (p. ej. ms-maestria) para verificar que quien llama es admin o el propio técnico.
     */
    public Map<String, Object> usuarioDelToken(String authorizationHeader) {
        String email = emailDe(leerClaims(authorizationHeader));
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no registrado."));
        return datosYRoles(usuario);
    }

    private Map<String, Object> datosYRoles(Usuario usuario) {
        Long id = usuario.getId();
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("id", id);
        respuesta.put("nombre", usuario.getNombre());
        respuesta.put("email", usuario.getEmail());
        respuesta.put("telefono", usuario.getTelefono());
        respuesta.put("esCliente", clienteRepository.existsById(id));
        respuesta.put("esTecnico", tecnicoRepository.existsById(id));
        respuesta.put("esAdmin", administradorRepository.existsById(id));
        return respuesta;
    }

    private static String emailDe(JsonNode claims) {
        String email = texto(claims, "email");
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El token no contiene email.");
        }
        return email.toLowerCase();
    }

    private JsonNode leerClaims(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token faltante.");
        }
        try {
            String payload = authorizationHeader.substring(7).split("\\.")[1];
            return objectMapper.readTree(new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token inválido.");
        }
    }

    /** Registro propio: nombre + apellido. Google: full_name / name. */
    private static String nombreCompleto(JsonNode metadata, String email) {
        String nombre = texto(metadata, "nombre");
        if (nombre != null) {
            String apellido = texto(metadata, "apellido");
            return apellido != null ? nombre + " " + apellido : nombre;
        }
        String fullName = texto(metadata, "full_name");
        if (fullName == null) fullName = texto(metadata, "name");
        return fullName != null ? fullName : email.substring(0, email.indexOf('@'));
    }

    private static String texto(JsonNode node, String campo) {
        JsonNode valor = node.path(campo);
        return valor.isTextual() && !valor.asText().isBlank() ? valor.asText().trim() : null;
    }

    private static String valorOVacio(String valor) {
        return valor != null ? valor : "";
    }
}
