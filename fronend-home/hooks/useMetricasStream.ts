'use client';

import { useEffect, useRef, useState } from 'react';
import api from '@/lib/api';
import { TOKEN_KEY } from '@/lib/session';

// ============================================================
// useMetricasStream — métricas en tiempo real desde Kafka (SSE)
// ============================================================
// ms-reportes consume homefixer.solicitudes.eventos y homefixer.tecnicos.ubicacion
// (consumer group reportes-metricas) y empuja un snapshot por Server-Sent Events
// en GET /api/reportes/stream cada vez que llegan eventos nuevos.
// EventSource no permite enviar Authorization, y el gateway exige el JWT (ROLE_ADMIN):
// por eso se lee el stream con fetch + ReadableStream y se reconecta con backoff.

export interface TecnicoPosicion {
  tecnicoId: number;
  latitud: number;
  longitud: number;
  ocupado: boolean;
  registradoEn: string;
  origen: string;
}

export interface EventoReciente {
  eventoId: string;
  tipo: string;
  solicitudId: number;
  estado: string | null;
  categoria: string | null;
  monto: number | null;
  origen: string | null;
  ocurridoEn: string;
}

export interface MetricasTiempoReal {
  generadoEn: string;
  totalEventos: number;
  eventosUltimoMinuto: number;
  eventosPorTipo: Record<string, number>;
  solicitudesPorEstado: Record<string, number>;
  solicitudesPorCategoria: Record<string, number>;
  ingresosTotales: number;
  calificacionPromedio: number | null;
  tecnicosEnLinea: number;
  tecnicosOcupados: number;
  actualizacionesGpsUltimoMinuto: number;
  tecnicos: TecnicoPosicion[];
  ultimosEventos: EventoReciente[];
}

export type EstadoConexion = 'conectando' | 'conectado' | 'reconectando' | 'error';

const MAX_BACKOFF_MS = 15_000;

function urlStream(): string {
  const base = (api.defaults.baseURL ?? 'http://localhost:8080/api').replace(/\/+$/, '');
  return `${base}/reportes/stream`;
}

/** Extrae los eventos SSE completos (separados por línea en blanco) del buffer. */
function parsearEventos(buffer: string): { eventos: { evento: string; data: string }[]; resto: string } {
  const bloques = buffer.split(/\r?\n\r?\n/);
  const resto = bloques.pop() ?? '';
  const eventos = bloques
    .map((bloque) => {
      let evento = 'message';
      const datos: string[] = [];
      for (const linea of bloque.split(/\r?\n/)) {
        if (linea.startsWith('event:')) evento = linea.slice(6).trim();
        else if (linea.startsWith('data:')) datos.push(linea.slice(5).replace(/^ /, ''));
      }
      return { evento, data: datos.join('\n') };
    })
    .filter((e) => e.data.length > 0);
  return { eventos, resto };
}

export function useMetricasStream() {
  const [metricas, setMetricas] = useState<MetricasTiempoReal | null>(null);
  const [estado, setEstado] = useState<EstadoConexion>('conectando');
  const [error, setError] = useState<string | null>(null);
  const [ultimaActualizacion, setUltimaActualizacion] = useState<Date | null>(null);
  const intentos = useRef(0);

  useEffect(() => {
    let cancelado = false;
    let controller: AbortController | null = null;
    let timer: ReturnType<typeof setTimeout> | undefined;

    const conectar = async () => {
      controller = new AbortController();
      setEstado(intentos.current === 0 ? 'conectando' : 'reconectando');
      try {
        const token = localStorage.getItem(TOKEN_KEY);
        const res = await fetch(urlStream(), {
          headers: { Accept: 'text/event-stream', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
          signal: controller.signal,
          cache: 'no-store',
        });
        if (res.status === 401 || res.status === 403) {
          setEstado('error');
          setError('Sin permisos: el stream de métricas es solo para administradores.');
          return; // no tiene sentido reintentar
        }
        if (!res.ok || !res.body) throw new Error(`El stream respondió ${res.status}`);

        setEstado('conectado');
        setError(null);
        intentos.current = 0;

        const reader = res.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        while (!cancelado) {
          const { value, done } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });
          const { eventos, resto } = parsearEventos(buffer);
          buffer = resto;
          for (const e of eventos) {
            if (e.evento !== 'metricas') continue;
            try {
              setMetricas(JSON.parse(e.data) as MetricasTiempoReal);
              setUltimaActualizacion(new Date());
            } catch {
              console.warn('[SSE] Evento de métricas ilegible', e.data.slice(0, 200));
            }
          }
        }
        if (!cancelado) throw new Error('El servidor cerró el stream');
      } catch (e) {
        if (cancelado || (e instanceof DOMException && e.name === 'AbortError')) return;
        intentos.current += 1;
        const espera = Math.min(MAX_BACKOFF_MS, 1000 * 2 ** Math.min(intentos.current, 4));
        setEstado('reconectando');
        setError(`${e instanceof Error ? e.message : 'Conexión perdida'}. Reintentando en ${Math.round(espera / 1000)} s…`);
        timer = setTimeout(conectar, espera);
      }
    };

    conectar();
    return () => {
      cancelado = true;
      controller?.abort();
      if (timer) clearTimeout(timer);
    };
  }, []);

  return { metricas, estado, error, ultimaActualizacion };
}
