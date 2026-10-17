package com.homefixer.ubicacion.service;

import com.homefixer.ubicacion.client.MaestriaFeignClient;
import com.homefixer.ubicacion.client.UsuariosFeignClient;
import com.homefixer.ubicacion.dto.InsigniaInfo;
import com.homefixer.ubicacion.dto.ReporteUbicacion;
import com.homefixer.ubicacion.dto.TecnicoCercanoDTO;
import com.homefixer.ubicacion.dto.TecnicoInfo;
import com.homefixer.ubicacion.dto.UbicacionDTO;
import com.homefixer.ubicacion.entity.UbicacionTecnico;
import com.homefixer.ubicacion.repository.UbicacionTecnicoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UbicacionService {

    private static final double RADIO_TIERRA_KM = 6371.0;

    private final UbicacionTecnicoRepository repository;
    private final UsuariosFeignClient usuariosFeignClient;
    private final MaestriaFeignClient maestriaFeignClient;

    @Value("${ubicacion.ttl-segundos:120}")
    private long ttlSegundos;

    @Value("${ubicacion.radio-km-defecto:30}")
    private double radioKmDefecto;

    @Transactional
    public void reportar(Long tecnicoId, ReporteUbicacion reporte) {
        repository.save(new UbicacionTecnico(tecnicoId, reporte.latitud(), reporte.longitud(),
                reporte.precision(), Instant.now()));
    }

    /** El técnico se desconecta: deja de aparecer en las búsquedas. */
    @Transactional
    public void desconectar(Long tecnicoId) {
        if (repository.existsById(tecnicoId)) repository.deleteById(tecnicoId);
    }

    public UbicacionDTO obtener(Long tecnicoId) {
        UbicacionTecnico u = repository.findById(tecnicoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El técnico no está compartiendo su ubicación."));
        return new UbicacionDTO(u.getTecnicoId(), u.getLatitud(), u.getLongitud(), u.getPrecisionMetros(),
                u.getActualizadoEn(), u.getActualizadoEn().isAfter(limiteEnLinea()));
    }

    /**
     * Técnicos en línea y disponibles, de la especialidad pedida, dentro del radio. Primero los que
     * tienen insignia de maestría en esa especialidad (en cualquiera, si no se pide una) y luego
     * por distancia (los más cercanos primero).
     */
    public List<TecnicoCercanoDTO> buscarCercanos(double lat, double lng, String categoria, Double radioKm, Long excluirId) {
        double radio = radioKm != null && radioKm > 0 ? radioKm : radioKmDefecto;
        List<UbicacionTecnico> enLinea = repository.findByActualizadoEnAfter(limiteEnLinea()).stream()
                .filter(u -> !u.getTecnicoId().equals(excluirId))
                .filter(u -> distanciaKm(lat, lng, u.getLatitud(), u.getLongitud()) <= radio)
                .toList();
        if (enLinea.isEmpty()) return List.of();

        Map<Long, TecnicoInfo> tecnicos = consultarTecnicos(enLinea);
        Map<Long, List<String>> insignias = consultarInsignias(enLinea);
        String especialidad = normalizar(categoria);
        Comparator<TecnicoCercanoDTO> maestrosPrimero = Comparator.comparing(
                (TecnicoCercanoDTO t) -> especialidad == null ? t.insignias().isEmpty() : !t.insignias().contains(especialidad));

        return enLinea.stream()
                .filter(u -> {
                    TecnicoInfo t = tecnicos.get(u.getTecnicoId());
                    // disponible null = filas antiguas, se considera disponible
                    return t != null && !Boolean.FALSE.equals(t.disponible())
                            && (especialidad == null || especialidad.equals(normalizar(t.especialidad())));
                })
                .map(u -> {
                    TecnicoInfo t = tecnicos.get(u.getTecnicoId());
                    return new TecnicoCercanoDTO(t.id(), t.nombre(), t.telefono(), t.especialidad(),
                            t.calificacionPromedio(), t.precioBase(), t.nivelReputacion(),
                            Boolean.TRUE.equals(t.validado()), u.getLatitud(), u.getLongitud(),
                            redondear(distanciaKm(lat, lng, u.getLatitud(), u.getLongitud())), u.getActualizadoEn(),
                            insignias.getOrDefault(t.id(), List.of()));
                })
                .sorted(maestrosPrimero.thenComparingDouble(TecnicoCercanoDTO::distanciaKm))
                .toList();
    }

    /** Ids de técnicos cercanos para notificar una solicitud nueva; coordenadas = "lat,lng". */
    public List<Long> buscarIdsCercanos(String coordenadas, String categoria) {
        String[] partes = coordenadas == null ? new String[0] : coordenadas.split(",");
        if (partes.length != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordenadas inválidas; formato esperado 'lat,lng'.");
        }
        double lat;
        double lng;
        try {
            lat = Double.parseDouble(partes[0].trim());
            lng = Double.parseDouble(partes[1].trim());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Coordenadas inválidas; formato esperado 'lat,lng'.");
        }
        return buscarCercanos(lat, lng, categoria, null, null).stream().map(TecnicoCercanoDTO::id).toList();
    }

    private Map<Long, TecnicoInfo> consultarTecnicos(List<UbicacionTecnico> ubicaciones) {
        try {
            return usuariosFeignClient.obtenerTecnicos(ubicaciones.stream().map(UbicacionTecnico::getTecnicoId).toList())
                    .stream().collect(Collectors.toMap(TecnicoInfo::id, Function.identity(), (a, b) -> a));
        } catch (Exception e) {
            log.error("No se pudo consultar ms-usuarios: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudieron obtener los datos de los técnicos.");
        }
    }

    /** Oficios con insignia por técnico. Si ms-maestria no responde se busca igual, sin insignias. */
    private Map<Long, List<String>> consultarInsignias(List<UbicacionTecnico> ubicaciones) {
        try {
            return maestriaFeignClient.obtenerInsignias(ubicaciones.stream().map(UbicacionTecnico::getTecnicoId).toList())
                    .stream()
                    .filter(i -> i.categoria() != null)
                    .collect(Collectors.groupingBy(InsigniaInfo::tecnicoId,
                            Collectors.mapping(InsigniaInfo::categoria, Collectors.toList())));
        } catch (Exception e) {
            log.warn("No se pudieron consultar las insignias en ms-maestria: {}", e.getMessage());
            return Map.of();
        }
    }

    private Instant limiteEnLinea() {
        return Instant.now().minus(Duration.ofSeconds(ttlSegundos));
    }

    private static String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim().toLowerCase();
    }

    private static double redondear(double km) {
        return Math.round(km * 100) / 100.0;
    }

    /** Distancia en km entre dos puntos (fórmula de Haversine). */
    static double distanciaKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * RADIO_TIERRA_KM * Math.asin(Math.sqrt(a));
    }
}
