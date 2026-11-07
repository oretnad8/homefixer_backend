import { Droplets, Hammer, KeyRound, Paintbrush, Snowflake, Sparkles, Sprout, Wrench, Zap, type LucideIcon } from 'lucide-react';
import type { CategoriaServicio } from '@/lib/types';
import { CATEGORIA_LABELS, cn } from '@/lib/utils';

// ============================================================
// InsigniaEmblema — emblema de maestría por oficio
// ============================================================
// Medallón dorado con el ícono del oficio. `size="sm"` se usa en las
// tarjetas de técnico; `md` en el perfil, con el nombre debajo.

const ICONOS: Record<CategoriaServicio, LucideIcon> = {
  plomeria: Droplets,
  electricidad: Zap,
  carpinteria: Hammer,
  pintura: Paintbrush,
  cerrajeria: KeyRound,
  limpieza: Sparkles,
  jardineria: Sprout,
  refrigeracion: Snowflake,
  otros: Wrench,
};

interface InsigniaEmblemaProps {
  categoria: CategoriaServicio;
  size?: 'sm' | 'md';
  /** Muestra "Maestro en <oficio>" bajo el medallón */
  conNombre?: boolean;
  className?: string;
}

export function InsigniaEmblema({ categoria, size = 'md', conNombre, className }: InsigniaEmblemaProps) {
  const Icono = ICONOS[categoria];
  const nombre = `Maestro en ${CATEGORIA_LABELS[categoria]}`;
  return (
    <span className={cn('inline-flex flex-col items-center gap-1.5', className)} title={nombre}>
      <span
        className={cn(
          'flex items-center justify-center rounded-pill bg-gradient-to-br from-gold-light to-gold text-gold-dark ring-2 ring-gold/40',
          size === 'sm' ? 'size-7' : 'size-14'
        )}
      >
        <Icono className={size === 'sm' ? 'size-3.5' : 'size-7'} aria-hidden />
        {!conNombre && <span className="sr-only">{nombre}</span>}
      </span>
      {conNombre && <span className="max-w-24 text-center text-xs font-medium leading-tight text-ink">{nombre}</span>}
    </span>
  );
}
