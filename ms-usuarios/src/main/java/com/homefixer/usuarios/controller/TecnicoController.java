package com.homefixer.usuarios.controller;

import com.homefixer.usuarios.entity.Tecnico;
import com.homefixer.usuarios.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tecnicos")
@RequiredArgsConstructor
public class TecnicoController {

    private final UsuarioService usuarioService;

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

    @GetMapping("/validados")
    public ResponseEntity<List<Tecnico>> getTecnicosValidados() {
        return ResponseEntity.ok(usuarioService.getAllTecnicosValidados());
    }
}
