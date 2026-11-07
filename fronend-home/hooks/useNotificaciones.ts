'use client';

import api, { USE_MOCKS } from '@/lib/api';
import { mockNotificaciones } from '@/lib/mock-data';
import { mapNotificacion, type NotificacionApi } from '@/lib/mappers';
import type { Notificacion } from '@/lib/types';
import { delay } from '@/lib/utils';
import { useAuthStore } from '@/store/authStore';
import { useQuery } from './useQuery';

//   GET   /notificaciones?usuarioId= → Notificacion[] (ms-notificacion las crea desde los eventos de RabbitMQ)
//   PATCH /notificaciones/leidas?usuarioId=
export function useNotificaciones(enabled: boolean) {
  const usuarioId = useAuthStore((s) => s.user?.id ?? '');
  return useQuery<Notificacion[]>(enabled && usuarioId ? `notificaciones:${usuarioId}` : null, async () => {
    if (USE_MOCKS) {
      await delay(400);
      return mockNotificaciones;
    }
    return (await api.get<NotificacionApi[]>('/notificaciones', { params: { usuarioId } })).data.map(mapNotificacion);
  });
}

export async function marcarNotificacionesLeidas(usuarioId: string) {
  if (USE_MOCKS) return;
  await api.patch('/notificaciones/leidas', null, { params: { usuarioId } });
}
