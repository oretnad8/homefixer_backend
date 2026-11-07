'use client';

import { useEffect, useMemo, useState } from 'react';
import dynamic from 'next/dynamic';
import { useParams } from 'next/navigation';
import { isAxiosError } from 'axios';
import { Clock, CreditCard, LoaderCircle, MapPin, MessageCircle, Phone, Star, Users, XCircle } from 'lucide-react';
import { Avatar } from '@/components/ui/Avatar';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { StarRating } from '@/components/ui/StarRating';
import { ErrorState, LoadingRegion, Skeleton } from '@/components/ui/States';
import type { MarcadorMapa } from '@/components/shared/MapLeaflet';
import { HitosTimeline } from '@/components/shared/HitosTimeline';
import { StatusTimeline } from '@/components/shared/StatusTimeline';
import { cancelarSolicitud, useHitos, useSolicitud } from '@/hooks/useSolicitudes';
import { useUbicacionTecnico } from '@/hooks/useTecnicos';
import { distanciaKm, formatDistancia, minutosEstimados } from '@/lib/geo';
import { usePageTitle } from '@/hooks/usePageTitle';
import type { EstadoSolicitud } from '@/lib/types';
import { CATEGORIA_LABELS, ESTADO_CONFIG, formatCurrency } from '@/lib/utils';

// ============================================================
// Servicio en Progreso (Cliente)
// ============================================================
// Mobile: mapa arriba + card de estado debajo.
// Desktop: mapa grande sticky a la izquierda; a la derecha panel con estado,
//          técnico y acciones (chatear, llamar, pagar).

const MapLeaflet = dynamic(() => import('@/components/shared/MapLeaflet'), {
  ssr: false,
  loading: () => <div className="h-full w-full animate-pulse rounded-card bg-surface-muted" />,
});

/** Posición del técnico en vivo mientras viene en camino / trabaja */
const SEGUIMIENTO_MS = 5000;

const PASOS = ['Aceptado', 'En camino', 'Trabajando', 'Finalizado'];
const PASO_POR_ESTADO: Record<EstadoSolicitud, number> = {
  pendiente: 0,
  aceptada: 1,
  en_progreso: 2,
  completada: 3,
  pagada: 3,
  cancelada: 0,
};

