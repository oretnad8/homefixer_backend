'use client';

import { useEffect, useMemo, useState } from 'react';
import dynamic from 'next/dynamic';
import { useParams, useRouter } from 'next/navigation';
import { CircleCheck, Hammer, MapPin, MessageCircle, Navigation, StickyNote, XCircle } from 'lucide-react';
import { Avatar } from '@/components/ui/Avatar';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { TextArea } from '@/components/ui/Input';
import { ErrorState, LoadingRegion, Skeleton } from '@/components/ui/States';
import type { MarcadorMapa } from '@/components/shared/MapLeaflet';
import { PhotoUploader } from '@/components/shared/PhotoUploader';
import { HitosTimeline } from '@/components/shared/HitosTimeline';
import { StatusTimeline } from '@/components/shared/StatusTimeline';
import { finalizarTrabajo, iniciarTrabajo, marcarLlegada, useHitos, useSolicitud } from '@/hooks/useSolicitudes';
import { usePageTitle } from '@/hooks/usePageTitle';
import { CATEGORIA_LABELS, URGENCIA_CONFIG, formatCurrency } from '@/lib/utils';
import { distanciaHasta, formatDistancia, minutosEstimados } from '@/lib/geo';
import { FOTOS_VACIAS, useServicioStore } from '@/store/servicioStore';
import { useUbicacionStore } from '@/store/ubicacionStore';

// ============================================================
// Servicio en Progreso (Técnico)
// ============================================================
// Mobile: todo apilado (cliente → estado → notas → fotos → finalizar).
// Desktop: dos columnas — datos del cliente, estado y notas a la izquierda;
//          fotos antes/después y "Finalizar Trabajo" a la derecha.
// La etapa sale del backend (estado + hito LLEGADA), no del estado local: un
// trabajo terminado no se puede reprocesar y se redirige a su resumen.

const MapLeaflet = dynamic(() => import('@/components/shared/MapLeaflet'), {
  ssr: false,
  loading: () => <div className="h-full w-full animate-pulse rounded-card bg-surface-muted" />,
});

type Etapa = 'en_camino' | 'llegue' | 'trabajando';
const PASOS = ['En camino', 'Llegué', 'Trabajando', 'Finalizado'];
const INDICE: Record<Etapa, number> = { en_camino: 0, llegue: 1, trabajando: 2 };

