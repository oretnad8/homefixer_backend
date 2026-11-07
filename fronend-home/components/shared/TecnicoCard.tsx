import { BadgeCheck, MapPin, Wrench } from 'lucide-react';
import { Avatar } from '@/components/ui/Avatar';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { StarRating } from '@/components/ui/StarRating';
import { formatDistancia, minutosEstimados } from '@/lib/geo';
import type { Tecnico } from '@/lib/types';
import { cn, formatCurrency } from '@/lib/utils';
import { InsigniaEmblema } from './InsigniaEmblema';

interface TecnicoCardProps {
  tecnico: Tecnico;
  onSolicitar?: () => void;
  loading?: boolean;
  disabled?: boolean;
  active?: boolean;
  /** El primero de la lista (con insignia del oficio o el más cercano): se destaca como recomendado */
  recomendado?: boolean;
  onHover?: (hovering: boolean) => void;
}

export function TecnicoCard({ tecnico: t, onSolicitar, loading, disabled, active, recomendado, onHover }: TecnicoCardProps) {
  return (
    <Card
      as="article"
      interactive
      aria-label={`${t.nombre} ${t.apellido}, ${t.especialidad}`}
      className={cn('flex flex-col gap-4', active && 'ring-2 ring-primary-300', recomendado && 'border-success/40')}
      onMouseEnter={() => onHover?.(true)}
      onMouseLeave={() => onHover?.(false)}
    >
      {recomendado && (
        <p className="-mb-1 text-xs font-semibold uppercase tracking-wide text-success-dark">
          {t.categoria && t.insignias?.includes(t.categoria) ? 'Recomendado · maestro del oficio' : 'Recomendado · el más cercano'}
        </p>
      )}
      <div className="flex items-start gap-3">
        <Avatar nombre={t.nombre} apellido={t.apellido} src={t.avatarUrl} online={t.disponible} />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h3 className="font-semibold text-ink">
              {t.nombre} {t.apellido}
            </h3>
            {t.esMaestro && <Badge variant="maestro">Maestro</Badge>}
            {t.validado && (
              <span className="inline-flex items-center gap-1 text-xs font-medium text-primary-600">
                <BadgeCheck className="size-4" aria-hidden /> Verificado
              </span>
            )}
          </div>
          <p className="flex items-center gap-1 text-sm text-subtle">
            <Wrench className="size-3.5" aria-hidden />
            {t.especialidad}
          </p>
          <StarRating value={t.calificacion} showValue totalValoraciones={t.totalValoraciones} className="mt-1" />
          {!!t.insignias?.length && (
            <div className="mt-2 flex flex-wrap gap-1.5" aria-label="Insignias de maestría">
              {t.insignias.map((c) => (
                <InsigniaEmblema key={c} categoria={c} size="sm" />
              ))}
            </div>
          )}
        </div>
      </div>

      {t.descripcion && <p className="line-clamp-2 text-sm text-subtle">{t.descripcion}</p>}

      <div className="mt-auto flex items-center justify-between gap-3 border-t border-border pt-3 text-sm">
        {t.distanciaKm != null ? (
          <span className="flex items-center gap-1 text-subtle">
            <MapPin className="size-4" aria-hidden />
            {formatDistancia(t.distanciaKm)} · ~{minutosEstimados(t.distanciaKm)} min
          </span>
        ) : (
          <span />
        )}
        <span className="text-subtle">
          Desde <strong className="text-base text-ink">{formatCurrency(t.precioBase)}</strong>
        </span>
      </div>

      {onSolicitar && (
        <Button
          width="full"
          onClick={onSolicitar}
          loading={loading}
          disabled={disabled || !t.disponible}
          variant={t.disponible ? 'primary' : 'outline'}
        >
          {t.disponible ? 'Solicitar' : 'No disponible'}
        </Button>
      )}
    </Card>
  );
}
