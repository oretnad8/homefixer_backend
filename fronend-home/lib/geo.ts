// ============================================================
// Geolocalización — 100% gratuita
// ============================================================
// · Posición del dispositivo: Geolocation API del navegador (requiere HTTPS).
// · Dirección ⇄ coordenadas: OpenStreetMap Nominatim (sin API key, uso moderado).
// · Mapas: Leaflet + teselas de OpenStreetMap (components/shared/MapLeaflet).

export interface Punto {
  lat: number;
  lng: number;
}

/** Centro de Santiago: se usa solo para centrar el mapa mientras no hay ubicación. */
export const CENTRO_POR_DEFECTO: Punto = { lat: -33.4372, lng: -70.6506 };

const NOMINATIM = 'https://nominatim.openstreetmap.org';

/** Distancia en km entre dos puntos (Haversine). */
export function distanciaKm(a: Punto, b: Punto): number {
  const rad = (g: number) => (g * Math.PI) / 180;
  const dLat = rad(b.lat - a.lat);
  const dLng = rad(b.lng - a.lng);
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(rad(a.lat)) * Math.cos(rad(b.lat)) * Math.sin(dLng / 2) ** 2;
  return 2 * 6371 * Math.asin(Math.sqrt(h));
}

/** Distancia desde `origen` hasta algo con latitud/longitud; undefined si falta alguno. */
export function distanciaHasta(origen: Punto | null | undefined, destino: { latitud?: number; longitud?: number }): number | undefined {
  if (!origen || destino.latitud == null || destino.longitud == null) return undefined;
  return distanciaKm(origen, { lat: destino.latitud, lng: destino.longitud });
}

/** Ordena por distancia (los que no tienen ubicación, al final). */
export function ordenarPorDistancia<T extends { latitud?: number; longitud?: number }>(lista: T[], origen: Punto | null | undefined): T[] {
  if (!origen) return lista;
  const d = (x: T) => distanciaHasta(origen, x) ?? Number.POSITIVE_INFINITY;
  return [...lista].sort((a, b) => d(a) - d(b));
}

/** "1,2 km" / "350 m" */
export function formatDistancia(km: number): string {
  return km < 1 ? `${Math.max(10, Math.round((km * 1000) / 10) * 10)} m` : `${km.toFixed(1).replace('.', ',')} km`;
}

/** Minutos estimados de viaje en ciudad (~25 km/h promedio). */
export function minutosEstimados(km: number): number {
  return Math.max(1, Math.round((km / 25) * 60));
}

/** "lat, lng" → Punto (formato de Solicitud.coordenadas en ms-solicitudes) */
export function aCoordenadas(p: Punto): string {
  return `${p.lat}, ${p.lng}`;
}

export const geolocalizacionSoportada = () => typeof navigator !== 'undefined' && 'geolocation' in navigator;

/** Mensaje legible para un error de la Geolocation API. */
export function mensajeErrorGeo(err: GeolocationPositionError | Error): string {
  if ('code' in err) {
    if (err.code === err.PERMISSION_DENIED) return 'Permiso de ubicación denegado. Actívalo en la configuración del navegador.';
    if (err.code === err.POSITION_UNAVAILABLE) return 'No se pudo determinar tu ubicación. Revisa que el GPS esté activo.';
    if (err.code === err.TIMEOUT) return 'La ubicación tardó demasiado. Intenta de nuevo.';
  }
  return err.message || 'No se pudo obtener tu ubicación.';
}

/** Posición actual del dispositivo (una sola lectura). */
export function obtenerPosicionActual(): Promise<Punto & { precision: number }> {
  return new Promise((resolve, reject) => {
    if (!geolocalizacionSoportada()) {
      reject(new Error('Tu navegador no permite obtener la ubicación.'));
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude, precision: pos.coords.accuracy }),
      (err) => reject(new Error(mensajeErrorGeo(err))),
      { enableHighAccuracy: true, timeout: 15000, maximumAge: 30000 }
    );
  });
}

/** Dirección escrita → coordenadas (Nominatim, priorizando Chile). null si no se encuentra. */
export async function geocodificar(direccion: string): Promise<Punto | null> {
  try {
    const res = await fetch(
      `${NOMINATIM}/search?format=json&limit=1&countrycodes=cl&accept-language=es&q=${encodeURIComponent(direccion)}`
    );
    const geo: { lat: string; lon: string }[] = await res.json();
    if (geo?.length) return { lat: parseFloat(geo[0].lat), lng: parseFloat(geo[0].lon) };
  } catch (e) {
    console.error('Error al geocodificar:', e);
  }
  return null;
}

/** Coordenadas → dirección legible (Nominatim). null si no se puede resolver. */
export async function direccionDesdePunto(p: Punto): Promise<string | null> {
  try {
    const res = await fetch(`${NOMINATIM}/reverse?format=json&accept-language=es&lat=${p.lat}&lon=${p.lng}`);
    const geo: { display_name?: string; address?: Record<string, string> } = await res.json();
    const a = geo.address;
    if (a) {
      const calle = [a.road, a.house_number].filter(Boolean).join(' ');
      const comuna = a.city_district ?? a.suburb ?? a.town ?? a.city ?? a.village;
      const corta = [calle, comuna].filter(Boolean).join(', ');
      if (corta.length >= 8) return corta;
    }
    return geo.display_name ?? null;
  } catch (e) {
    console.error('Error al obtener la dirección:', e);
    return null;
  }
}
