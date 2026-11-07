import { MapPin, Navigation } from 'lucide-react';
import { cn } from '@/lib/utils';

// ============================================================
// MapPlaceholder — mapa simulado
// ============================================================
// Sustituir por Google Maps / Mapbox / Leaflet manteniendo la misma API
// (pins con coordenadas relativas 0-100). El contenedor define la altura.

export interface MapPin {
  id: string;
  label: string;
  /** Posición relativa en % */
  x: number;
  y: number;
  tone?: 'primary' | 'success' | 'gold' | 'muted';
  active?: boolean;
}

interface MapPlaceholderProps {
  pins: MapPin[];
  /** Dibuja una ruta punteada entre el primer y el segundo pin */
  route?: boolean;
  className?: string;
  label?: string;
}

const tones = {
  primary: 'text-primary-600',
  success: 'text-success',
  gold: 'text-gold',
  muted: 'text-subtle',
};

export function MapPlaceholder({ pins, route, className, label = 'Mapa con ubicaciones' }: MapPlaceholderProps) {
  const [a, b] = pins;
  return (
    <div
      role="img"
      aria-label={`${label}: ${pins.map((p) => p.label).join(', ')}`}
      className={cn('map-grid relative overflow-hidden rounded-card border border-border', className)}
    >
      {/* Calles y parque decorativos */}
      <div aria-hidden className="absolute inset-0">
        <div className="absolute left-0 right-0 top-[38%] h-3 bg-surface/90" />
        <div className="absolute left-0 right-0 top-[72%] h-2 bg-surface/80" />
        <div className="absolute bottom-0 top-0 left-[28%] w-3 bg-surface/90" />
        <div className="absolute bottom-0 top-0 left-[66%] w-2 bg-surface/80" />
        <div className="absolute left-[70%] top-[8%] h-[24%] w-[22%] rounded-card bg-success-light" />
        <div className="absolute left-[6%] top-[78%] h-[16%] w-[16%] rounded-card bg-success-light" />
      </div>

      {route && a && b && (
        <svg aria-hidden className="absolute inset-0 size-full" viewBox="0 0 100 100" preserveAspectRatio="none">
          <path
            d={`M ${a.x} ${a.y} L ${a.x} ${b.y} L ${b.x} ${b.y}`}
            fill="none"
            className="stroke-primary-500"
            strokeWidth="0.8"
            strokeDasharray="2 1.5"
            vectorEffect="non-scaling-stroke"
            style={{ strokeWidth: 3 }}
          />
        </svg>
      )}

      {pins.map((p) => (
        <div
          key={p.id}
          className="absolute flex -translate-x-1/2 -translate-y-full flex-col items-center transition-transform"
          style={{ left: `${p.x}%`, top: `${p.y}%` }}
        >
          <span
            className={cn(
              'mb-1 whitespace-nowrap rounded-pill bg-surface px-2 py-0.5 text-[11px] font-semibold text-ink shadow-card',
              p.active && 'bg-primary-500 text-white'
            )}
          >
            {p.label}
          </span>
          <MapPin
            aria-hidden
            className={cn(
              'drop-shadow',
              p.active ? 'size-9 text-primary-600' : 'size-7',
              !p.active && tones[p.tone ?? 'primary'],
              'fill-surface'
            )}
          />
        </div>
      ))}

      <span className="absolute bottom-3 right-3 flex size-10 items-center justify-center rounded-pill bg-surface text-primary-600 shadow-card-md" aria-hidden>
        <Navigation className="size-5" />
      </span>
    </div>
  );
}
