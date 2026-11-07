'use client';

import api, { USE_MOCKS } from '@/lib/api';
import { mockPago } from '@/lib/mock-data';
import { findSolicitud, mockDb, updateSolicitud } from '@/lib/mock-db';
import { mapPago, type PagoApi } from '@/lib/mappers';
import type { MetodoPago, Pago } from '@/lib/types';
import { delay } from '@/lib/utils';
import { useAuthStore } from '@/store/authStore';
import { useQuery } from './useQuery';

// ============================================================
// usePagos — microservicio de pagos
// ============================================================
//   GET  /pagos/solicitud/:solicitudId → Pago (cobro PENDIENTE que ms-pagos genera
//        al recibir por RabbitMQ el evento de solicitud COMPLETADA)
//   POST /pagos/:id/procesar { metodoPago } → Pago (publica pago.procesado)
//   GET  /pagos/cliente/:clienteId → Pago[]

/** Comisión de la plataforma aplicada sobre el costo del servicio */
export const COMISION_SERVICIO = 0.05;

export function usePagoDeSolicitud(solicitudId: string | undefined) {
  return useQuery<Pago>(solicitudId ? `pago:${solicitudId}` : null, async () => {
    if (USE_MOCKS) {
      await delay(500);
      const sol = findSolicitud(solicitudId!);
      if (!sol) throw new Error('No encontramos el servicio a pagar.');
      return {
        ...mockPago,
        id: `pago-${sol.id}`,
        solicitudId: sol.id,
        tecnicoId: sol.tecnicoId ?? mockPago.tecnicoId,
        monto: sol.presupuestoEstimado ?? mockPago.monto,
      };
    }
    return mapPago((await api.get<PagoApi>(`/pagos/solicitud/${solicitudId}`)).data);
  });
}

export function useHistorialPagos() {
  const clienteId = useAuthStore((s) => s.user?.id ?? '');
  return useQuery<Pago[]>(`pagos-historial:${clienteId}`, async () => {
    if (USE_MOCKS) {
      await delay(600);
      return mockDb.solicitudes
        .filter((s) => s.clienteId === 'cliente-001' && (s.estado === 'completada' || s.estado === 'pagada'))
        .map((s) => ({
          ...mockPago,
          id: `pago-${s.id}`,
          solicitudId: s.id,
          monto: s.presupuestoEstimado ?? 0,
          estado: 'completado' as const,
          descripcion: s.descripcion,
          creadoEn: s.actualizadoEn,
        }));
    }
    return (await api.get<PagoApi[]>(`/pagos/cliente/${clienteId}`)).data.map(mapPago);
  });
}

export async function procesarPago(pagoId: string, metodoPago: MetodoPago): Promise<Pago> {
  if (!USE_MOCKS) return mapPago((await api.post<PagoApi>(`/pagos/${pagoId}/procesar`, { metodoPago })).data);
  await delay(1500);
  // En el backend esto lo hace ms-solicitudes al recibir el evento pago.procesado
  updateSolicitud(pagoId.replace(/^pago-/, ''), { estado: 'pagada' });
  return { ...mockPago, id: pagoId, metodoPago, estado: 'completado' };
}
