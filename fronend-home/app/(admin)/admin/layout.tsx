import type { ReactNode } from 'react';
import { AppShell } from '@/components/layout/AppShell';

// Menú: Certificaciones, Cerrar Sesión (definido en lib/navigation.ts)
export default function AdminLayout({ children }: { children: ReactNode }) {
  return <AppShell rol="admin">{children}</AppShell>;
}
