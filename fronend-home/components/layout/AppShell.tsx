import type { ReactNode } from 'react';
import type { UserRole } from '@/lib/types';
import { MobileDrawer } from './MobileDrawer';
import { RoleGuard } from './RoleGuard';
import { SesionTecnico } from './SesionTecnico';
import { Sidebar } from './Sidebar';
import { Topbar } from './Topbar';

// ============================================================
// AppShell — shell común de Cliente y Técnico
// ============================================================
//  < lg : Topbar con degradado + drawer deslizante (hamburguesa)
//  ≥ lg : Sidebar fijo de 256px a la izquierda + topbar blanco integrado
//  Contenido: max-width 1200px centrado con padding responsivo.

export function AppShell({ rol, children }: { rol: UserRole; children: ReactNode }) {
  return (
    <RoleGuard rol={rol}>
      <a
        href="#contenido"
        className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded-pill focus:bg-surface focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:shadow-card-lg"
      >
        Saltar al contenido
      </a>
      {rol === 'tecnico' && <SesionTecnico />}
      <Sidebar rol={rol} />
      <MobileDrawer rol={rol} />
      <div className="flex min-h-dvh flex-col lg:pl-64">
        <Topbar rol={rol} />
        <main id="contenido" className="flex-1">
          <div className="mx-auto w-full max-w-[1200px] px-4 py-4 md:px-6 md:py-6 lg:px-8 lg:py-8">{children}</div>
        </main>
      </div>
    </RoleGuard>
  );
}
