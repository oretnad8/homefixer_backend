import type { ReactNode } from 'react';
import { AppShell } from '@/components/layout/AppShell';

// Menú: Inicio, Mis Servicios, Historial, Pagos, Mi Perfil, Configuración, Cerrar Sesión
// (definido en lib/navigation.ts)
export default function ClienteLayout({ children }: { children: ReactNode }) {
  return <AppShell rol="cliente">{children}</AppShell>;
}
