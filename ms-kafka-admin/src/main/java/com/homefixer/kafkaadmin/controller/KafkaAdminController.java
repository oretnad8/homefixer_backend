package com.homefixer.kafkaadmin.controller;

import com.homefixer.kafkaadmin.dto.AdminDtos.ActualizarConfigRequest;
import com.homefixer.kafkaadmin.dto.AdminDtos.AmpliarParticionesRequest;
import com.homefixer.kafkaadmin.dto.AdminDtos.ClusterInfo;
import com.homefixer.kafkaadmin.dto.AdminDtos.ConfigValor;
import com.homefixer.kafkaadmin.dto.AdminDtos.CrearTopicoRequest;
import com.homefixer.kafkaadmin.dto.AdminDtos.EliminarGruposRequest;
import com.homefixer.kafkaadmin.dto.AdminDtos.GrupoDetalle;
import com.homefixer.kafkaadmin.dto.AdminDtos.GrupoResumen;
import com.homefixer.kafkaadmin.dto.AdminDtos.MensajeDlt;
import com.homefixer.kafkaadmin.dto.AdminDtos.ResetOffsetsRequest;
import com.homefixer.kafkaadmin.dto.AdminDtos.ResultadoReset;
import com.homefixer.kafkaadmin.dto.AdminDtos.ResultadoSimulacion;
import com.homefixer.kafkaadmin.dto.AdminDtos.TopicoDetalle;
import com.homefixer.kafkaadmin.dto.AdminDtos.TopicoResumen;
import com.homefixer.kafkaadmin.service.DltAuditoriaService;
import com.homefixer.kafkaadmin.service.KafkaGruposService;
import com.homefixer.kafkaadmin.service.KafkaTopicosService;
import com.homefixer.kafkaadmin.service.SimuladorCargaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * API de administración de Kafka. Por el api-gateway: /api/kafka/admin/** (JWT con ROLE_ADMIN).
 *
 * <pre>
 *  Clúster          GET    /cluster
 *  Tópicos          GET    /topics[?internos=true]
 *                   GET    /topics/{t}[?todasLasConfigs=true]
 *                   POST   /topics
 *                   GET    /topics/{t}/config
 *                   PUT    /topics/{t}/config
 *                   POST   /topics/{t}/partitions
 *                   DELETE /topics/{t}[?forzar=true]
 *  Consumer groups  GET    /consumer-groups
 *                   GET    /consumer-groups/{g}            (miembros, asignación y lag por partición)
 *                   POST   /consumer-groups/{g}/reset-offsets
 *                   DELETE /consumer-groups/{g}
 *  DLT              GET    /dlt[?topico=&amp;limite=]
 *  Demo de carga    POST   /simular?eventos=N
 * </pre>
 */
@RestController
@RequestMapping("/api/kafka/admin")
@RequiredArgsConstructor
public class KafkaAdminController {

    private final KafkaTopicosService topicos;
    private final KafkaGruposService grupos;
    private final DltAuditoriaService dlt;
    private final SimuladorCargaService simulador;

    // ---------------- Clúster ----------------

    @GetMapping("/cluster")
    public ClusterInfo cluster() {
        return topicos.cluster();
    }

    // ---------------- Tópicos ----------------

    @GetMapping("/topics")
    public List<TopicoResumen> listarTopicos(@RequestParam(defaultValue = "false") boolean internos) {
        return topicos.listar(internos);
    }

    @GetMapping("/topics/{nombre}")
    public TopicoDetalle detalleTopico(@PathVariable String nombre,
                                       @RequestParam(defaultValue = "false") boolean todasLasConfigs) {
        return topicos.detalle(nombre, todasLasConfigs);
    }

    @PostMapping("/topics")
    public ResponseEntity<TopicoDetalle> crearTopico(@Valid @RequestBody CrearTopicoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(topicos.crear(req));
    }

    @GetMapping("/topics/{nombre}/config")
    public Map<String, ConfigValor> configTopico(@PathVariable String nombre,
                                                 @RequestParam(defaultValue = "false") boolean todas) {
        return topicos.configs(nombre, todas);
    }

    @PutMapping("/topics/{nombre}/config")
    public Map<String, ConfigValor> actualizarConfig(@PathVariable String nombre, @RequestBody ActualizarConfigRequest req) {
        return topicos.actualizarConfigs(nombre, req);
    }

    @PostMapping("/topics/{nombre}/partitions")
    public TopicoDetalle ampliarParticiones(@PathVariable String nombre, @Valid @RequestBody AmpliarParticionesRequest req) {
        return topicos.ampliarParticiones(nombre, req.total());
    }

    @DeleteMapping("/topics/{nombre}")
    public Map<String, Object> eliminarTopico(@PathVariable String nombre,
                                              @RequestParam(defaultValue = "false") boolean forzar) {
        topicos.eliminar(nombre, forzar);
        return Map.of("topico", nombre, "eliminado", true);
    }

    // ---------------- Consumer groups ----------------

    @GetMapping("/consumer-groups")
    public List<GrupoResumen> listarGrupos() {
        return grupos.listar();
    }

    @GetMapping("/consumer-groups/{grupo}")
    public GrupoDetalle detalleGrupo(@PathVariable String grupo) {
        return grupos.detalle(grupo);
    }

    @PostMapping("/consumer-groups/{grupo}/reset-offsets")
    public ResultadoReset resetOffsets(@PathVariable String grupo, @Valid @RequestBody ResetOffsetsRequest req) {
        return grupos.resetOffsets(grupo, req);
    }

    @DeleteMapping("/consumer-groups/{grupo}")
    public Map<String, Object> eliminarGrupo(@PathVariable String grupo) {
        grupos.eliminar(List.of(grupo));
        return Map.of("grupo", grupo, "eliminado", true);
    }

    @PostMapping("/consumer-groups/eliminar")
    public Map<String, Object> eliminarGrupos(@Valid @RequestBody EliminarGruposRequest req) {
        grupos.eliminar(req.grupos());
        return Map.of("grupos", req.grupos(), "eliminados", true);
    }

    // ---------------- DLT y demo ----------------

    @GetMapping("/dlt")
    public List<MensajeDlt> dlt(@RequestParam(required = false) String topico,
                                @RequestParam(defaultValue = "50") int limite) {
        return dlt.ultimos(topico, limite);
    }

    @PostMapping("/simular")
    public ResponseEntity<ResultadoSimulacion> simular(@RequestParam(defaultValue = "600") int eventos) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(simulador.simular(eventos));
    }
}
