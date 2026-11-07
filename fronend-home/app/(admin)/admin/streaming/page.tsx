'use client';

import { useMemo } from 'react';
import dynamic from 'next/dynamic';
import { Activity, CircleDollarSign, MapPin, Radio, Star, Truck, Wifi, WifiOff, Zap } from 'lucide-react';
import { Card, CardHeader } from '@/components/ui/Card';
import { Badge, type BadgeVariant } from '@/components/ui/Badge';
import { EmptyState, ErrorState, LoadingRegion, Skeleton } from '@/components/ui/States';
import { StatCard } from '@/components/shared/StatCard';
import type { MarcadorMapa } from '@/components/shared/MapLeaflet';
import { usePageTitle } from '@/hooks/usePageTitle';
import { useMetricasStream, type EstadoConexion, type EventoReciente } from '@/hooks/useMetricasStream';
import type { CategoriaServicio } from '@/lib/types';
import { CATEGORIA_LABELS, formatCurrency, formatTime } from '@/lib/utils';

const MapLeaflet = dynamic(() => import('@/components/shared/MapLeaflet'), {
  ssr: false,
  loading: () => <div className="h-full w-full animate-pulse rounded-card bg-surface-muted" />,
});

// ============================================================
// Streaming en vivo (Administrador)
// ============================================================
// Métricas calculadas en tiempo real por ms-reportes a partir de los tópicos Kafka
// homefixer.solicitudes.eventos y homefixer.tecnicos.ubicacion, recibidas por SSE.
// Para tener datos en la demo: ms-ubicacion con KAFKA_SIMULADOR=true (GPS) y
// POST /api/kafka/admin/simular?eventos=600 (ciclo de vida de solicitudes).

/** Orden del ciclo de vida: el gráfico de estados se lee como un embudo */
const ORDEN_ESTADOS = ['CREADA', 'ACEPTADA', 'EN_PROCESO', 'COMPLETADA', 'PAGADA', 'CANCELADA'];

const ETIQUETA_ESTADO: Record<string, string> = {
  CREADA: 'Creada',
  ACEPTADA: 'Aceptada',
  EN_PROCESO: 'En proceso',
  COMPLETADA: 'Completada',
  PAGADA: 'Pagada',
  CANCELADA: 'Cancelada',
};

const ETIQUETA_TIPO: Record<string, string> = {
  SOLICITUD_CREADA: 'Solicitud creada',
  TECNICO_ASIGNADO: 'Técnico asignado',
  ESTADO_ACTUALIZADO: 'Cambio de estado',
  PAGO_PROCESADO: 'Pago procesado',
  VALORACION_REGISTRADA: 'Valoración',
};

const VARIANTE_TIPO: Record<string, BadgeVariant> = {
  SOLICITUD_CREADA: 'pendiente',
  TECNICO_ASIGNADO: 'progreso',
  ESTADO_ACTUALIZADO: 'progreso',
  PAGO_PROCESADO: 'completado',
  VALORACION_REGISTRADA: 'maestro',
};

const CONEXION: Record<EstadoConexion, { label: string; variant: BadgeVariant }> = {
  conectando: { label: 'Conectando…', variant: 'neutral' },
  conectado: { label: 'En vivo', variant: 'completado' },
  reconectando: { label: 'Reconectando…', variant: 'pendiente' },
  error: { label: 'Sin conexión', variant: 'danger' },
};

function etiquetaCategoria(c: string): string {
  return CATEGORIA_LABELS[c as CategoriaServicio] ?? c;
}

const numero = new Intl.NumberFormat('es-CL');

