import { CircleCheck, CircleDot, CreditCard, Hammer, MapPin, PlusCircle, UserCheck, XCircle, type LucideIcon } from 'lucide-react';
import type { HitoSolicitud, TipoHito } from '@/lib/types';
import { cn, formatDate, formatTime } from '@/lib/utils';

// ============================================================
// HitosTimeline — trazabilidad de la solicitud
// ============================================================
// Lista vertical de los hitos registrados por ms-solicitudes, cada uno
// con la fecha y hora en que ocurrió (GET /solicitudes/:id/hitos).

const HITOS: Record<TipoHito, { label: string; icon: LucideIcon; tono: string }> = {
  CREADA: { label: 'Solicitud creada', icon: PlusCircle, tono: 'text-primary-600 bg-primary-50' },
  BUSCANDO_TECNICO: { label: 'Buscando técnico', icon: CircleDot, tono: 'text-primary-600 bg-primary-50' },
  ACEPTADA: { label: 'Técnico asignado', icon: UserCheck, tono: 'text-primary-600 bg-primary-50' },
  LLEGADA: { label: 'Técnico llegó al domicilio', icon: MapPin, tono: 'text-primary-600 bg-primary-50' },
  EN_PROCESO: { label: 'Trabajo iniciado', icon: Hammer, tono: 'text-primary-600 bg-primary-50' },
  COMPLETADA: { label: 'Trabajo terminado · pago solicitado', icon: CircleCheck, tono: 'text-warning-dark bg-warning-light' },
  PAGADA: { label: 'Pago recibido', icon: CreditCard, tono: 'text-success-dark bg-success-light' },
  CANCELADA: { label: 'Solicitud cancelada', icon: XCircle, tono: 'text-danger-dark bg-danger-light' },
};

export function HitosTimeline({ hitos, className }: { hitos: HitoSolicitud[]; className?: string }) {
  if (!hitos.length) return <p className="text-sm text-subtle">Sin hitos registrados.</p>;
  return (
    <ol className={cn('space-y-0', className)} aria-label="Línea de tiempo de la solicitud">
      {hitos.map((h, i) => {
        const cfg = HITOS[h.hito] ?? HITOS.CREADA;
        const Icon = cfg.icon;
        const ultimo = i === hitos.length - 1;
        return (
          <li key={`${h.hito}-${h.fecha}`} className="relative flex gap-3 pb-4 last:pb-0">
            {!ultimo && <span aria-hidden className="absolute left-4 top-8 h-[calc(100%-2rem)] w-0.5 -translate-x-1/2 bg-border" />}
            <span className={cn('relative z-10 flex size-8 shrink-0 items-center justify-center rounded-pill', cfg.tono)}>
              <Icon className="size-4" aria-hidden />
            </span>
            <div className="min-w-0 pt-1">
              <p className={cn('text-sm font-medium', ultimo ? 'text-ink' : 'text-subtle')}>{cfg.label}</p>
              <p className="text-xs text-subtle">
                <time dateTime={h.fecha}>
                  {formatDate(h.fecha)} · {formatTime(h.fecha)}
                </time>
              </p>
            </div>
          </li>
        );
      })}
    </ol>
  );
}
