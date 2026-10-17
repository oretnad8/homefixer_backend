package com.homefixer.ubicacion.client;

import com.homefixer.ubicacion.dto.InsigniaInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collection;
import java.util.List;

@FeignClient(name = "ms-maestria")
public interface MaestriaFeignClient {
    /** Insignias de maestría de varios técnicos en una sola llamada. */
    @GetMapping("/api/maestria/insignias/lote")
    List<InsigniaInfo> obtenerInsignias(@RequestParam("ids") Collection<Long> ids);
}
