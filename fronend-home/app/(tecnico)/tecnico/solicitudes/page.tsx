'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { AnimatePresence, motion } from 'framer-motion';
import { ClipboardList, RotateCcw } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { EmptyState, ErrorState, LoadingRegion, Skeleton } from '@/components/ui/States';
import { SolicitudCard } from '@/components/shared/SolicitudCard';
import { aceptarSolicitud, rechazarSolicitud, useSolicitudesDisponibles } from '@/hooks/useSolicitudes';
import { usePageTitle } from '@/hooks/usePageTitle';
import { distanciaHasta, ordenarPorDistancia } from '@/lib/geo';
import { CATEGORIA_LABELS, cn } from '@/lib/utils';
import type { Tecnico } from '@/lib/types';
import { useUbicacionStore } from '@/store/ubicacionStore';
import { useAuthStore } from '@/store/authStore';

// ============================================================
// Solicitudes Disponibles
// ============================================================
// Mobile: lista vertical de cards. Desktop: grid de 2-3 columnas.
// Cada card con Aceptar / Rechazar.

type Filtro = 'todas' | 'urgentes';

export default function SolicitudesPage() {
  usePageTitle('Solicitudes Disponibles');
  const router = useRouter();
  const tecnicoId = useAuthStore((s) => s.user?.id ?? 'tecnico-001');
  const categoria = useAuthStore((s) => (s.user as Tecnico | null)?.categoria);
  const posicion = useUbicacionStore((s) => s.posicion);
  const { data, isLoading, error, refetch } = useSolicitudesDisponibles();
  const [aceptando, setAceptando] = useState<string | null>(null);
  const [ocultas, setOcultas] = useState<string[]>([]);
  const [filtro, setFiltro] = useState<Filtro>('todas');

  const lista = ordenarPorDistancia(
    (data ?? []).filter((s) => !ocultas.includes(s.id)).filter((s) => filtro === 'todas' || s.urgencia !== 'normal'),
    posicion
  );

  // Las solicitudes nuevas llegan sin recargar
  useEffect(() => {
    const id = setInterval(refetch, 20000);
    return () => clearInterval(id);
  }, [refetch]);

  const aceptar = async (id: string) => {
    setAceptando(id);
    try {
      await aceptarSolicitud(id, tecnicoId);
      router.push(`/tecnico/servicio-en-progreso/${id}`);
    } catch {
      setAceptando(null);
    }
  };

  const rechazar = (id: string) => {
    setOcultas((o) => [...o, id]); // optimista: la card sale con animación
    void rechazarSolicitud(id);
  };

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3 md:mb-6">
        <p className="text-sm text-subtle">
          {data
            ? `${lista.length} solicitudes de ${categoria ? CATEGORIA_LABELS[categoria] : 'tu especialidad'}${posicion ? ', las más cercanas primero' : ''}`
            : 'Buscando solicitudes…'}
        </p>
        <div className="flex items-center gap-2">
          <div className="flex rounded-pill border border-border bg-surface p-1" role="group" aria-label="Filtrar solicitudes">
            {(['todas', 'urgentes'] as Filtro[]).map((f) => (
              <button
                key={f}
                type="button"
                aria-pressed={filtro === f}
                onClick={() => setFiltro(f)}
                className={cn(
                  'h-8 rounded-pill px-4 text-sm font-medium capitalize transition-colors',
                  filtro === f ? 'bg-primary-500 text-white' : 'text-subtle hover:text-ink'
                )}
              >
                {f}
              </button>
            ))}
          </div>
          <Button variant="ghost" size="sm" width="auto" onClick={refetch} aria-label="Actualizar" leftIcon={<RotateCcw className="size-4" />}>
            <span className="hidden sm:inline">Actualizar</span>
          </Button>
        </div>
      </div>

      {isLoading && !data ? (
        <LoadingRegion className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {[0, 1, 2, 3, 4, 5].map((i) => (
            <Skeleton key={i} className="h-64 rounded-card" />
          ))}
        </LoadingRegion>
      ) : error ? (
        <ErrorState message={error.message} onRetry={refetch} />
      ) : lista.length === 0 ? (
        <EmptyState
          icon={<ClipboardList />}
          title="No hay solicitudes disponibles"
          description="Mantén activa tu disponibilidad para recibir nuevas solicitudes."
          action={
            <Button variant="outline" size="sm" width="auto" onClick={refetch}>
              Buscar de nuevo
            </Button>
          }
        />
      ) : (
        <ul className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          <AnimatePresence initial={false}>
            {lista.map((s) => (
              <motion.li key={s.id} layout exit={{ opacity: 0, scale: 0.95 }} transition={{ duration: 0.2 }}>
                <SolicitudCard
                  solicitud={s}
                  distanciaKm={distanciaHasta(posicion, s)}
                  aceptando={aceptando === s.id}
                  onAceptar={() => aceptar(s.id)}
                  onRechazar={() => rechazar(s.id)}
                />
              </motion.li>
            ))}
          </AnimatePresence>
        </ul>
      )}
    </div>
  );
}
