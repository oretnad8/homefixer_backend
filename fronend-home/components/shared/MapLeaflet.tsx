'use client';

import { useEffect, useMemo } from 'react';
import { MapContainer, Marker, Polyline, Popup, TileLayer, useMap } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import L from 'leaflet';
import { CENTRO_POR_DEFECTO } from '@/lib/geo';

// ============================================================
// MapLeaflet — mapa real con OpenStreetMap (gratis, sin API key)
// ============================================================
// Se importa con next/dynamic + ssr:false (Leaflet usa `window`).

export type TipoMarcador = 'destino' | 'tecnico' | 'yo';

export interface MarcadorMapa {
  id: string;
  lat: number;
  lng: number;
  tipo: TipoMarcador;
  titulo: string;
  detalle?: string;
  /** Resaltado (p. ej. la tarjeta con hover) */
  activo?: boolean;
}

interface MapLeafletProps {
  marcadores: MarcadorMapa[];
  /** Dibuja una línea punteada entre estos dos ids (técnico → domicilio) */
  ruta?: [string, string];
  /**
   * 'cambios': encuadra cuando cambia el conjunto de marcadores (listas que se refrescan).
   * 'siempre': encuadra en cada actualización de posición (seguimiento en vivo).
   */
  encuadre?: 'cambios' | 'siempre';
  onSelect?: (id: string) => void;
  className?: string;
}

const COLORES: Record<TipoMarcador, string> = {
  destino: '#2563eb',
  tecnico: '#16a34a',
  yo: '#7c3aed',
};

function icono(tipo: TipoMarcador, activo?: boolean) {
  const size = activo ? 44 : 34;
  const color = activo ? '#f59e0b' : COLORES[tipo];
  const glifo =
    tipo === 'destino'
      ? '<path d="M3 10.5 12 3l9 7.5V21h-6v-6H9v6H3z" fill="white"/>'
      : tipo === 'tecnico'
        ? '<path d="M14.7 6.3a4 4 0 0 0-5.4 5.4L4 17l3 3 5.3-5.3a4 4 0 0 0 5.4-5.4l-2.5 2.5-2.1-.4-.4-2.1z" fill="white"/>'
        : '<circle cx="12" cy="11" r="4" fill="white"/>';
  const pulso =
    tipo !== 'destino'
      ? `<span style="position:absolute;left:50%;bottom:-6px;width:14px;height:14px;margin-left:-7px;border-radius:9999px;background:${color};opacity:.35;animation:hf-pulso 1.8s ease-out infinite"></span>`
      : '';
  return L.divIcon({
    html: `<div style="position:relative;width:${size}px;height:${size}px">${pulso}
      <svg viewBox="0 0 24 24" width="${size}" height="${size}" style="filter:drop-shadow(0 2px 3px rgba(0,0,0,.35))">
        <circle cx="12" cy="12" r="11" fill="${color}" stroke="white" stroke-width="2"/>
        <g transform="translate(4.8 4.8) scale(.6)">${glifo}</g>
      </svg></div>`,
    className: 'hf-marcador',
    iconSize: [size, size],
    iconAnchor: [size / 2, size / 2],
    popupAnchor: [0, -size / 2],
  });
}

function Encuadre({ puntos, clave }: { puntos: [number, number][]; clave: string }) {
  const map = useMap();
  useEffect(() => {
    if (puntos.length === 1) map.setView(puntos[0], Math.max(map.getZoom(), 15));
    else if (puntos.length > 1) map.fitBounds(L.latLngBounds(puntos), { padding: [48, 48], maxZoom: 16 });
    // `clave` decide cuándo se vuelve a encuadrar; `puntos` se lee en ese momento
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [clave, map]);
  return null;
}

export default function MapLeaflet({ marcadores, ruta, encuadre = 'cambios', onSelect, className }: MapLeafletProps) {
  const puntos = useMemo(() => marcadores.map((m) => [m.lat, m.lng] as [number, number]), [marcadores]);
  const clave =
    encuadre === 'siempre'
      ? marcadores.map((m) => `${m.id}@${m.lat.toFixed(5)},${m.lng.toFixed(5)}`).join('|')
      : marcadores.map((m) => m.id).sort().join('|');

  const tramo = ruta
    ? ruta.map((id) => marcadores.find((m) => m.id === id)).filter((m): m is MarcadorMapa => !!m)
    : [];
  const centro: [number, number] = puntos[0] ?? [CENTRO_POR_DEFECTO.lat, CENTRO_POR_DEFECTO.lng];

  return (
    <div className={className ?? 'relative z-0 h-full w-full overflow-hidden rounded-card border border-border shadow-card'}>
      <style>{'@keyframes hf-pulso{0%{transform:scale(.6);opacity:.5}100%{transform:scale(2.6);opacity:0}}.hf-marcador{background:none;border:none}'}</style>
      <MapContainer center={centro} zoom={13} scrollWheelZoom style={{ height: '100%', width: '100%', zIndex: 0 }}>
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        {tramo.length === 2 && (
          <Polyline
            positions={tramo.map((m) => [m.lat, m.lng] as [number, number])}
            pathOptions={{ color: '#2563eb', weight: 4, dashArray: '8 10', opacity: 0.8 }}
          />
        )}

        {marcadores.map((m) => (
          <Marker
            key={m.id}
            position={[m.lat, m.lng]}
            icon={icono(m.tipo, m.activo)}
            zIndexOffset={m.activo ? 1000 : m.tipo === 'destino' ? 500 : 0}
            eventHandlers={onSelect ? { click: () => onSelect(m.id) } : undefined}
          >
            <Popup>
              <strong className="block">{m.titulo}</strong>
              {m.detalle && <span className="text-xs text-gray-600">{m.detalle}</span>}
            </Popup>
          </Marker>
        ))}

        <Encuadre puntos={puntos} clave={clave} />
      </MapContainer>
    </div>
  );
}
