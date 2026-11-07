'use client';

import api, { USE_MOCKS } from '@/lib/api';
import { mapSolicitud, type SolicitudApi } from '@/lib/mappers';
import { findSolicitud, findTecnico, mockDb, updateSolicitud } from '@/lib/mock-db';
import type { HitoSolicitud, NuevaSolicitudInput, Solicitud, TipoHito } from '@/lib/types';
import { aCoordenadas, geocodificar, type Punto } from '@/lib/geo';
import { delay } from '@/lib/utils';
import { useAuthStore } from '@/store/authStore';
import { useQuery } from './useQuery';

// ============================================================
// useSolicitudes — ms-solicitudes
// ============================================================
//   GET   /solicitudes/cliente/:id                 → Solicitud[] (cliente)
//   GET   /solicitudes/tecnico/:id                 → Solicitud[] (trabajos del técnico)
//   GET   /solicitudes/disponibles?tecnicoId=      → Solicitud[] (nuevas, sin técnico)
//   GET   /solicitudes/:id                         → Solicitud
//   POST  /solicitudes                             → Solicitud
//   PATCH /solicitudes/:id/asignar-tecnico/:tecId  (solo el técnico acepta)
//   POST  /solicitudes/:id/rechazar?tecnicoId= | /llegada | /iniciar | /finalizar { notas } | /cancelar
//   GET   /solicitudes/:id/hitos                   → línea de tiempo [{ hito, fecha }]

/** Solicitudes del cliente autenticado */
export function useMisSolicitudes() {
  const clienteId = useAuthStore((s) => s.user?.id ?? '1');
  return useQuery<Solicitud[]>(`mis-solicitudes:${clienteId}`, async () => {
    if (USE_MOCKS) {
      await delay(600);
      return mockDb.solicitudes
        .filter((s) => s.clienteId === clienteId)
        .sort((a, b) => b.creadoEn.localeCompare(a.creadoEn));
    }
    const { data } = await api.get<SolicitudApi[]>(`/solicitudes/cliente/${clienteId}`);
    return data.map(mapSolicitud);
  });
}

/** Solicitudes nuevas que el técnico autenticado puede aceptar */
export function useSolicitudesDisponibles() {
  const tecnicoId = useAuthStore((s) => s.user?.id ?? '');
  return useQuery<Solicitud[]>(`solicitudes-disponibles:${tecnicoId}`, async () => {
    if (USE_MOCKS) {
      await delay(700);
      return mockDb.solicitudes.filter(
        (s) => s.estado === 'pendiente' && s.clienteId !== 'cliente-001' && !mockDb.rechazadas.has(s.id)
      );
    }
    const { data } = await api.get<SolicitudApi[]>('/solicitudes/disponibles', { params: { tecnicoId } });
    return data.map(mapSolicitud);
  });
}

/** Trabajos asignados al técnico autenticado */
export function useMisTrabajos() {
  const tecnicoId = useAuthStore((s) => s.user?.id ?? '2');
  return useQuery<Solicitud[]>(`mis-trabajos:${tecnicoId}`, async () => {
    if (USE_MOCKS) {
      await delay(600);
      return mockDb.solicitudes.filter((s) => s.tecnicoId === tecnicoId);
    }
    const { data } = await api.get<SolicitudApi[]>(`/solicitudes/tecnico/${tecnicoId}`);
    return data.map(mapSolicitud);
  });
}

export function useSolicitud(id: string | undefined) {
  return useQuery<Solicitud>(id ? `solicitud:${id}` : null, async () => {
    if (USE_MOCKS) {
      await delay(500);
      const s = findSolicitud(id!);
      if (!s) throw new Error('No encontramos esta solicitud.');
      return s;
    }
    return mapSolicitud((await api.get<SolicitudApi>(`/solicitudes/${id}`)).data);
  });
}

/** Mocks: reconstruye la línea de tiempo a partir del estado actual */
const HITOS_MOCK: Partial<Record<Solicitud['estado'], TipoHito[]>> = {
  pendiente: ['CREADA'],
  aceptada: ['CREADA', 'ACEPTADA'],
  en_progreso: ['CREADA', 'ACEPTADA', 'EN_PROCESO'],
  completada: ['CREADA', 'ACEPTADA', 'EN_PROCESO', 'COMPLETADA'],
  pagada: ['CREADA', 'ACEPTADA', 'EN_PROCESO', 'COMPLETADA', 'PAGADA'],
  cancelada: ['CREADA', 'CANCELADA'],
};

