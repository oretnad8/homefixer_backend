import { Clock, MapPin, Navigation, Wrench } from 'lucide-react';
import { Avatar } from '@/components/ui/Avatar';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { formatDistancia } from '@/lib/geo';
import type { Solicitud } from '@/lib/types';
import { CATEGORIA_LABELS, ESTADO_CONFIG, URGENCIA_CONFIG, formatCurrency, formatRelativeDate } from '@/lib/utils';

// ============================================================
// SolicitudCard — solicitud de servicio (vista técnico y cliente)
// ============================================================

interface SolicitudCardProps {
  solicitud: Solicitud;
  onAceptar?: () => void;
  onRechazar?: () => void;
  aceptando?: boolean;
  /** Muestra el estado en lugar de la urgencia (listas del cliente) */
  mostrarEstado?: boolean;
  href?: string;
  /** Distancia desde la posición del técnico */
  distanciaKm?: number;
  /** cliente: muestra el técnico asignado (el cliente ya sabe que la solicitud es suya) */
  vista?: 'tecnico' | 'cliente';
}

export function SolicitudCard({
  solicitud: s,
  onAceptar,
  onRechazar,
  aceptando,
  mostrarEstado,
  href,
  distanciaKm,
  vista = 'tecnico',
}: SolicitudCardProps) {
  const vistaCliente = vista === 'cliente';
  // Vista cliente: la contraparte es el técnico asignado; vista técnico: el cliente
  const contraparte = vistaCliente ? s.tecnico && `${s.tecnico.nombre} ${s.tecnico.apellido}`.trim() : s.clienteNombre ?? 'Cliente';
  const [nombre, apellido] = (contraparte ?? '').split(' ');
  const urgente = s.urgencia !== 'normal';

  return (
    <Card as="article" interactive className="flex h-full flex-col gap-3">
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 items-center gap-3">
          {contraparte ? (
            <Avatar nombre={nombre} apellido={apellido} size="sm" />
          ) : (
            <div className="flex size-9 shrink-0 items-center justify-center rounded-pill bg-primary-50 text-primary-600">
              <Wrench className="size-4" aria-hidden />
            </div>
          )}
          <div className="min-w-0">
            {vistaCliente ? (
              <>
                <h3 className="truncate font-semibold text-ink">{CATEGORIA_LABELS[s.categoria]}</h3>
                <p className="truncate text-sm text-subtle">{contraparte ?? 'Sin técnico asignado'}</p>
              </>
            ) : (
              <>
                <h3 className="truncate font-semibold text-ink">{contraparte}</h3>
                <p className="text-sm text-subtle">{CATEGORIA_LABELS[s.categoria]}</p>
              </>
            )}
          </div>
        </div>
        {mostrarEstado ? (
          <Badge variant={ESTADO_CONFIG[s.estado].variant}>{ESTADO_CONFIG[s.estado].label}</Badge>
        ) : (
          urgente && <Badge variant={s.urgencia === 'emergencia' ? 'danger' : 'urgente'}>{URGENCIA_CONFIG[s.urgencia].label}</Badge>
        )}
      </div>

      <p className="line-clamp-3 text-sm text-ink">{s.descripcion}</p>

      <dl className="mt-auto space-y-1.5 text-sm text-subtle">
        <div className="flex items-center gap-2">
          <MapPin className="size-4 shrink-0" aria-hidden />
          <dt className="sr-only">Dirección</dt>
          <dd className="truncate">{s.direccion}</dd>
        </div>
        {distanciaKm != null && (
          <div className="flex items-center gap-2 font-medium text-primary-700">
            <Navigation className="size-4 shrink-0" aria-hidden />
            <dt className="sr-only">Distancia</dt>
            <dd>A {formatDistancia(distanciaKm)} de ti</dd>
          </div>
        )}
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <Clock className="size-4 shrink-0" aria-hidden />
            <dt className="sr-only">Publicada</dt>
            <dd>{formatRelativeDate(s.creadoEn)}</dd>
          </div>
          {s.presupuestoEstimado && (
            <>
              <dt className="sr-only">Presupuesto estimado</dt>
              <dd className="text-base font-semibold text-ink">{formatCurrency(s.presupuestoEstimado)}</dd>
            </>
          )}
        </div>
      </dl>

      {(onAceptar || onRechazar) && (
        <div className="grid grid-cols-2 gap-2 border-t border-border pt-3">
          {onRechazar && (
            <Button variant="outline-danger" width="full" size="sm" onClick={onRechazar} disabled={aceptando}>
              Rechazar
            </Button>
          )}
          {onAceptar && (
            <Button variant="success" width="full" size="sm" onClick={onAceptar} loading={aceptando}>
              Aceptar
            </Button>
          )}
        </div>
      )}

      {href && (
        <Button href={href} variant="outline" size="sm" width="full">
          Ver detalle
        </Button>
      )}
    </Card>
  );
}
