package com.homefixer.usuarios.controller;

import com.homefixer.usuarios.entity.Tecnico;
import com.homefixer.usuarios.service.CambioRolService;
import com.homefixer.usuarios.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tecnicos")
@RequiredArgsConstructor
public class TecnicoController {

    private final UsuarioService usuarioService;
    private final CambioRolService cambioRolService;

    @PostMapping
    public ResponseEntity<Tecnico> createTecnico(@RequestBody Tecnico tecnico) {
        return new ResponseEntity<>(usuarioService.createTecnico(tecnico), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Tecnico> updateTecnico(@PathVariable Long id, @RequestBody Tecnico tecnico) {
        return ResponseEntity.ok(usuarioService.updateTecnico(id, tecnico));
    }

    @PatchMapping("/{id}/validar")
    public ResponseEntity<Void> validateTecnico(@PathVariable Long id) {
        usuarioService.validateTecnico(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Datos de técnico de un usuario. Se consulta por la tabla tecnicos: un usuario que
     * también es cliente podría deserializarse como Cliente en /api/usuarios/{id}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Tecnico> getTecnico(@PathVariable("id") Long id) {
        return ResponseEntity.ok(usuarioService.getTecnicoById(id));
    }

    @GetMapping("/validados")
    public ResponseEntity<List<Tecnico>> getTecnicosValidados() {
        return ResponseEntity.ok(usuarioService.getAllTecnicosValidados());
    }

    /** Varios técnicos por id (uso interno de ms-ubicacion); los ids que no son técnicos se omiten. */
    @GetMapping("/lote")
    public ResponseEntity<List<Tecnico>> getTecnicosPorIds(@RequestParam("ids") List<Long> ids) {
        return ResponseEntity.ok(usuarioService.getTecnicosByIds(ids));
    }

    /** Oficio y tarifa del técnico: { "especialidad": "electricidad", "precioBase": 25000 }. */
    @PatchMapping("/{id}/perfil-profesional")
    public ResponseEntity<Tecnico> actualizarPerfilProfesional(@PathVariable("id") Long id, @RequestBody Map<String, Object> body) {
        Object precio = body.get("precioBase");
        return ResponseEntity.ok(usuarioService.actualizarPerfilProfesional(id,
                body.get("especialidad") != null ? body.get("especialidad").toString() : null,
                precio instanceof Number n ? n.doubleValue() : null));
    }

    /** Interruptor de disponibilidad del técnico: { "disponible": true|false }. */
    @PatchMapping("/{id}/disponibilidad")
    public ResponseEntity<Void> actualizarDisponibilidad(@PathVariable("id") Long id, @RequestBody Map<String, Boolean> body) {
        usuarioService.actualizarDisponibilidad(id, Boolean.TRUE.equals(body.get("disponible")));
        return ResponseEntity.noContent().build();
    }

    /** "Trabaja con nosotros": agrega el rol técnico a un usuario existente. */
    @PostMapping("/postular/{usuarioId}")
    public ResponseEntity<Void> postular(@PathVariable("usuarioId") Long usuarioId) {
        cambioRolService.postularComoTecnico(usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** Renuncia al rol técnico; solo si no tiene solicitudes activas asignadas. */
    @DeleteMapping("/{id}/renunciar")
    public ResponseEntity<Void> renunciar(@PathVariable("id") Long id) {
        cambioRolService.renunciarComoTecnico(id);
        return ResponseEntity.noContent().build();
    }
}
