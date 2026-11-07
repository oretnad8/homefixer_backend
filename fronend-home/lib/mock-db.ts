import {
  mockMensajes,
  mockSolicitudes,
  mockSolicitudesDisponibles,
  mockTecnicos,
} from './mock-data';
import type { Mensaje, Solicitud } from './types';

// ============================================================
// "BASE DE DATOS" EN MEMORIA PARA MOCKS
// ============================================================
// Permite que el flujo completo funcione sin backend: una solicitud creada en
// "Nueva Solicitud" aparece luego en "Servicio en Progreso", un mensaje enviado
// queda en el chat, etc. Se reinicia al recargar la página.

export const mockDb = {
  solicitudes: [...mockSolicitudes, ...mockSolicitudesDisponibles] as Solicitud[],
  mensajes: [...mockMensajes] as Mensaje[],
  rechazadas: new Set<string>(),
};

export function findSolicitud(id: string): Solicitud | undefined {
  return mockDb.solicitudes.find((s) => s.id === id);
}

export function updateSolicitud(id: string, cambios: Partial<Solicitud>): Solicitud {
  const actual = findSolicitud(id);
  if (!actual) throw new Error('La solicitud no existe');
  const nueva = { ...actual, ...cambios, actualizadoEn: new Date().toISOString() };
  mockDb.solicitudes = mockDb.solicitudes.map((s) => (s.id === id ? nueva : s));
  return nueva;
}

export function findTecnico(id: string) {
  return mockTecnicos.find((t) => t.id === id);
}
