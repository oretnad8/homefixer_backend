'use client';

import { isAxiosError } from 'axios';
import api, { USE_MOCKS } from '@/lib/api';
import { distanciaKm, type Punto } from '@/lib/geo';
import { mockEstadisticas, mockTecnicos } from '@/lib/mock-data';
import type { CategoriaServicio, EstadisticasTecnico, Tecnico } from '@/lib/types';
import { CATEGORIA_LABELS, aCategoria, delay } from '@/lib/utils';
import { useAuthStore } from '@/store/authStore';
import { useQuery } from './useQuery';

// ============================================================
// useTecnicos — ms-usuarios + ms-ubicacion
// ============================================================
//   GET   /ubicacion/tecnicos-cercanos?lat&lng&categoria&excluirId → técnicos EN LÍNEA, por distancia
//   GET   /ubicacion/tecnicos/:id                  → posición en vivo de un técnico
//   PUT   /ubicacion/tecnicos/:id  { latitud, longitud, precision } (lo envía el técnico)
//   GET   /tecnicos/:id                            → perfil del técnico (especialidad, disponible…)
//   PATCH /tecnicos/:id/perfil-profesional         { especialidad, precioBase }
//   PATCH /tecnicos/:id/disponibilidad             { disponible }
//   GET   /solicitudes/tecnico/:id/estadisticas    → EstadisticasTecnico

/** Técnico cercano tal como lo devuelve ms-ubicacion */
interface TecnicoCercanoApi {
  id: number;
  nombre: string;
  telefono?: string | null;
  especialidad?: string | null;
  calificacionPromedio?: number | null;
  precioBase?: number | null;
  nivelReputacion?: string | null;
  validado: boolean;
  latitud: number;
  longitud: number;
  distanciaKm: number;
  actualizadoEn: string;
  /** Oficios con insignia de maestría (ms-ubicacion los ordena primero) */
  insignias?: string[] | null;
}

/** Técnico tal como lo devuelve ms-usuarios */
export interface TecnicoPerfilApi {
  id: number;
  nombre: string;
  email: string;
  telefono?: string;
  especialidad?: string;
  calificacionPromedio?: number;
  precioBase?: number;
  disponible?: boolean | null;
  validado?: boolean;
  nivelReputacion?: string;
}

export interface UbicacionTecnico extends Punto {
  actualizadoEn: string;
  enLinea: boolean;
}

/**
 * Técnicos en línea cerca de `punto`, solo de la `categoria` pedida, del más cercano al más lejano.
 * `punto` null deja la consulta en espera (p. ej. mientras se obtiene la ubicación).
 */
export function useTecnicosCercanos(punto: Punto | null, categoria: CategoriaServicio | null) {
  // Un usuario cliente y técnico a la vez no debe verse a sí mismo en la lista
  const usuarioId = useAuthStore((s) => s.user?.id ?? '');
  const key = punto ? `tecnicos-cercanos:${punto.lat},${punto.lng}:${categoria ?? 'todos'}:${usuarioId}` : null;

  return useQuery<Tecnico[]>(key, async () => {
    if (USE_MOCKS) {
      await delay(700);
      const lista = categoria ? mockTecnicos.filter((t) => t.especialidad === CATEGORIA_LABELS[categoria]) : mockTecnicos;
      return lista
        .map((t, i) => ({
          ...t,
          latitud: punto!.lat + Math.sin(i * 2) * 0.01 * (i + 1),
          longitud: punto!.lng + Math.cos(i * 2) * 0.01 * (i + 1),
        }))
        .map((t) => ({ ...t, distanciaKm: distanciaKm(punto!, { lat: t.latitud, lng: t.longitud }) }))
        .sort((a, b) => a.distanciaKm - b.distanciaKm);
    }
    const { data } = await api.get<TecnicoCercanoApi[]>('/ubicacion/tecnicos-cercanos', {
      params: {
        lat: punto!.lat,
        lng: punto!.lng,
        categoria: categoria ?? undefined,
        excluirId: /^\d+$/.test(usuarioId) ? usuarioId : undefined,
      },
    });
    return data.map((t) => {
      const cat = aCategoria(t.especialidad);
      return {
        id: String(t.id),
        nombre: t.nombre,
        apellido: '',
        email: '',
        telefono: t.telefono ?? undefined,
        rol: 'tecnico' as const,
        creadoEn: t.actualizadoEn,
        especialidad: CATEGORIA_LABELS[cat],
        categoria: cat,
        calificacion: t.calificacionPromedio ?? 0,
        totalValoraciones: 0,
        disponible: true,
        precioBase: t.precioBase ?? 0,
        trabajosCompletados: 0,
        esMaestro: !!t.insignias?.length,
        insignias: (t.insignias ?? []).map(aCategoria),
        validado: t.validado,
        latitud: t.latitud,
        longitud: t.longitud,
        distanciaKm: t.distanciaKm,
      };
    });
  });
}

/** Posición en vivo del técnico asignado; null si no está compartiendo ubicación. */
export function useUbicacionTecnico(tecnicoId: string | undefined) {
  return useQuery<UbicacionTecnico | null>(tecnicoId ? `ubicacion-tecnico:${tecnicoId}` : null, async () => {
    if (USE_MOCKS) {
      await delay(300);
      return null;
    }
    try {
      const { data } = await api.get<{ latitud: number; longitud: number; actualizadoEn: string; enLinea: boolean }>(
        `/ubicacion/tecnicos/${tecnicoId}`
      );
      return { lat: data.latitud, lng: data.longitud, actualizadoEn: data.actualizadoEn, enLinea: data.enLinea };
    } catch (e) {
      if (isAxiosError(e) && e.response?.status === 404) return null;
      throw e;
    }
  });
}

/** Perfil del técnico autenticado tal como está en ms-usuarios. */
export async function obtenerPerfilTecnico(tecnicoId: string): Promise<TecnicoPerfilApi> {
  return (await api.get<TecnicoPerfilApi>(`/tecnicos/${tecnicoId}`)).data;
}

export async function actualizarPerfilProfesional(especialidad: CategoriaServicio, precioBase: number) {
  if (USE_MOCKS) return delay(400);
  const tecnicoId = useAuthStore.getState().user?.id;
  await api.patch(`/tecnicos/${tecnicoId}/perfil-profesional`, { especialidad, precioBase });
}

export function useEstadisticasTecnico() {
  const tecnicoId = useAuthStore((s) => s.user?.id ?? '');
  return useQuery<EstadisticasTecnico>(`estadisticas-tecnico:${tecnicoId}`, async () => {
    if (USE_MOCKS) {
      await delay(600);
      return mockEstadisticas;
    }
    return (await api.get<EstadisticasTecnico>(`/solicitudes/tecnico/${tecnicoId}/estadisticas`)).data;
  });
}

export async function actualizarDisponibilidad(disponible: boolean) {
  if (USE_MOCKS) return delay(200);
  const tecnicoId = useAuthStore.getState().user?.id;
  await api.patch(`/tecnicos/${tecnicoId}/disponibilidad`, { disponible });
}

/** Envía la posición del técnico a ms-ubicacion. */
export async function reportarUbicacion(tecnicoId: string, lat: number, lng: number, precision: number) {
  if (USE_MOCKS) return;
  await api.put(`/ubicacion/tecnicos/${tecnicoId}`, { latitud: lat, longitud: lng, precision });
}

/** Deja de aparecer en el mapa de los clientes. */
export async function dejarDeCompartirUbicacion(tecnicoId: string) {
  if (USE_MOCKS) return;
  await api.delete(`/ubicacion/tecnicos/${tecnicoId}`);
}
