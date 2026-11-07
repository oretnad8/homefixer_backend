'use client';

import { useState } from 'react';
import { Card } from '@/components/ui/Card';
import { Toggle } from '@/components/ui/Toggle';
import type { UserRole } from '@/lib/types';

interface Preferencia {
  id: string;
  titulo: string;
  detalle: string;
}

const PREFERENCIAS: Record<Exclude<UserRole, 'admin'>, Preferencia[]> = {
  cliente: [
    { id: 'push', titulo: 'Notificaciones push', detalle: 'Avisos cuando tu técnico acepta o va en camino.' },
    { id: 'email', titulo: 'Correos de confirmación', detalle: 'Comprobantes de pago y resúmenes del servicio.' },
    { id: 'promos', titulo: 'Promociones', detalle: 'Descuentos y novedades de HomeFixer.' },
  ],
  tecnico: [
    { id: 'push', titulo: 'Nuevas solicitudes', detalle: 'Avisos inmediatos de solicitudes cerca de ti.' },
    { id: 'urgentes', titulo: 'Solo urgentes fuera de horario', detalle: 'Fuera de tu horario, solo recibe solicitudes urgentes.' },
    { id: 'email', titulo: 'Resumen semanal', detalle: 'Ganancias y valoraciones de la semana por correo.' },
  ],
};

/** Configuración de notificaciones (compartida por ambos roles) */
export function ConfiguracionView({ rol }: { rol: Exclude<UserRole, 'admin'> }) {
  const [valores, setValores] = useState<Record<string, boolean>>({ push: true, email: true });

  return (
    <Card padding="none" className="mx-auto max-w-2xl">
      <h2 className="px-5 pt-5 text-lg font-semibold text-ink md:px-8 md:pt-8">Notificaciones</h2>
      <ul className="divide-y divide-border">
        {PREFERENCIAS[rol].map((p) => (
          <li key={p.id} className="flex items-center justify-between gap-4 px-5 py-4 md:px-8">
            <div>
              <p className="font-medium text-ink">{p.titulo}</p>
              <p className="text-sm text-subtle">{p.detalle}</p>
            </div>
            <Toggle label={p.titulo} hideLabel checked={!!valores[p.id]} onChange={(v) => setValores((s) => ({ ...s, [p.id]: v }))} />
          </li>
        ))}
      </ul>
    </Card>
  );
}
