import Link from 'next/link';
import type { ServicioPopular } from '@/lib/types';
import { formatCurrency } from '@/lib/utils';

/** Tarjeta de "Servicios Populares" → abre Nueva Solicitud con la categoría preseleccionada */
export function ServicioCard({ servicio }: { servicio: ServicioPopular }) {
  return (
    <Link
      href={`/cliente/nueva-solicitud?categoria=${servicio.categoria}`}
      className="group flex h-full flex-col rounded-card border border-border bg-surface p-4 shadow-card transition-all hover:-translate-y-0.5 hover:border-primary-200 hover:shadow-card-md md:p-5"
    >
      <span
        className="flex size-12 items-center justify-center rounded-card bg-primary-50 text-2xl transition-colors group-hover:bg-primary-100 md:size-14 md:text-3xl"
        aria-hidden
      >
        {servicio.emoji}
      </span>
      <h3 className="mt-3 font-semibold text-ink">{servicio.nombre}</h3>
      <p className="mt-0.5 line-clamp-2 text-xs text-subtle md:text-sm">{servicio.descripcion}</p>
      <p className="mt-auto pt-3 text-xs text-subtle">
        Desde <span className="font-semibold text-primary-600">{formatCurrency(servicio.precioDesde)}</span>
      </p>
    </Link>
  );
}