export default function ServicioEnProgresoClientePage() {
  usePageTitle('Servicio en Progreso');
  const { id } = useParams<{ id: string }>();
  const { data: s, isLoading, error, refetch } = useSolicitud(id);
  const hitos = useHitos(id);
  const [cancelando, setCancelando] = useState(false);
  const [errorAccion, setErrorAccion] = useState<string | null>(null);

  // Mientras el servicio está activo se refresca el estado y la línea de tiempo (los cambia el técnico)
  const activo = s?.estado === 'pendiente' || s?.estado === 'aceptada' || s?.estado === 'en_progreso';
  const refetchHitos = hitos.refetch;
  useEffect(() => {
    if (!activo) return;
    const intervalo = setInterval(() => {
      refetch();
      refetchHitos();
    }, 10000);
    return () => clearInterval(intervalo);
  }, [activo, refetch, refetchHitos]);

  // Seguimiento en vivo del técnico asignado (ms-ubicacion)
  const siguiendo = s?.estado === 'aceptada' || s?.estado === 'en_progreso';
  const ubicacion = useUbicacionTecnico(siguiendo ? s?.tecnicoId : undefined);
  const refetchUbicacion = ubicacion.refetch;
  useEffect(() => {
    if (!siguiendo) return;
    const intervalo = setInterval(refetchUbicacion, SEGUIMIENTO_MS);
    return () => clearInterval(intervalo);
  }, [siguiendo, refetchUbicacion]);
  const posTecnico = siguiendo && ubicacion.data?.enLinea ? ubicacion.data : null;

  const marcadores = useMemo<MarcadorMapa[]>(() => {
    const lista: MarcadorMapa[] = [];
    if (s?.latitud != null && s?.longitud != null) {
      lista.push({ id: 'casa', lat: s.latitud, lng: s.longitud, tipo: 'destino', titulo: 'Tu domicilio', detalle: s.direccion });
    }
    if (posTecnico) {
      lista.push({ id: 'tecnico', lat: posTecnico.lat, lng: posTecnico.lng, tipo: 'tecnico', titulo: s?.tecnico?.nombre ?? 'Técnico' });
    }
    return lista;
  }, [s, posTecnico]);

  const cancelar = async () => {
    setCancelando(true);
    setErrorAccion(null);
    try {
      await cancelarSolicitud(id);
      refetch();
    } catch (e) {
      const msg = isAxiosError(e) ? (e.response?.data as { message?: string } | undefined)?.message : undefined;
      setErrorAccion(msg ?? 'No se pudo cancelar la solicitud.');
    } finally {
      setCancelando(false);
    }
  };

  if (isLoading && !s) {
    return (
      <LoadingRegion className="grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)] lg:gap-8">
        <Skeleton className="h-64 rounded-card lg:h-[calc(100dvh-8rem)]" />
        <div className="space-y-4">
          <Skeleton className="h-28 rounded-card" />
          <Skeleton className="h-44 rounded-card" />
          <Skeleton className="h-12 rounded-pill" />
        </div>
      </LoadingRegion>
    );
  }
  if (error || !s) return <ErrorState message={error?.message} onRetry={refetch} />;

  const t = s.tecnico;
  const paso = PASO_POR_ESTADO[s.estado];
  const km = posTecnico && s.latitud != null && s.longitud != null
    ? distanciaKm(posTecnico, { lat: s.latitud, lng: s.longitud })
    : null;
  const eta =
    s.estado === 'en_progreso'
      ? 'Trabajando en tu domicilio'
      : s.estado === 'aceptada'
        ? km == null
          ? 'Esperando la ubicación del técnico…'
          : km < 0.1
            ? 'El técnico llegó a tu domicilio'
            : `A ${formatDistancia(km)} · llega en ~${minutosEstimados(km)} min`
        : null;

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)] lg:gap-8">
      <div className="lg:sticky lg:top-24 lg:self-start">
        <div className="h-64 md:h-80 lg:h-[calc(100dvh-8rem)]">
          <MapLeaflet
            marcadores={marcadores}
            ruta={s.estado === 'aceptada' ? ['tecnico', 'casa'] : undefined}
            encuadre="siempre"
          />
        </div>
      </div>

      <div className="space-y-4">
        <Card>
          <div className="mb-5 flex items-center justify-between gap-3">
            <div>
              <p className="text-sm text-subtle">{CATEGORIA_LABELS[s.categoria]}</p>
              <h2 className="text-lg font-semibold text-ink">Estado del servicio</h2>
            </div>
            <Badge variant={ESTADO_CONFIG[s.estado].variant}>{ESTADO_CONFIG[s.estado].label}</Badge>
          </div>
          <StatusTimeline steps={PASOS} current={paso} />
          {eta && (
            <p className="mt-5 flex items-center gap-2 rounded-input bg-primary-50 px-3 py-2 text-sm font-medium text-primary-700">
              <Clock className="size-4" aria-hidden />
              {eta}
            </p>
          )}
        </Card>

        {t ? (
          <Card>
            <div className="flex items-center gap-3">
              <Avatar nombre={t.nombre} apellido={t.apellido} size="lg" online={t.disponible} />
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="font-semibold text-ink">
                    {t.nombre} {t.apellido}
                  </h3>
                  {t.esMaestro && <Badge variant="maestro">Maestro</Badge>}
                </div>
                <p className="text-sm text-subtle">{t.especialidad}</p>
                <StarRating value={t.calificacion} showValue totalValoraciones={t.totalValoraciones} />
              </div>
            </div>
            <div className="mt-4 grid grid-cols-2 gap-3">
              <Button href={`/cliente/chat/${s.id}`} width="full" leftIcon={<MessageCircle className="size-4" />}>
                Chatear
              </Button>
              <Button href={`tel:${t.telefono ?? ''}`} variant="outline" width="full" leftIcon={<Phone className="size-4" />}>
                Llamar
              </Button>
            </div>
          </Card>
        ) : s.estado === 'pendiente' ? (
          <Card className="space-y-3">
            <div role="status" aria-live="polite" className="flex items-start gap-3">
              <LoaderCircle className="mt-0.5 size-5 shrink-0 animate-spin text-primary-600" aria-hidden />
              <div>
                <p className="font-semibold text-ink">Esperando que los especialistas acepten tu solicitud</p>
                <p className="mt-1 text-sm text-subtle">
                  Los técnicos cercanos ya fueron notificados. Esta página se actualiza sola cuando uno acepta.
                </p>
              </div>
            </div>
            <Button
              href={`/cliente/tecnicos-disponibles?solicitud=${s.id}&categoria=${s.categoria}`}
              variant="outline"
              width="full"
              leftIcon={<Users className="size-4" />}
            >
              Ver especialistas cercanos
            </Button>
          </Card>
        ) : null}

        {s.estado === 'cancelada' && (
          <Card className="text-sm font-medium text-danger">Esta solicitud fue cancelada.</Card>
        )}

        <Card>
          <h3 className="mb-3 font-semibold text-ink">Detalle</h3>
          <p className="text-sm text-ink">{s.descripcion}</p>
          <dl className="mt-4 space-y-2 text-sm">
            <div className="flex items-start gap-2 text-subtle">
              <MapPin className="mt-0.5 size-4 shrink-0" aria-hidden />
              <dt className="sr-only">Dirección</dt>
              <dd>{s.direccion}</dd>
            </div>
            {s.presupuestoEstimado && (
              <div className="flex justify-between border-t border-border pt-3">
                <dt className="text-subtle">Presupuesto estimado</dt>
                <dd className="font-semibold text-ink">{formatCurrency(s.presupuestoEstimado)}</dd>
              </div>
            )}
          </dl>
        </Card>

        <Card>
          <h3 className="mb-3 font-semibold text-ink">Línea de tiempo</h3>
          <HitosTimeline hitos={hitos.data ?? []} />
        </Card>

        {/* Primero el pago; la valoración se habilita cuando ms-solicitudes recibe pago.procesado */}
        {s.estado === 'completada' && (
          <Button href={`/cliente/pago/${s.id}`} variant="success" width="full" size="lg" leftIcon={<CreditCard className="size-5" />}>
            Pagar servicio
          </Button>
        )}
        {s.estado === 'pagada' && (
          <Button href={`/cliente/valoracion/${s.id}`} variant="outline" width="full" leftIcon={<Star className="size-4" />}>
            Valorar al técnico
          </Button>
        )}

        {(s.estado === 'pendiente' || s.estado === 'aceptada') && (
          <Button variant="outline-danger" width="full" loading={cancelando} onClick={cancelar} leftIcon={<XCircle className="size-4" />}>
            Cancelar solicitud
          </Button>
        )}
        {errorAccion && (
          <p role="alert" className="text-center text-sm font-medium text-danger">
            {errorAccion}
          </p>
        )}
      </div>
    </div>
  );
}
