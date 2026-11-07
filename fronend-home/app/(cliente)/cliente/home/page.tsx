'use client';

import { useMemo, useState } from 'react';
import { ChevronRight, Plus, Search } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { EmptyState, ErrorState, LoadingRegion, Skeleton } from '@/components/ui/States';
import { PageHero, SectionTitle } from '@/components/shared/PageHero';
import { ServicioCard } from '@/components/shared/ServicioCard';
import { Badge } from '@/components/ui/Badge';
import { useMisSolicitudes } from '@/hooks/useSolicitudes';
import { usePageTitle } from '@/hooks/usePageTitle';
import { mockServiciosPopulares } from '@/lib/mock-data';
import { CATEGORIA_LABELS, ESTADO_CONFIG, formatRelativeDate } from '@/lib/utils';
import { useAuthStore } from '@/store/authStore';
import Link from 'next/link';

// ============================================================
// Home Cliente
// ============================================================
// Mobile: header azul con saludo + búsqueda, grid 2 col de Servicios Populares
//         y debajo las solicitudes recientes.
// Desktop: saludo + búsqueda ancha arriba; grid 3-4 col de servicios a la
//         izquierda y columna lateral de "Solicitudes recientes".

export default function HomeClientePage() {
  usePageTitle('Inicio');
  const user = useAuthStore((s) => s.user);
  const [busqueda, setBusqueda] = useState('');

  const servicios = useMemo(() => {
    const q = busqueda.trim().toLowerCase();
    if (!q) return mockServiciosPopulares;
    return mockServiciosPopulares.filter(
      (s) => s.nombre.toLowerCase().includes(q) || s.descripcion.toLowerCase().includes(q)
    );
  }, [busqueda]);

  return (
    <div className="space-y-6 lg:space-y-8">
      <PageHero>
        <div className="lg:flex lg:items-end lg:justify-between lg:gap-6">
          <div>
            <p className="text-sm text-primary-100 lg:text-subtle">¡Hola, {user?.nombre ?? 'bienvenido'}! 👋</p>
            <h2 className="mt-1 text-2xl font-bold lg:text-3xl">¿Qué necesitas reparar hoy?</h2>
          </div>
          <Button href="/cliente/nueva-solicitud" leftIcon={<Plus className="size-5" />} className="hidden lg:inline-flex">
            Nueva Solicitud
          </Button>
        </div>

        <div className="relative mt-4 lg:mt-6">
          <label htmlFor="buscar-servicio" className="sr-only">
            Buscar servicio
          </label>
          <Search className="pointer-events-none absolute left-4 top-1/2 size-5 -translate-y-1/2 text-subtle" aria-hidden />
          <input
            id="buscar-servicio"
            type="search"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
            placeholder="Buscar servicio (plomería, electricidad…)"
            className="h-12 w-full rounded-pill border border-transparent bg-surface pl-12 pr-4 text-sm text-ink shadow-card-md outline-none placeholder:text-subtle focus:ring-2 focus:ring-primary-300 lg:h-14 lg:border-border lg:shadow-card"
          />
        </div>
      </PageHero>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,1fr)_320px] lg:gap-8">
        <section aria-labelledby="titulo-populares">
          <SectionTitle>
            <span id="titulo-populares">Servicios Populares</span>
          </SectionTitle>
          {servicios.length === 0 ? (
            <EmptyState
              icon={<Search />}
              title="Sin resultados"
              description={`No encontramos servicios para “${busqueda}”.`}
              action={
                <Button variant="outline" size="sm" width="auto" onClick={() => setBusqueda('')}>
                  Limpiar búsqueda
                </Button>
              }
            />
          ) : (
            <ul className="grid grid-cols-2 gap-3 md:grid-cols-3 md:gap-4 2xl:grid-cols-4">
              {servicios.map((s) => (
                <li key={s.id}>
                  <ServicioCard servicio={s} />
                </li>
              ))}
            </ul>
          )}
        </section>

        <SolicitudesRecientes />
      </div>

      {/* CTA fijo en mobile / tablet */}
      <div className="sticky bottom-4 lg:hidden">
        <Button href="/cliente/nueva-solicitud" width="full" size="lg" leftIcon={<Plus className="size-5" />} className="shadow-card-lg">
          Nueva Solicitud
        </Button>
      </div>
    </div>
  );
}

function SolicitudesRecientes() {
  const { data, isLoading, error, refetch } = useMisSolicitudes();

  return (
    <aside aria-labelledby="titulo-recientes" className="lg:sticky lg:top-24 lg:self-start">
      <SectionTitle
        action={
          <Link href="/cliente/mis-servicios" className="text-sm font-medium text-primary-600 hover:underline">
            Ver todas
          </Link>
        }
      >
        <span id="titulo-recientes">Solicitudes recientes</span>
      </SectionTitle>

      {isLoading ? (
        <LoadingRegion className="space-y-3">
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} className="h-20 rounded-card" />
          ))}
        </LoadingRegion>
      ) : error ? (
        <ErrorState message={error.message} onRetry={refetch} />
      ) : !data?.length ? (
        <EmptyState title="Aún no tienes solicitudes" description="Crea tu primera solicitud y te conectamos con un técnico." />
      ) : (
        <ul className="space-y-3">
          {data.slice(0, 4).map((s) => {
            const href =
              s.estado === 'pendiente'
                ? `/cliente/tecnicos-disponibles?solicitud=${s.id}&categoria=${s.categoria}`
                : `/cliente/servicio-en-progreso/${s.id}`;
            return (
              <li key={s.id}>
                <Link href={href} className="block rounded-card focus-visible:outline-2 focus-visible:outline-primary-500">
                  <Card padding="sm" interactive className="flex items-center gap-3">
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center justify-between gap-2">
                        <p className="font-semibold text-ink">{CATEGORIA_LABELS[s.categoria]}</p>
                        <Badge variant={ESTADO_CONFIG[s.estado].variant} icon={false}>
                          {ESTADO_CONFIG[s.estado].label}
                        </Badge>
                      </div>
                      <p className="truncate text-sm text-subtle">{s.descripcion}</p>
                      <p className="mt-0.5 text-xs text-subtle">{formatRelativeDate(s.creadoEn)}</p>
                    </div>
                    <ChevronRight className="size-5 shrink-0 text-subtle" aria-hidden />
                  </Card>
                </Link>
              </li>
            );
          })}
        </ul>
      )}
    </aside>
  );
}
