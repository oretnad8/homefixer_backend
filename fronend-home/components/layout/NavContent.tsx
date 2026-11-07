'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { LogOut } from 'lucide-react';
import { Avatar } from '@/components/ui/Avatar';
import { Badge } from '@/components/ui/Badge';
import { Logo } from '@/components/shared/Logo';
import { useAuth } from '@/hooks/useAuth';
import { NAV_ITEMS, isNavItemActive } from '@/lib/navigation';
import type { Tecnico, UserRole } from '@/lib/types';
import { cn, rolTecnicoLabel } from '@/lib/utils';

// ============================================================
// NavContent — contenido compartido por Sidebar (desktop) y MobileDrawer
// ============================================================
// Cliente: logo arriba. Técnico: card de perfil arriba.
// Ambos: items con íconos + botón rojo "Cerrar Sesión" fijo abajo.

interface NavContentProps {
  rol: UserRole;
  onNavigate?: () => void;
}

export function NavContent({ rol, onNavigate }: NavContentProps) {
  const pathname = usePathname();
  const { user, logout } = useAuth();
  const items = NAV_ITEMS[rol];

  return (
    <div className="flex h-full flex-col">
      {/* Cabecera */}
      {rol !== 'tecnico' ? (
        <div className="flex h-16 shrink-0 items-center px-6 lg:h-20">
          <Logo />
        </div>
      ) : (
        // pt-16 en el drawer (< lg) deja espacio al botón de cerrar
        <div className="px-4 pt-16 lg:pt-5">
          <div className="rounded-card bg-gradient-brand p-4 text-white">
            <div className="flex items-center gap-3">
              <Avatar nombre={user?.nombre ?? 'Técnico'} apellido={user?.apellido} size="md" className="rounded-pill ring-2 ring-white/40" />
              <div className="min-w-0">
                <p className="truncate font-semibold">
                  {user ? `${user.nombre} ${user.apellido}` : 'Técnico'}
                </p>
                <p className="truncate text-sm text-primary-100">
                  {rolTecnicoLabel((user as Tecnico | null)?.especialidad)}
                </p>
              </div>
            </div>
            {(user as Tecnico | null)?.esMaestro && (
              <Badge variant="maestro" className="mt-3">
                Técnico Maestro
              </Badge>
            )}
          </div>
        </div>
      )}

      {/* Items */}
      <nav aria-label="Navegación principal" className="scrollbar-thin mt-4 flex-1 overflow-y-auto px-3">
        <ul className="space-y-1">
          {items.map((item) => {
            const active = isNavItemActive(item, pathname);
            const Icon = item.icon;
            return (
              <li key={item.href}>
                <Link
                  href={item.href}
                  onClick={onNavigate}
                  aria-current={active ? 'page' : undefined}
                  className={cn(
                    'flex h-11 items-center gap-3 rounded-input px-3 text-sm font-medium transition-colors',
                    active ? 'bg-primary-50 text-primary-700' : 'text-subtle hover:bg-surface-muted hover:text-ink'
                  )}
                >
                  <Icon className={cn('size-5', active ? 'text-primary-500' : 'text-subtle')} aria-hidden />
                  {item.label}
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>

      {/* Pie: perfil (cliente) + cerrar sesión */}
      <div className="shrink-0 space-y-3 border-t border-border p-4">
        {rol === 'cliente' && user && (
          <div className="flex items-center gap-3 px-1">
            <Avatar nombre={user.nombre} apellido={user.apellido} size="sm" />
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-ink">
                {user.nombre} {user.apellido}
              </p>
              <p className="truncate text-xs text-subtle">{user.email}</p>
            </div>
          </div>
        )}
        <button
          type="button"
          onClick={() => logout()}
          className="flex h-11 w-full items-center justify-center gap-2 rounded-pill bg-danger text-sm font-semibold text-white transition-colors hover:bg-danger-dark"
        >
          <LogOut className="size-4" aria-hidden />
          Cerrar Sesión
        </button>
      </div>
    </div>
  );
}
