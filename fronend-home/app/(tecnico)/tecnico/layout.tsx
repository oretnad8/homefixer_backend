import type { ReactNode } from 'react';
import { AppShell } from '@/components/layout/AppShell';

// Menú: Inicio, Solicitudes, Mis Trabajos, Estadísticas, Mi Perfil, Configuración, Cerrar Sesión
// (definido en lib/navigation.ts)
export default function TecnicoLayout({ children }: { children: ReactNode }) {
  return <AppShell rol="tecnico">{children}</AppShell>;
}
