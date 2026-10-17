package com.homefixer.ubicacion.client;

import com.homefixer.ubicacion.dto.TecnicoInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

@FeignClient(name = "ms-usuarios")
public interface UsuariosFeignClient {
    /** Datos de varios técnicos en una sola llamada; los ids que no son técnicos se omiten. */
    @GetMapping("/api/tecnicos/lote")
    List<TecnicoInfo> obtenerTecnicos(@RequestParam("ids") Collection<Long> ids);
}
