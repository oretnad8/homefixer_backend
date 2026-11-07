'use client';

import { Suspense, useEffect, useMemo, useState } from 'react';
import dynamic from 'next/dynamic';
import { useRouter, useSearchParams } from 'next/navigation';
import { LoaderCircle, LocateFixed, Lock, RotateCcw, UserSearch } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { EmptyState, ErrorState, LoadingRegion, Skeleton } from '@/components/ui/States';
import type { MarcadorMapa } from '@/components/shared/MapLeaflet';
import { TecnicoCard } from '@/components/shared/TecnicoCard';
import { useSolicitud } from '@/hooks/useSolicitudes';
import { useTecnicosCercanos } from '@/hooks/useTecnicos';
import { usePageTitle } from '@/hooks/usePageTitle';
import { CENTRO_POR_DEFECTO, formatDistancia, obtenerPosicionActual, type Punto } from '@/lib/geo';
import type { CategoriaServicio } from '@/lib/types';
import { CATEGORIA_LABELS, cn } from '@/lib/utils';

const MapLeaflet = dynamic(() => import('@/components/shared/MapLeaflet'), {
  ssr: false,
  loading: () => <div className="h-full w-full animate-pulse rounded-card bg-surface-muted" />,
});

// ============================================================
// Técnicos Disponibles
// ============================================================
// Con ?solicitud=: la solicitud ya está publicada a los especialistas cercanos y el
// cliente ESPERA a que uno la acepte (solo el técnico acepta; el cliente no asigna).
// Se muestran los técnicos EN LÍNEA de esa categoría, del más cercano al más lejano,
// y cuando uno acepta se pasa automáticamente a "servicio en progreso".
// Sin solicitud: explorar técnicos cerca de la ubicación del cliente.

/** La lista y el mapa se refrescan solos (los técnicos se mueven / se conectan) */
const REFRESCO_MS = 15_000;
/** Cada cuánto se revisa si algún especialista ya aceptó la solicitud */
const REVISAR_ACEPTACION_MS = 5_000;

const CATEGORIAS = Object.keys(CATEGORIA_LABELS) as CategoriaServicio[];

export default function TecnicosDisponiblesPage() {
  usePageTitle('Técnicos Disponibles');
  return (
    <Suspense>
      <TecnicosDisponibles />
    </Suspense>
  );
}