export default function ServicioEnProgresoTecnicoPage() {
  usePageTitle('Servicio en Progreso');
  const router = useRouter();
  const { id } = useParams<{ id: string }>();
  const { data: s, isLoading, error, refetch } = useSolicitud(id);
  const hitos = useHitos(id);
  const [actualizando, setActualizando] = useState(false);
  const [finalizando, setFinalizando] = useState(false);
  const [errorFinal, setErrorFinal] = useState<string | null>(null);
  const [errorEtapa, setErrorEtapa] = useState<string | null>(null);

  // Trabajo ya terminado: su pantalla es el resumen (no se puede volver a marcar "Llegué")
  const terminado = s?.estado === 'completada' || s?.estado === 'pagada';
  useEffect(() => {
    if (terminado) router.replace(`/tecnico/servicio-completado/${id}`);
  }, [terminado, id, router]);

  const fotos = useServicioStore((st) => st.fotos[id] ?? FOTOS_VACIAS);
  const notas = useServicioStore((st) => st.notas[id] ?? '');
  const { addFoto, removeFoto, setNotas } = useServicioStore();

  // Mi posición la comparte SesionTecnico mientras estoy en esta página (el cliente me sigue en vivo)
  const posicion = useUbicacionStore((st) => st.posicion);
  const estadoUbicacion = useUbicacionStore((st) => st.estado);
  const marcadores = useMemo<MarcadorMapa[]>(() => {
    const lista: MarcadorMapa[] = [];
    if (s?.latitud != null && s?.longitud != null) {
      lista.push({ id: 'cliente', lat: s.latitud, lng: s.longitud, tipo: 'destino', titulo: s.clienteNombre ?? 'Cliente', detalle: s.direccion });
    }
    if (posicion) lista.push({ id: 'yo', lat: posicion.lat, lng: posicion.lng, tipo: 'yo', titulo: 'Tú' });
    return lista;
  }, [s, posicion]);

  if ((isLoading && !s) || terminado) {
    return (
      <LoadingRegion className="grid grid-cols-1 gap-4 lg:grid-cols-2 lg:gap-8">
        <div className="space-y-4">
          <Skeleton className="h-40 rounded-card" />
          <Skeleton className="h-32 rounded-card" />
          <Skeleton className="h-40 rounded-card" />
        </div>
        <Skeleton className="h-96 rounded-card" />
      </LoadingRegion>
    );
  }
  if (error || !s) return <ErrorState message={error?.message} onRetry={refetch} />;

  if (s.estado === 'cancelada') {
    return (
      <Card padding="lg" className="mx-auto w-full md:max-w-lg">
        <div className="flex flex-col items-center text-center">
          <XCircle className="size-14 text-danger" aria-hidden />
          <h2 className="mt-3 text-lg font-semibold text-ink">Solicitud cancelada</h2>
          <p className="mt-1 text-sm text-subtle">Esta solicitud está cerrada y ya no se puede trabajar en ella.</p>
        </div>
        <div className="mt-6 border-t border-border pt-5">
          <HitosTimeline hitos={hitos.data ?? []} />
        </div>
        <Button href="/tecnico/mis-trabajos" variant="outline" width="full" className="mt-6">
          Volver a Mis Trabajos
        </Button>
      </Card>
    );
  }

  const llego = hitos.data?.some((h) => h.hito === 'LLEGADA') ?? false;
  const etapaActual: Etapa = s.estado === 'en_progreso' ? 'trabajando' : llego ? 'llegue' : 'en_camino';
  const km = distanciaHasta(posicion, s);
  const [nombre, apellido] = (s.clienteNombre ?? 'Cliente').split(' ');

  const avanzar = async () => {
    setActualizando(true);
    setErrorEtapa(null);
    try {
      if (etapaActual === 'en_camino') await marcarLlegada(s.id);
      else await iniciarTrabajo(s.id);
      refetch();
      hitos.refetch();
    } catch {
      setErrorEtapa('No se pudo actualizar el estado. Intenta de nuevo.');
    } finally {
      setActualizando(false);
    }
  };

  const finalizar = async () => {
    setErrorFinal(null);
    setFinalizando(true);
    try {
      await finalizarTrabajo(s.id, notas);
      router.push(`/tecnico/servicio-completado/${s.id}`);
    } catch {
      setErrorFinal('No se pudo finalizar el trabajo. Intenta de nuevo.');
      setFinalizando(false);
    }
  };

  return (
    <div className="grid grid-cols-1 gap-4 lg:grid-cols-2 lg:items-start lg:gap-8">
      {/* Columna izquierda: cliente + estado + notas */}
      <div className="space-y-4">
        <Card>
          <div className="flex items-start gap-3">
            <Avatar nombre={nombre} apellido={apellido} size="lg" />
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <h2 className="text-lg font-semibold text-ink">{s.clienteNombre}</h2>
                {s.urgencia !== 'normal' && <Badge variant="urgente">{URGENCIA_CONFIG[s.urgencia].label}</Badge>}
              </div>
              <p className="text-sm text-subtle">{CATEGORIA_LABELS[s.categoria]}</p>
              <p className="mt-2 flex items-start gap-1.5 text-sm text-ink">
                <MapPin className="mt-0.5 size-4 shrink-0 text-subtle" aria-hidden />
                {s.direccion}
              </p>
            </div>
          </div>
          <div className="mt-4 grid grid-cols-2 gap-3">
            <Button href={`/tecnico/chat/${s.id}`} variant="outline" width="full" leftIcon={<MessageCircle className="size-4" />}>
              Chatear
            </Button>
            <Button
              href={`https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(
                s.latitud != null && s.longitud != null ? `${s.latitud},${s.longitud}` : s.direccion
              )}`}
              variant="outline"
              width="full"
              leftIcon={<Navigation className="size-4" />}
            >
              Cómo llegar
            </Button>
          </div>
        </Card>

        <Card padding="none" className="overflow-hidden">
          <div className="h-56 md:h-64">
            <MapLeaflet
              marcadores={marcadores}
              ruta={etapaActual === 'en_camino' ? ['yo', 'cliente'] : undefined}
              encuadre="siempre"
              className="relative z-0 h-full w-full"
            />
          </div>
          <p className="px-4 py-3 text-sm text-subtle">
            {km != null
              ? `Estás a ${formatDistancia(km)} del cliente (~${minutosEstimados(km)} min). El cliente ve tu ubicación en vivo.`
              : estadoUbicacion === 'denegada'
                ? 'Permite el acceso a tu ubicación para que el cliente pueda seguirte.'
                : 'Obteniendo tu ubicación…'}
          </p>
        </Card>

        <Card>
          <h3 className="mb-4 font-semibold text-ink">Estado del trabajo</h3>
          <StatusTimeline steps={PASOS} current={INDICE[etapaActual]} />
          {etapaActual !== 'trabajando' && (
            <Button width="full" className="mt-5" loading={actualizando} onClick={avanzar} leftIcon={etapaActual === 'en_camino' ? <MapPin className="size-4" /> : <Hammer className="size-4" />}>
              {etapaActual === 'en_camino' ? 'Marcar "Llegué"' : 'Iniciar trabajo'}
            </Button>
          )}
          {errorEtapa && (
            <p role="alert" className="mt-3 text-center text-sm font-medium text-danger">
              {errorEtapa}
            </p>
          )}
          <div className="mt-5 border-t border-border pt-4">
            <h4 className="mb-3 text-sm font-semibold text-ink">Historial</h4>
            <HitosTimeline hitos={hitos.data ?? []} />
          </div>
        </Card>

        <Card>
          <h3 className="mb-2 font-semibold text-ink">Problema reportado</h3>
          <p className="text-sm text-ink">{s.descripcion}</p>
          {s.presupuestoEstimado && (
            <p className="mt-3 text-sm text-subtle">
              Presupuesto estimado: <strong className="text-ink">{formatCurrency(s.presupuestoEstimado)}</strong>
            </p>
          )}
          <div className="mt-5">
            <TextArea
              label="Notas del trabajo"
              rows={4}
              value={notas}
              onChange={(e) => setNotas(id, e.target.value)}
              hint="Materiales usados, observaciones para el cliente…"
            />
          </div>
          <p className="mt-2 flex items-center gap-1 text-xs text-subtle">
            <StickyNote className="size-3.5" aria-hidden /> Las notas se incluyen en el resumen del servicio.
          </p>
        </Card>
      </div>

      {/* Columna derecha: fotos + finalizar */}
      <Card className="space-y-6 lg:sticky lg:top-24">
        <h3 className="font-semibold text-ink">Evidencia fotográfica</h3>
        <PhotoUploader label="Antes" fotos={fotos.antes} onAdd={(u) => addFoto(id, 'antes', u)} onRemove={(u) => removeFoto(id, 'antes', u)} />
        <PhotoUploader label="Después" fotos={fotos.despues} onAdd={(u) => addFoto(id, 'despues', u)} onRemove={(u) => removeFoto(id, 'despues', u)} />

        {errorFinal && (
          <p role="alert" className="rounded-input bg-danger-light px-4 py-3 text-sm font-medium text-danger-dark">
            {errorFinal}
          </p>
        )}

        <Button
          variant="success"
          width="full"
          size="lg"
          onClick={finalizar}
          loading={finalizando}
          disabled={etapaActual !== 'trabajando'}
          leftIcon={<CircleCheck className="size-5" />}
        >
          Finalizar Trabajo
        </Button>
        {etapaActual !== 'trabajando' && <p className="text-center text-xs text-subtle">Inicia el trabajo para poder finalizarlo.</p>}
      </Card>
    </div>
  );
}