export default function StreamingAdminPage() {
  usePageTitle('Streaming en vivo');
  const { metricas, estado, error, ultimaActualizacion } = useMetricasStream();

  const marcadores = useMemo<MarcadorMapa[]>(
    () =>
      (metricas?.tecnicos ?? []).map((t) => ({
        id: String(t.tecnicoId),
        lat: t.latitud,
        lng: t.longitud,
        tipo: 'tecnico',
        titulo: `Técnico #${t.tecnicoId}`,
        detalle: `${t.ocupado ? 'Atendiendo una solicitud' : 'Disponible'} · ${t.origen}`,
        activo: t.ocupado,
      })),
    [metricas?.tecnicos],
  );

  const estados = useMemo(() => {
    const datos = metricas?.solicitudesPorEstado ?? {};
    const conocidos = ORDEN_ESTADOS.filter((e) => e in datos);
    const otros = Object.keys(datos).filter((e) => !ORDEN_ESTADOS.includes(e));
    return [...conocidos, ...otros].map((e) => ({ clave: e, etiqueta: ETIQUETA_ESTADO[e] ?? e, valor: datos[e] }));
  }, [metricas?.solicitudesPorEstado]);

  const categorias = useMemo(
    () =>
      Object.entries(metricas?.solicitudesPorCategoria ?? {})
        .map(([c, v]) => ({ clave: c, etiqueta: etiquetaCategoria(c), valor: v }))
        .sort((a, b) => b.valor - a.valor),
    [metricas?.solicitudesPorCategoria],
  );

  const conexion = CONEXION[estado];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold text-ink md:text-2xl">Streaming de eventos con Kafka</h1>
          <p className="text-sm text-subtle">
            Métricas calculadas al instante desde los tópicos de solicitudes y ubicación de técnicos.
          </p>
        </div>
        <div className="flex items-center gap-2" aria-live="polite">
          <Badge variant={conexion.variant} icon={false}>
            {estado === 'conectado' ? <Wifi className="size-3.5" aria-hidden /> : <WifiOff className="size-3.5" aria-hidden />}
            {conexion.label}
          </Badge>
          {ultimaActualizacion && (
            <span className="text-xs text-subtle">Actualizado {ultimaActualizacion.toLocaleTimeString('es-CL')}</span>
          )}
        </div>
      </div>

      {error && <p className="rounded-card bg-warning-light px-4 py-2 text-sm text-warning-dark">{error}</p>}

      {!metricas ? (
        estado === 'error' ? (
          <ErrorState message={error ?? 'No se pudo conectar al stream de métricas.'} onRetry={() => window.location.reload()} />
        ) : (
          <LoadingRegion label="Conectando al stream…" className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            {[0, 1, 2, 3].map((i) => (
              <Skeleton key={i} className="h-28 rounded-card" />
            ))}
          </LoadingRegion>
        )
      ) : (
        <>
          <section aria-label="Indicadores" className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <StatCard
              label="Eventos último minuto"
              value={numero.format(metricas.eventosUltimoMinuto)}
              hint={`${numero.format(metricas.totalEventos)} eventos procesados en total`}
              icon={<Zap />}
            />
            <StatCard
              label="Ingresos por pagos"
              value={formatCurrency(metricas.ingresosTotales)}
              hint="Suma de PAGO_PROCESADO"
              tone="success"
              icon={<CircleDollarSign />}
            />
            <StatCard
              label="Técnicos en línea"
              value={`${metricas.tecnicosEnLinea}`}
              hint={`${metricas.tecnicosOcupados} atendiendo · ${numero.format(metricas.actualizacionesGpsUltimoMinuto)} posiciones/min`}
              tone="warning"
              icon={<Truck />}
            />
            <StatCard
              label="Calificación promedio"
              value={metricas.calificacionPromedio != null ? metricas.calificacionPromedio.toFixed(2) : '—'}
              hint="Valoraciones recibidas por el stream"
              tone="gold"
              icon={<Star />}
            />
          </section>

          <div className="grid gap-6 lg:grid-cols-2">
            <Card>
              <CardHeader title="Solicitudes por estado" />
              <p className="-mt-2 mb-4 text-xs text-subtle">Último estado conocido de cada solicitud, en orden del ciclo de vida.</p>
              <ListaBarras filas={estados} vacio="Aún no llegan eventos de solicitudes." />
            </Card>
            <Card>
              <CardHeader title="Solicitudes nuevas por categoría" />
              <p className="-mt-2 mb-4 text-xs text-subtle">Eventos SOLICITUD_CREADA, de mayor a menor.</p>
              <ListaBarras filas={categorias} vacio="Aún no se crean solicitudes." />
            </Card>
          </div>

          <div className="grid gap-6 lg:grid-cols-5">
            <Card className="lg:col-span-3">
              <CardHeader
                title="Técnicos en movimiento"
                action={<span className="text-xs text-subtle">Tópico homefixer.tecnicos.ubicacion</span>}
              />
              {marcadores.length === 0 ? (
                <EmptyState
                  icon={<MapPin />}
                  title="Sin posiciones recientes"
                  description="Inicie ms-ubicacion con KAFKA_SIMULADOR=true o conecte un técnico para ver el flujo GPS."
                />
              ) : (
                <div className="h-80 overflow-hidden rounded-card md:h-96">
                  <MapLeaflet marcadores={marcadores} encuadre="cambios" />
                </div>
              )}
              <p className="mt-3 text-xs text-subtle">
                Verde: disponible · Ámbar: atendiendo una solicitud. Cada punto se mueve con cada evento GPS consumido.
              </p>
            </Card>

            <Card className="lg:col-span-2">
              <CardHeader
                title="Últimos eventos"
                action={<Radio className="size-4 text-success" aria-label="Recibiendo eventos" />}
              />
              <FeedEventos eventos={metricas.ultimosEventos} />
            </Card>
          </div>

          <Card>
            <CardHeader title="Eventos por tipo" action={<Activity className="size-4 text-subtle" aria-hidden />} />
            <ListaBarras
              filas={Object.entries(metricas.eventosPorTipo)
                .map(([t, v]) => ({ clave: t, etiqueta: ETIQUETA_TIPO[t] ?? t, valor: v }))
                .sort((a, b) => b.valor - a.valor)}
              vacio="Sin eventos todavía."
            />
          </Card>
        </>
      )}
    </div>
  );
}

