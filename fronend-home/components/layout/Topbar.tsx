'use client';

import { useEffect, useState } from 'react';
import { Bell, Menu } from 'lucide-react';
import { Modal } from '@/components/ui/Modal';
import { LoadingRegion, Skeleton } from '@/components/ui/States';
import { Toggle } from '@/components/ui/Toggle';
import { marcarNotificacionesLeidas, useNotificaciones } from '@/hooks/useNotificaciones';
import { actualizarDisponibilidad } from '@/hooks/useTecnicos';
import type { Notificacion, UserRole } from '@/lib/types';
import { cn, formatRelativeDate } from '@/lib/utils';
import { useAuthStore } from '@/store/authStore';
import { useUIStore } from '@/store/uiStore';

// ============================================================
// Topbar
// ============================================================
// Mobile: barra fija azul (primary-500, continúa en el degradado vertical
//         de los headers de página), hamburguesa + título + campana.
// Desktop (lg): barra blanca integrada en el área de contenido, sin
// hamburguesa (el sidebar ya está visible) y con acciones a la derecha.

/** Cada cuánto se consulta si hay notificaciones nuevas */
const POLL_NOTIFICACIONES_MS = 30000;

export function Topbar({ rol }: { rol: UserRole }) {
  const title = useUIStore((s) => s.pageTitle);
  const open = useUIStore((s) => s.sidebarOpen);
  const openSidebar = useUIStore((s) => s.openSidebar);
  const usuarioId = useAuthStore((s) => s.user?.id ?? '');
  const [notifOpen, setNotifOpen] = useState(false);
  const notificaciones = useNotificaciones(true);
  const count = notificaciones.data?.filter((n) => !n.leida).length ?? 0;

  // Las notificaciones las genera ms-notificacion a partir de los eventos de RabbitMQ
  const { refetch } = notificaciones;
  useEffect(() => {
    const id = setInterval(refetch, POLL_NOTIFICACIONES_MS);
    return () => clearInterval(id);
  }, [refetch]);

  const cerrarNotificaciones = () => {
    setNotifOpen(false);
    if (count > 0) void marcarNotificacionesLeidas(usuarioId).then(refetch);
  };

  return (
    <header
      className={cn(
        'sticky top-0 z-20 flex h-16 shrink-0 items-center gap-2 px-2',
        'bg-primary-500 text-white',
        'lg:border-b lg:border-border lg:bg-surface/90 lg:bg-none lg:px-8 lg:text-ink lg:backdrop-blur'
      )}
    >
      <button
        type="button"
        onClick={openSidebar}
        className="flex size-11 items-center justify-center rounded-pill hover:bg-white/15 lg:hidden"
        aria-label="Abrir menú"
        aria-expanded={open}
        aria-controls="mobile-drawer"
      >
        <Menu className="size-6" />
      </button>

      <h1 className="min-w-0 flex-1 truncate text-lg font-semibold lg:text-xl">{title}</h1>

      <div className="flex items-center gap-2 lg:gap-4">
        {rol === 'tecnico' && <DisponibilidadToggle className="hidden lg:inline-flex" />}
        <button
          type="button"
          onClick={() => setNotifOpen(true)}
          className="relative flex size-11 items-center justify-center rounded-pill hover:bg-white/15 lg:hover:bg-surface-muted"
          aria-label={`Notificaciones${count ? `, ${count} sin leer` : ''}`}
        >
          <Bell className="size-6 lg:size-5" />
          {count > 0 && (
            <span className="absolute right-0.5 top-0.5 flex size-[18px] items-center justify-center rounded-pill bg-warning text-[10px] font-bold text-white ring-2 ring-primary-500 lg:right-1 lg:top-1 lg:ring-surface">
              {count}
            </span>
          )}
        </button>
      </div>

      <NotificacionesModal
        open={notifOpen}
        onClose={cerrarNotificaciones}
        data={notificaciones.data}
        isLoading={notificaciones.isLoading && !notificaciones.data}
      />
    </header>
  );
}

/** Toggle de disponibilidad del técnico (topbar en desktop, home en mobile) */
export function DisponibilidadToggle({ className }: { className?: string }) {
  const disponible = useUIStore((s) => s.disponible);
  const setDisponible = useUIStore((s) => s.setDisponible);
  return (
    <div
      className={cn(
        'items-center gap-2 rounded-pill border px-3 py-1.5',
        disponible ? 'border-success/30 bg-success-light text-success-dark' : 'border-border bg-surface-muted text-subtle',
        className
      )}
    >
      <Toggle
        label={disponible ? 'Disponible' : 'No disponible'}
        checked={disponible}
        onChange={(v) => {
          setDisponible(v);
          void actualizarDisponibilidad(v);
        }}
      />
    </div>
  );
}

interface NotificacionesModalProps {
  open: boolean;
  onClose: () => void;
  data: Notificacion[] | undefined;
  isLoading: boolean;
}

function NotificacionesModal({ open, onClose, data, isLoading }: NotificacionesModalProps) {
  return (
    <Modal open={open} onClose={onClose} title="Notificaciones">
      {isLoading || !data ? (
        <LoadingRegion className="space-y-3">
          {[0, 1, 2].map((i) => (
            <Skeleton key={i} className="h-16" />
          ))}
        </LoadingRegion>
      ) : data.length === 0 ? (
        <p className="py-8 text-center text-sm text-subtle">No tienes notificaciones.</p>
      ) : (
        <ul className="divide-y divide-border text-ink">
          {data.map((n) => (
            <li key={n.id} className="flex gap-3 py-3">
              <span
                className={cn('mt-1.5 size-2 shrink-0 rounded-pill', n.leida ? 'bg-transparent' : 'bg-primary-500')}
                aria-label={n.leida ? undefined : 'No leída'}
              />
              <div>
                <p className="text-sm font-semibold">{n.titulo}</p>
                <p className="text-sm text-subtle">{n.detalle}</p>
                <p className="mt-1 text-xs text-subtle">{formatRelativeDate(n.creadoEn)}</p>
              </div>
            </li>
          ))}
        </ul>
      )}
    </Modal>
  );
}
