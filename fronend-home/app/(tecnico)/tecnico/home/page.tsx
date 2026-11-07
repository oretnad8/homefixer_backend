'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Briefcase, ClipboardList, Star, Wallet } from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { EmptyState, ErrorState, LoadingRegion, Skeleton } from '@/components/ui/States';
import { DisponibilidadToggle } from '@/components/layout/Topbar';
import { PageHero, SectionTitle } from '@/components/shared/PageHero';
import { SolicitudCard } from '@/components/shared/SolicitudCard';
import { StatCard } from '@/components/shared/StatCard';
import { EstadoUbicacionCard } from '@/components/shared/EstadoUbicacionCard';
import { aceptarSolicitud, rechazarSolicitud, useSolicitudesDisponibles } from '@/hooks/useSolicitudes';
import { useEstadisticasTecnico } from '@/hooks/useTecnicos';
import { usePageTitle } from '@/hooks/usePageTitle';
import { distanciaHasta, ordenarPorDistancia } from '@/lib/geo';
import type { Tecnico } from '@/lib/types';
import { formatCurrency, rolTecnicoLabel } from '@/lib/utils';
import { useAuthStore } from '@/store/authStore';
import { useUbicacionStore } from '@/store/ubicacionStore';

// ============================================================
// Home Técnico
// ============================================================
// Mobile: header azul con saludo + badge dorado, toggle de disponibilidad,
//         stats apiladas y nuevas solicitudes en lista.
// Desktop: dashboard — stats en grid de 4, toggle en el topbar, y
//         nuevas solicitudes en grid de cards.

export default function HomeTecnicoPage() {
  usePageTitle('Inicio');
  const router = useRouter();
  const user = useAuthStore((s) => s.user) as Tecnico | null;
  const stats = useEstadisticasTecnico();
  const solicitudes = useSolicitudesDisponibles();
  const [aceptando, setAceptando] = useState<string | null>(null);
  const posicion = useUbicacionStore((s) => s.posicion);
  const cercanas = ordenarPorDistancia(solicitudes.data ?? [], posicion);

  const aceptar = async (id: string) => {
    setAceptando(id);
    try {
      await aceptarSolicitud(id, user?.id ?? 'tecnico-001');
      router.push(`/tecnico/servicio-en-progreso/${id}`);
    } catch {
      setAceptando(null);
    }
  };

  const rechazar = async (id: string) => {
    await rechazarSolicitud(id);
    solicitudes.refetch();
  };

  return (
    <div className="space-y-6 lg:space-y-8">
      <PageHero>
        <div className="flex flex-wrap items-center gap-2">
          <p className="text-sm text-primary-100 lg:text-subtle">¡Buen día, {user?.nombre ?? 'técnico'}!</p>
          {user?.esMaestro && <Badge variant="maestro">Técnico Maestro</Badge>}
        </div>
        <h2 className="mt-1 text-2xl font-bold lg:text-3xl">Tu panel de hoy</h2>
        <p className="mt-1 text-sm text-primary-100 lg:text-subtle">{rolTecnicoLabel(user?.especialidad)}</p>
        <DisponibilidadToggle className="mt-4 inline-flex lg:hidden" />
      </PageHero>

      <EstadoUbicacionCard />

      <section aria-labelledby="titulo-stats">
        <h2 id="titulo-stats" className="sr-only">
          Estadísticas
        </h2>
        {stats.isLoading ? (
          <LoadingRegion className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4 lg:gap-4">
            {[0, 1, 2, 3].map((i) => (
              <Skeleton key={i} className="h-24 rounded-card md:h-36" />
            ))}
          </LoadingRegion>
        ) : stats.error || !stats.data ? (
          <ErrorState message={stats.error?.message} onRetry={stats.refetch} />
        ) : (
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4 lg:gap-4">
            <StatCard label="Trabajos hoy" value={stats.data.trabajosHoy} icon={<Briefcase />} />
            <StatCard label="Ganancias de la semana" value={formatCurrency(stats.data.gananciasSemana)} icon={<Wallet />} tone="success" />
            <StatCard label="Calificación" value={stats.data.calificacion.toFixed(1)} icon={<Star />} tone="gold" hint={`${user?.totalValoraciones ?? 0} valoraciones`} />
            <StatCard label="Completados" value={stats.data.trabajosCompletados} icon={<ClipboardList />} tone="warning" />
          </div>
        )}
      </section>

      <section aria-labelledby="titulo-nuevas">
        <SectionTitle
          action={
            <Link href="/tecnico/solicitudes" className="text-sm font-medium text-primary-600 hover:underline">
              Ver todas
            </Link>
          }
        >
          <span id="titulo-nuevas">Nuevas solicitudes</span>
        </SectionTitle>
        {solicitudes.isLoading ? (
          <LoadingRegion className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} className="h-64 rounded-card" />
            ))}
          </LoadingRegion>
        ) : solicitudes.error ? (
          <ErrorState message={solicitudes.error.message} onRetry={solicitudes.refetch} />
        ) : !solicitudes.data?.length ? (
          <EmptyState icon={<ClipboardList />} title="Sin solicitudes nuevas" description="Te avisaremos cuando un cliente cerca de ti necesite ayuda." />
        ) : (
          <ul className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {cercanas.slice(0, 3).map((s) => (
              <li key={s.id}>
                <SolicitudCard
                  solicitud={s}
                  distanciaKm={distanciaHasta(posicion, s)}
                  aceptando={aceptando === s.id}
                  onAceptar={() => aceptar(s.id)}
                  onRechazar={() => rechazar(s.id)}
                />
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
