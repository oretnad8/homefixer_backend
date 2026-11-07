'use client';

import type { ReactNode } from 'react';
import { EmptyState, ErrorState, LoadingRegion, Skeleton } from '@/components/ui/States';
import type { QueryResult } from '@/hooks/useQuery';
import type { Solicitud } from '@/lib/types';
import { SolicitudCard } from './SolicitudCard';

interface SolicitudesListProps {
  query: QueryResult<Solicitud[]>;
  filtro?: (s: Solicitud) => boolean;
  hrefDe: (s: Solicitud) => string;
  empty: { title: string; description: string; action?: ReactNode };
  vista?: 'tecnico' | 'cliente';
}

/** Lista de solicitudes: 1 columna mobile, grid 2-3 columnas en desktop */
export function SolicitudesList({ query, filtro, hrefDe, empty, vista }: SolicitudesListProps) {
  const { data, isLoading, error, refetch } = query;

  if (isLoading) {
    return (
      <LoadingRegion className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {[0, 1, 2].map((i) => (
          <Skeleton key={i} className="h-56 rounded-card" />
        ))}
      </LoadingRegion>
    );
  }
  if (error) return <ErrorState message={error.message} onRetry={refetch} />;

  const lista = filtro ? (data ?? []).filter(filtro) : (data ?? []);
  if (!lista.length) return <EmptyState {...empty} />;

  return (
    <ul className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
      {lista.map((s) => (
        <li key={s.id}>
          <SolicitudCard solicitud={s} mostrarEstado href={hrefDe(s)} vista={vista} />
        </li>
      ))}
    </ul>
  );
}