// ------------------------------------------------------------
// Barras horizontales de una sola serie: un tono, sin leyenda (el título dice qué se mide),
// nombre y valor como texto. Todas las barras parten de la misma línea base.
// ------------------------------------------------------------

interface Fila {
  clave: string;
  etiqueta: string;
  valor: number;
}

function ListaBarras({ filas, vacio }: { filas: Fila[]; vacio: string }) {
  if (filas.length === 0) return <p className="py-6 text-center text-sm text-subtle">{vacio}</p>;
  const max = Math.max(...filas.map((f) => f.valor), 1);
  const total = filas.reduce((s, f) => s + f.valor, 0);
  return (
    <ul className="space-y-3">
      {filas.map((f) => {
        const pct = total > 0 ? Math.round((f.valor / total) * 100) : 0;
        return (
          <li key={f.clave} className="group" title={`${f.etiqueta}: ${numero.format(f.valor)} (${pct}%)`}>
            <div className="mb-1 flex items-baseline justify-between gap-3 text-sm">
              <span className="truncate text-ink">{f.etiqueta}</span>
              <span className="shrink-0 tabular-nums font-semibold text-ink">
                {numero.format(f.valor)} <span className="font-normal text-subtle">· {pct}%</span>
              </span>
            </div>
            <div className="h-2.5 w-full rounded-[4px] bg-surface-muted" aria-hidden>
              <div
                className="h-full rounded-[4px] bg-primary-500 transition-[width] duration-500 ease-out group-hover:bg-primary-600"
                style={{ width: `${Math.max(2, (f.valor / max) * 100)}%` }}
              />
            </div>
          </li>
        );
      })}
    </ul>
  );
}

function FeedEventos({ eventos }: { eventos: EventoReciente[] }) {
  if (eventos.length === 0) return <p className="py-6 text-center text-sm text-subtle">Esperando eventos…</p>;
  return (
    <ol className="max-h-96 space-y-2 overflow-y-auto pr-1" aria-live="polite" aria-relevant="additions">
      {eventos.map((e) => (
        <li key={e.eventoId} className="flex items-start justify-between gap-3 rounded-card border border-border px-3 py-2">
          <div className="min-w-0">
            <Badge variant={VARIANTE_TIPO[e.tipo] ?? 'neutral'} icon={false}>
              {ETIQUETA_TIPO[e.tipo] ?? e.tipo}
            </Badge>
            <p className="mt-1 truncate text-sm text-ink">
              Solicitud #{e.solicitudId}
              {e.estado ? ` → ${ETIQUETA_ESTADO[e.estado] ?? e.estado}` : ''}
              {e.categoria ? ` · ${etiquetaCategoria(e.categoria)}` : ''}
            </p>
            <p className="truncate text-xs text-subtle">{e.origen ?? 'origen desconocido'}</p>
          </div>
          <div className="shrink-0 text-right">
            {e.monto != null && <p className="text-sm font-semibold tabular-nums text-ink">{formatCurrency(e.monto)}</p>}
            <p className="text-xs text-subtle">{formatTime(e.ocurridoEn)}</p>
          </div>
        </li>
      ))}
    </ol>
  );
}