function TecnicosDisponibles() {
  const router = useRouter();
  const params = useSearchParams();
  const solicitudId = params.get('solicitud');
  const categoriaParam = params.get('categoria');
  const [categoriaLibre, setCategoriaLibre] = useState<CategoriaServicio | null>(
    categoriaParam && categoriaParam in CATEGORIA_LABELS ? (categoriaParam as CategoriaServicio) : null
  );
  const [hover, setHover] = useState<string | null>(null);
  const [miPosicion, setMiPosicion] = useState<Punto | null>(null);
  const [avisoUbicacion, setAvisoUbicacion] = useState<string | null>(null);

  const { data: solicitud, isLoading: cargandoSolicitud, refetch: refetchSolicitud } = useSolicitud(solicitudId || undefined);
  const esperando = solicitud?.estado === 'pendiente';

  // Mientras espera, se revisa seguido si un especialista ya la aceptó (el efecto de abajo redirige)
  useEffect(() => {
    if (!esperando) return;
    const id = setInterval(refetchSolicitud, REVISAR_ACEPTACION_MS);
    return () => clearInterval(id);
  }, [esperando, refetchSolicitud]);

  // Con solicitud, la categoría es la del problema del cliente: no se muestran otros oficios
  const categoria: CategoriaServicio | null = solicitud ? solicitud.categoria : categoriaLibre;
  const solLat = solicitud?.latitud;
  const solLng = solicitud?.longitud;
  const tieneCoordenadas = solLat != null && solLng != null;
  const necesitaMiPosicion = !solicitudId || (!!solicitud && !tieneCoordenadas);
  // Memo por valor: el intervalo de refresco depende de `punto`
  const punto = useMemo<Punto | null>(
    () => (solLat != null && solLng != null ? { lat: solLat, lng: solLng } : necesitaMiPosicion ? miPosicion : null),
    [solLat, solLng, necesitaMiPosicion, miPosicion]
  );

  // Sin coordenadas de la solicitud se usa la ubicación del dispositivo
  useEffect(() => {
    if (!necesitaMiPosicion || miPosicion) return;
    let cancelado = false;
    obtenerPosicionActual()
      .then((p) => !cancelado && setMiPosicion(p))
      .catch((e: Error) => {
        if (cancelado) return;
        setAvisoUbicacion(`${e.message} Mostrando técnicos cerca del centro de Santiago.`);
        setMiPosicion(CENTRO_POR_DEFECTO);
      });
    return () => {
      cancelado = true;
    };
  }, [necesitaMiPosicion, miPosicion]);

  // Si la solicitud ya tiene técnico, el flujo sigue en "servicio en progreso"
  useEffect(() => {
    if (solicitud && solicitud.estado !== 'pendiente') router.replace(`/cliente/servicio-en-progreso/${solicitud.id}`);
  }, [solicitud, router]);

  const { data: tecnicos, isLoading, error, refetch } = useTecnicosCercanos(punto, categoria);

  useEffect(() => {
    if (!punto) return;
    const id = setInterval(refetch, REFRESCO_MS);
    return () => clearInterval(id);
  }, [punto, refetch]);

  const marcadores = useMemo<MarcadorMapa[]>(() => {
    const lista: MarcadorMapa[] = [];
    if (punto) {
      lista.push({
        id: 'destino',
        lat: punto.lat,
        lng: punto.lng,
        tipo: 'destino',
        titulo: solicitud ? `Tu solicitud #${solicitud.id}` : 'Tu ubicación',
        detalle: solicitud?.direccion,
      });
    }
    for (const t of tecnicos ?? []) {
      if (t.latitud == null || t.longitud == null) continue;
      lista.push({
        id: t.id,
        lat: t.latitud,
        lng: t.longitud,
        tipo: 'tecnico',
        titulo: t.nombre,
        detalle: `${t.especialidad} · ${t.distanciaKm != null ? formatDistancia(t.distanciaKm) : ''} · ★ ${t.calificacion.toFixed(1)}`,
        activo: hover === t.id,
      });
    }
    return lista;
  }, [punto, solicitud, tecnicos, hover]);

  const seleccionarEnMapa = (id: string) => {
    if (id === 'destino') return;
    setHover(id);
    document.getElementById(`tecnico-${id}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  };

  // Explorando (sin solicitud): "Solicitar" lleva a crear la solicitud de ese oficio.
  // Con solicitud no hay botón: la solicitud ya fue publicada y el técnico es quien acepta.
  const crearSolicitudDe = (especialidad: CategoriaServicio | null) => {
    router.push(`/cliente/nueva-solicitud${especialidad ? `?categoria=${especialidad}` : ''}`);
  };

  const cargandoLista = (isLoading && !tecnicos) || !punto || (solicitudId && cargandoSolicitud);

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,5fr)_minmax(0,6fr)] lg:gap-8">
      {/* Columna mapa */}
      <div className="flex flex-col gap-4 lg:sticky lg:top-24 lg:self-start">
        {solicitud && (
          <div className="rounded-card border border-border bg-surface p-4 shadow-card">
            <h3 className="mb-1 font-semibold text-ink">Solicitud #{solicitud.id}</h3>
            <p className="line-clamp-2 text-sm text-subtle">{solicitud.descripcion}</p>
            <p className="mt-2 text-sm text-subtle">{solicitud.direccion}</p>
          </div>
        )}
        <div className="h-64 md:h-80 lg:h-[calc(100dvh-17rem)]">
          <MapLeaflet marcadores={marcadores} onSelect={seleccionarEnMapa} />
        </div>
        {avisoUbicacion && <p className="text-xs text-subtle">{avisoUbicacion}</p>}
      </div>

      {/* Columna lista */}
      <section aria-labelledby="titulo-lista" className="min-w-0">
        <div className="mb-4 flex items-baseline justify-between gap-3">
          <h2 id="titulo-lista" className="text-lg font-semibold text-ink md:text-xl">
            {tecnicos && !cargandoLista
              ? `${tecnicos.length} ${tecnicos.length === 1 ? 'técnico conectado' : 'técnicos conectados'} cerca`
              : 'Buscando técnicos…'}
          </h2>
          <Button variant="ghost" size="sm" width="auto" onClick={refetch} aria-label="Actualizar" leftIcon={<RotateCcw className="size-4" />}>
            <span className="hidden sm:inline">Actualizar</span>
          </Button>
        </div>

        {solicitud ? (
          <p className="mb-4 flex items-center gap-2 text-sm text-subtle">
            <Lock className="size-4" aria-hidden />
            Solo técnicos de <strong className="text-ink">{CATEGORIA_LABELS[solicitud.categoria]}</strong>, del más cercano al más lejano.
          </p>
        ) : (
          <div className="scrollbar-thin -mx-4 mb-4 flex gap-2 overflow-x-auto px-4 pb-1 md:mx-0 md:flex-wrap md:px-0" role="group" aria-label="Filtrar por especialidad">
            {[null, ...CATEGORIAS].map((f) => {
              const on = categoriaLibre === f;
              return (
                <button
                  key={f ?? 'todos'}
                  type="button"
                  aria-pressed={on}
                  onClick={() => setCategoriaLibre(f)}
                  className={cn(
                    'h-9 shrink-0 rounded-pill border px-4 text-sm font-medium transition-colors',
                    on ? 'border-primary-500 bg-primary-500 text-white' : 'border-border bg-surface text-subtle hover:border-primary-300 hover:text-ink'
                  )}
                >
                  {f ? CATEGORIA_LABELS[f] : 'Todos'}
                </button>
              );
            })}
          </div>
        )}

        {esperando && (
          <div role="status" aria-live="polite" className="mb-4 flex items-start gap-3 rounded-card border border-primary-200 bg-primary-50 p-4">
            <LoaderCircle className="mt-0.5 size-5 shrink-0 animate-spin text-primary-600" aria-hidden />
            <div>
              <p className="font-semibold text-ink">Esperando que los especialistas acepten tu solicitud</p>
              <p className="mt-1 text-sm text-subtle">
                Avisamos a los técnicos de {CATEGORIA_LABELS[solicitud.categoria]} cercanos. Cuando uno la acepte te
                llevaremos automáticamente al seguimiento del servicio.
              </p>
            </div>
          </div>
        )}

        {cargandoLista ? (
          <LoadingRegion label="Buscando técnicos" className="grid gap-4 md:grid-cols-2 lg:grid-cols-1 2xl:grid-cols-2">
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} className="h-60 rounded-card" />
            ))}
          </LoadingRegion>
        ) : error ? (
          <ErrorState message={error.message} onRetry={refetch} />
        ) : !tecnicos?.length ? (
          <EmptyState
            icon={<UserSearch />}
            title={categoria ? `No hay técnicos de ${CATEGORIA_LABELS[categoria]} conectados cerca` : 'No hay técnicos conectados cerca'}
            description={
              solicitud
                ? 'Seguimos buscando automáticamente. Tu solicitud ya está publicada: cualquier técnico de esta especialidad puede aceptarla desde su panel.'
                : 'Seguimos buscando automáticamente cada pocos segundos.'
            }
            action={
              solicitud ? (
                <Button variant="outline" size="sm" width="auto" href={`/cliente/servicio-en-progreso/${solicitud.id}`}>
                  Ver estado de la solicitud
                </Button>
              ) : (
                <Button variant="outline" size="sm" width="auto" onClick={() => setCategoriaLibre(null)} leftIcon={<LocateFixed className="size-4" />}>
                  Ver todos
                </Button>
              )
            }
          />
        ) : (
          <ul className="grid gap-4 md:grid-cols-2 lg:grid-cols-1 2xl:grid-cols-2">
            {tecnicos.map((t, i) => (
              <li key={t.id} id={`tecnico-${t.id}`} className="scroll-mt-24">
                <TecnicoCard
                  tecnico={t}
                  recomendado={i === 0}
                  active={hover === t.id}
                  onHover={(h) => setHover(h ? t.id : null)}
                  onSolicitar={solicitudId ? undefined : () => crearSolicitudDe(categoria)}
                />
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