/** Hitos de la solicitud con la fecha y hora de cada cambio */
export function useHitos(id: string | undefined) {
  return useQuery<HitoSolicitud[]>(id ? `hitos:${id}` : null, async () => {
    if (USE_MOCKS) {
      await delay(300);
      const s = findSolicitud(id!);
      if (!s) return [];
      return (HITOS_MOCK[s.estado] ?? ['CREADA']).map((hito, i, lista) => ({
        hito,
        fecha: i === 0 ? s.creadoEn : i === lista.length - 1 ? s.actualizadoEn : s.creadoEn,
      }));
    }
    const { data } = await api.get<{ hito: TipoHito; fecha: string }[]>(`/solicitudes/${id}/hitos`);
    return data.map((h) => ({ hito: h.hito, fecha: h.fecha }));
  });
}

// ---------- Mutaciones ----------

/** La dirección escrita no se pudo ubicar en el mapa. */
export class DireccionNoEncontradaError extends Error {
  constructor() {
    super('No encontramos esa dirección en el mapa. Agrega la comuna o usa tu ubicación actual.');
    this.name = 'DireccionNoEncontradaError';
  }
}

/**
 * Crea la solicitud. `punto` = ubicación exacta (GPS del cliente); si no viene,
 * se geocodifica la dirección escrita con OpenStreetMap.
 * `claveIdempotencia`: una por envío del formulario; si el POST se repite (doble
 * clic, reintento), ms-solicitudes devuelve la misma solicitud en vez de crear otra.
 */
export async function crearSolicitud(
  input: NuevaSolicitudInput,
  clienteId: string,
  clienteNombre: string,
  punto: Punto | null | undefined,
  claveIdempotencia: string
) {
  if (!USE_MOCKS) {
    const ubicacion = punto ?? (await geocodificar(input.direccion));
    if (!ubicacion) throw new DireccionNoEncontradaError();
    const { data } = await api.post<SolicitudApi>('/solicitudes', {
      clienteId: Number(clienteId),
      clienteNombre,
      descripcion: input.descripcion,
      categoria: input.categoria,
      urgencia: input.urgencia,
      direccion: input.direccion,
      coordenadas: aCoordenadas(ubicacion),
      claveIdempotencia,
    });
    return mapSolicitud(data);
  }

  await delay(800);
  const ahora = new Date().toISOString();
  const nueva: Solicitud = {
    ...input,
    id: `sol-${Date.now()}`,
    clienteId,
    clienteNombre,
    estado: 'pendiente',
    creadoEn: ahora,
    actualizadoEn: ahora,
  };
  mockDb.solicitudes = [nueva, ...mockDb.solicitudes];
  return nueva;
}

/**
 * El técnico toma una solicitud disponible. Es la ÚNICA forma de asignar técnico:
 * el cliente publica la solicitud y espera a que un especialista la acepte
 * (ms-solicitudes rechaza con 403 si quien llama no es ese técnico).
 */
export async function aceptarSolicitud(id: string, tecnicoId: string) {
  if (!USE_MOCKS) {
    return mapSolicitud((await api.patch<SolicitudApi>(`/solicitudes/${id}/asignar-tecnico/${tecnicoId}`)).data);
  }
  await delay(500);
  return updateSolicitud(id, { estado: 'aceptada', tecnicoId, tecnico: findTecnico(tecnicoId) });
}

/** El técnico descarta una solicitud disponible (deja de mostrársele) */
export async function rechazarSolicitud(id: string) {
  if (!USE_MOCKS) {
    const tecnicoId = useAuthStore.getState().user?.id;
    await api.post(`/solicitudes/${id}/rechazar`, null, { params: { tecnicoId } });
    return;
  }
  await delay(300);
  mockDb.rechazadas.add(id);
}

/** El técnico llegó al domicilio: queda registrado en la línea de tiempo */
export async function marcarLlegada(id: string) {
  if (!USE_MOCKS) return mapSolicitud((await api.post<SolicitudApi>(`/solicitudes/${id}/llegada`)).data);
  await delay(300);
  return findSolicitud(id);
}

export async function iniciarTrabajo(id: string) {
  if (!USE_MOCKS) return mapSolicitud((await api.post<SolicitudApi>(`/solicitudes/${id}/iniciar`)).data);
  await delay(400);
  return updateSolicitud(id, { estado: 'en_progreso' });
}

export async function finalizarTrabajo(id: string, notas: string) {
  if (!USE_MOCKS) return mapSolicitud((await api.post<SolicitudApi>(`/solicitudes/${id}/finalizar`, { notas })).data);
  await delay(800);
  return updateSolicitud(id, { estado: 'completada' });
}

/** El cliente cancela una solicitud que aún no ha comenzado */
export async function cancelarSolicitud(id: string) {
  if (!USE_MOCKS) return mapSolicitud((await api.post<SolicitudApi>(`/solicitudes/${id}/cancelar`)).data);
  await delay(400);
  return updateSolicitud(id, { estado: 'cancelada' });
}
