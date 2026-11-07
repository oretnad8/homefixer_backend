'use client';

import Link from 'next/link';
import { AlertTriangle, LocateFixed, LocateOff, Loader2, Wrench } from 'lucide-react';
import type { Tecnico } from '@/lib/types';
import { cn } from '@/lib/utils';
import { useAuthStore } from '@/store/authStore';
import { useUbicacionStore, type EstadoUbicacion } from '@/store/ubicacionStore';

// ============================================================
// EstadoUbicacionCard — ¿los clientes me ven en el mapa?
// ============================================================

const TEXTO: Record<EstadoUbicacion, { titulo: string; detalle: string; tono: 'ok' | 'info' | 'error' }> = {
  activa: {
    titulo: 'Compartiendo tu ubicación',
    detalle: 'Los clientes cercanos que buscan tu especialidad te ven en el mapa.',
    tono: 'ok',
  },
  solicitando: {
    titulo: 'Obteniendo tu ubicación…',
    detalle: 'Si el navegador lo pide, permite el acceso a tu ubicación.',
    tono: 'info',
  },
  inactiva: {
    titulo: 'No estás visible para los clientes',
    detalle: 'Activa "Disponible" para compartir tu ubicación y recibir solicitudes cercanas.',
    tono: 'info',
  },
  denegada: {
    titulo: 'Permiso de ubicación bloqueado',
    detalle: 'Toca el candado de la barra de direcciones → Ubicación → Permitir, y recarga la página.',
    tono: 'error',
  },
  'no-soportada': {
    titulo: 'Tu navegador no comparte ubicación',
    detalle: 'Usa Chrome, Edge, Safari o Firefox actualizados (con HTTPS).',
    tono: 'error',
  },
  error: {
    titulo: 'Problema con tu ubicación',
    detalle: 'Revisa que el GPS esté activo.',
    tono: 'error',
  },
};

export function EstadoUbicacionCard({ className }: { className?: string }) {
  const estado = useUbicacionStore((s) => s.estado);
  const mensaje = useUbicacionStore((s) => s.mensaje);
  const precision = useUbicacionStore((s) => s.posicion?.precision);
  const user = useAuthStore((s) => s.user) as Tecnico | null;
  const t = TEXTO[estado];
  const sinOficio = !user?.categoria || user.categoria === 'otros';

  const Icono = estado === 'activa' ? LocateFixed : estado === 'solicitando' ? Loader2 : t.tono === 'error' ? LocateOff : LocateFixed;

  return (
    <div className={cn('grid gap-3', className)}>
      <div
        role="status"
        className={cn(
          'flex items-start gap-3 rounded-card border px-4 py-3 text-sm',
          t.tono === 'ok' && 'border-success/30 bg-success-light text-success-dark',
          t.tono === 'info' && 'border-border bg-surface text-subtle',
          t.tono === 'error' && 'border-danger/30 bg-danger-light text-danger-dark'
        )}
      >
        <Icono className={cn('mt-0.5 size-5 shrink-0', estado === 'solicitando' && 'animate-spin')} aria-hidden />
        <div>
          <p className="font-semibold">{t.titulo}</p>
          <p>
            {estado === 'error' && mensaje ? mensaje : t.detalle}
            {estado === 'activa' && precision != null && ` Precisión ±${Math.round(precision)} m.`}
          </p>
        </div>
      </div>

      {sinOficio && (
        <Link
          href="/tecnico/perfil"
          className="flex items-start gap-3 rounded-card border border-warning/40 bg-warning-light px-4 py-3 text-sm text-warning-dark hover:underline"
        >
          <AlertTriangle className="mt-0.5 size-5 shrink-0" aria-hidden />
          <span>
            <strong className="block">Configura tu oficio</strong>
            Solo verás solicitudes y aparecerás para clientes de tu especialidad. Elige la tuya en Mi Perfil.
          </span>
          <Wrench className="ml-auto mt-0.5 size-4 shrink-0" aria-hidden />
        </Link>
      )}
    </div>
  );
}
