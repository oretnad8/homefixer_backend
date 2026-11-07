'use client';

import api, { USE_MOCKS } from '@/lib/api';
import { mapValoracion, type ValoracionApi } from '@/lib/mappers';
import type { Valoracion } from '@/lib/types';
import { delay } from '@/lib/utils';
import { useAuthStore } from '@/store/authStore';

// ============================================================
// Valoraciones — microservicio de valoraciones
// ============================================================
//   POST /valoraciones { solicitudId, calificacion, comentario, autorRol } → Valoracion
//   (ms-valoraciones publica valoracion.registrada: ms-usuarios recalcula el promedio)

export interface ValoracionInput {
  solicitudId: string;
  calificacion: number;
  comentario?: string;
}

export async function enviarValoracion(input: ValoracionInput): Promise<Valoracion> {
  if (!USE_MOCKS) {
    // El cliente valora al técnico y el técnico al cliente
    const autorRol = useAuthStore.getState().user?.rol ?? 'cliente';
    return mapValoracion((await api.post<ValoracionApi>('/valoraciones', { ...input, autorRol })).data);
  }
  await delay(700);
  return {
    ...input,
    id: `val-${Date.now()}`,
    clienteId: 'cliente-001',
    tecnicoId: 'tecnico-001',
    creadoEn: new Date().toISOString(),
  };
}
