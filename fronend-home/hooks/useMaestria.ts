'use client';

import api, { USE_MOCKS } from '@/lib/api';
import { BUCKET_CERTIFICACIONES, subirArchivo } from '@/lib/storage';
import type { CategoriaServicio, Certificacion, EstadoCertificacion, InsigniaMaestria, NivelReputacion } from '@/lib/types';
import { aCategoria, delay } from '@/lib/utils';
import { useAuthStore } from '@/store/authStore';
import { useQuery } from './useQuery';

// ============================================================
// useMaestria — ms-maestria (certificaciones e insignias de maestría)
// ============================================================
//   POST /maestria/certificaciones                    { titulo, institucion, categoria, fechaEmision, urlDocumento }
//   GET  /maestria/certificaciones/tecnico/:id        → Certificacion[]
//   GET  /maestria/certificaciones/pendientes         (admin)
//   PUT  /maestria/certificaciones/:id/aprobar        (admin) → otorga la insignia
//   PUT  /maestria/certificaciones/:id/rechazar       (admin) { motivo }
//   GET  /maestria/insignias/tecnico/:id              → InsigniaMaestria[]
//   GET  /maestria/requisitos                         → { nivelMinimo }

interface CertificacionApi {
  id: number;
  tecnicoId: number;
  tecnicoNombre?: string | null;
  titulo: string;
  institucion: string;
  categoria?: string | null;
  fechaEmision?: string | null;
  urlDocumento?: string | null;
  estado?: 'PENDIENTE' | 'APROBADA' | 'RECHAZADA' | null;
  motivoRechazo?: string | null;
  fechaSolicitud?: string | null;
  fechaRevision?: string | null;
}

interface InsigniaApi {
  id: number;
  nombre: string;
  descripcion?: string | null;
  categoria?: string | null;
  fechaOtorgada?: string | null;
}

export const NIVELES: NivelReputacion[] = ['NUEVO', 'BUENO', 'EXPERTO'];

export const NIVEL_LABELS: Record<NivelReputacion, string> = {
  NUEVO: 'Nuevo',
  BUENO: 'Bueno',
  EXPERTO: 'Experto',
};

/** Requisito de cada nivel (lo calcula ms-usuarios con cada valoración) */
export const NIVEL_REQUISITOS: Record<NivelReputacion, string> = {
  NUEVO: 'Menos de 3 valoraciones o promedio bajo 4,0',
  BUENO: '3+ valoraciones con promedio 4,0 o más',
  EXPERTO: '10+ valoraciones con promedio 4,5 o más',
};

export function alcanzaNivel(actual: NivelReputacion | undefined, minimo: NivelReputacion) {
  return NIVELES.indexOf(actual ?? 'NUEVO') >= NIVELES.indexOf(minimo);
}

function mapCertificacion(c: CertificacionApi): Certificacion {
  return {
    id: String(c.id),
    tecnicoId: String(c.tecnicoId),
    tecnicoNombre: c.tecnicoNombre ?? undefined,
    titulo: c.titulo,
    institucion: c.institucion,
    categoria: aCategoria(c.categoria),
    fechaEmision: c.fechaEmision ?? undefined,
    urlDocumento: c.urlDocumento ?? undefined,
    estado: (c.estado ?? 'PENDIENTE').toLowerCase() as EstadoCertificacion,
    motivoRechazo: c.motivoRechazo ?? undefined,
    fechaSolicitud: c.fechaSolicitud ?? undefined,
    fechaRevision: c.fechaRevision ?? undefined,
  };
}

export function mapInsignia(i: InsigniaApi): InsigniaMaestria {
  return {
    id: String(i.id),
    nombre: i.nombre,
    descripcion: i.descripcion ?? undefined,
    categoria: aCategoria(i.categoria),
    fechaOtorgada: i.fechaOtorgada ?? undefined,
  };
}

// ---------- Técnico ----------

export function useCertificaciones(tecnicoId: string | undefined) {
  return useQuery<Certificacion[]>(tecnicoId ? `certificaciones:${tecnicoId}` : null, async () => {
    if (USE_MOCKS) {
      await delay(400);
      return [];
    }
    const { data } = await api.get<CertificacionApi[]>(`/maestria/certificaciones/tecnico/${tecnicoId}`);
    return data.map(mapCertificacion);
  });
}

export function useInsignias(tecnicoId: string | undefined) {
  return useQuery<InsigniaMaestria[]>(tecnicoId ? `insignias:${tecnicoId}` : null, async () => {
    if (USE_MOCKS) {
      await delay(400);
      return [];
    }
    return obtenerInsignias(tecnicoId!);
  });
}

export async function obtenerInsignias(tecnicoId: string): Promise<InsigniaMaestria[]> {
  const { data } = await api.get<InsigniaApi[]>(`/maestria/insignias/tecnico/${tecnicoId}`);
  return data.map(mapInsignia);
}

/** Nivel de reputación necesario para postular (configurable en ms-maestria) */
export function useNivelMinimo() {
  return useQuery<NivelReputacion>('maestria:requisitos', async () => {
    if (USE_MOCKS) return 'BUENO';
    const { data } = await api.get<{ nivelMinimo: NivelReputacion }>('/maestria/requisitos');
    return data.nivelMinimo;
  });
}

export interface NuevaCertificacion {
  titulo: string;
  institucion: string;
  categoria: CategoriaServicio;
  fechaEmision?: string;
  archivo: File;
}

/** Sube el documento a Supabase Storage y registra la certificación (queda PENDIENTE). */
export async function subirCertificacion(datos: NuevaCertificacion): Promise<Certificacion> {
  if (USE_MOCKS) {
    await delay(800);
    return {
      id: `cert-${Date.now()}`,
      tecnicoId: useAuthStore.getState().user?.id ?? '',
      titulo: datos.titulo,
      institucion: datos.institucion,
      categoria: datos.categoria,
      fechaEmision: datos.fechaEmision,
      estado: 'pendiente',
      fechaSolicitud: new Date().toISOString(),
    };
  }
  const { user, token } = useAuthStore.getState();
  if (!user || !token) throw new Error('Tu sesión expiró. Vuelve a iniciar sesión.');
  const extension = datos.archivo.name.split('.').pop()?.toLowerCase() || 'pdf';
  const ruta = `tecnico-${user.id}/${datos.categoria}-${Date.now()}.${extension}`;
  const urlDocumento = await subirArchivo(BUCKET_CERTIFICACIONES, ruta, datos.archivo, token);

  const { data } = await api.post<CertificacionApi>('/maestria/certificaciones', {
    titulo: datos.titulo,
    institucion: datos.institucion,
    categoria: datos.categoria,
    fechaEmision: datos.fechaEmision || null,
    urlDocumento,
  });
  return mapCertificacion(data);
}

// ---------- Administrador ----------

export function useCertificacionesPendientes() {
  return useQuery<Certificacion[]>('certificaciones:pendientes', async () => {
    if (USE_MOCKS) {
      await delay(500);
      return [];
    }
    const { data } = await api.get<CertificacionApi[]>('/maestria/certificaciones/pendientes');
    return data.map(mapCertificacion);
  });
}

export async function aprobarCertificacion(id: string) {
  if (USE_MOCKS) return delay(400);
  await api.put(`/maestria/certificaciones/${id}/aprobar`);
}

export async function rechazarCertificacion(id: string, motivo: string) {
  if (USE_MOCKS) return delay(400);
  await api.put(`/maestria/certificaciones/${id}/rechazar`, { motivo });
}
