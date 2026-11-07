'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import api, { USE_MOCKS } from '@/lib/api';
import { mockDb } from '@/lib/mock-db';
import { mapMensaje, type MensajeApi } from '@/lib/mappers';
import type { Mensaje } from '@/lib/types';
import { delay } from '@/lib/utils';
import { useAuthStore } from '@/store/authStore';
import { useQuery } from './useQuery';

// ============================================================
// useChat — microservicio de chat
// ============================================================
//   GET  /chat/:solicitudId/mensajes → Mensaje[]  (ms-solicitudes)
//   POST /chat/:solicitudId/mensajes { remitenteId, remitenteNombre, texto } → Mensaje
// Los mensajes nuevos de la otra parte se obtienen consultando cada POLL_MS;
// ms-solicitudes además publica chat.mensaje y ms-notificacion avisa al destinatario.

const POLL_MS = 4000;

const RESPUESTAS_MOCK = [
  'Entendido, lo reviso en cuanto llegue.',
  'Perfecto, gracias por avisar.',
  'Sí, llevo el material necesario.',
];

export function useChat(solicitudId: string | undefined) {
  const user = useAuthStore((s) => s.user);
  const [nuevos, setNuevos] = useState<Mensaje[]>([]);
  const [escribiendo, setEscribiendo] = useState(false);
  const timers = useRef<ReturnType<typeof setTimeout>[]>([]);

  const query = useQuery<Mensaje[]>(solicitudId ? `chat:${solicitudId}` : null, async () => {
    if (USE_MOCKS) {
      await delay(500);
      return mockDb.mensajes.filter((m) => m.solicitudId === solicitudId);
    }
    return (await api.get<MensajeApi[]>(`/chat/${solicitudId}/mensajes`)).data.map(mapMensaje);
  });

  // Consulta periódica para recibir los mensajes de la otra parte
  const { refetch } = query;
  useEffect(() => {
    if (USE_MOCKS || !solicitudId) return;
    const id = setInterval(refetch, POLL_MS);
    return () => clearInterval(id);
  }, [solicitudId, refetch]);

  useEffect(() => {
    const pendientes = timers.current;
    return () => pendientes.forEach(clearTimeout);
  }, []);

  const enviar = useCallback(
    async (texto: string) => {
      if (!solicitudId || !texto.trim()) return;
      const esCliente = user?.rol !== 'tecnico';
      const mensaje: Mensaje = {
        id: `msg-${Date.now()}`,
        solicitudId,
        remitenteId: user?.id ?? 'cliente-001',
        remitenteNombre: user ? `${user.nombre} ${user.apellido}`.trim() : 'Tú',
        esCliente,
        texto: texto.trim(),
        creadoEn: new Date().toISOString(),
      };
      setNuevos((prev) => [...prev, mensaje]);

      if (!USE_MOCKS) {
        const { data } = await api.post<MensajeApi>(`/chat/${solicitudId}/mensajes`, {
          remitenteId: Number(mensaje.remitenteId),
          remitenteNombre: mensaje.remitenteNombre,
          texto: mensaje.texto,
        });
        // Se reemplaza el mensaje optimista por el guardado (con su id real)
        const guardado = mapMensaje(data);
        setNuevos((prev) => prev.map((m) => (m.id === mensaje.id ? guardado : m)));
        return;
      }

      mockDb.mensajes.push(mensaje);
      // Respuesta simulada de la otra parte
      setEscribiendo(true);
      timers.current.push(
        setTimeout(() => {
          const respuesta: Mensaje = {
            id: `msg-${Date.now()}`,
            solicitudId,
            remitenteId: esCliente ? 'tecnico-001' : 'cliente-001',
            remitenteNombre: esCliente ? 'Carlos Ramírez' : 'María González',
            esCliente: !esCliente,
            texto: RESPUESTAS_MOCK[Math.floor(Math.random() * RESPUESTAS_MOCK.length)],
            creadoEn: new Date().toISOString(),
          };
          mockDb.mensajes.push(respuesta);
          setNuevos((prev) => [...prev, respuesta]);
          setEscribiendo(false);
        }, 1600)
      );
    },
    [solicitudId, user]
  );

  const iniciales = query.data ?? [];
  const idsIniciales = new Set(iniciales.map((m) => m.id));

  return {
    mensajes: [...iniciales, ...nuevos.filter((m) => !idsIniciales.has(m.id))],
    // Solo la primera carga muestra el indicador (no cada consulta periódica)
    isLoading: query.isLoading && !query.data,
    error: query.error,
    refetch: query.refetch,
    escribiendo,
    enviar,
  };
}
