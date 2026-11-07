import {
  Activity,
  Briefcase,
  CalendarClock,
  ChartColumn,
  ClipboardList,
  CreditCard,
  House,
  ShieldCheck,
  Settings,
  User,
  Wrench,
  type LucideIcon,
} from 'lucide-react';
import type { UserRole } from './types';

// ============================================================
// NAVEGACIÓN POR ROL — fuente única para Sidebar (desktop) y MobileDrawer
// ============================================================

export interface NavItem {
  label: string;
  href: string;
  icon: LucideIcon;
  /** Prefijos de ruta que también marcan este item como activo */
  matches?: string[];
}

export const NAV_ITEMS: Record<UserRole, NavItem[]> = {
  cliente: [
    {
      label: 'Inicio',
      href: '/cliente/home',
      icon: House,
      matches: ['/cliente/nueva-solicitud', '/cliente/tecnicos-disponibles'],
    },
    {
      label: 'Mis Servicios',
      href: '/cliente/mis-servicios',
      icon: Wrench,
      matches: ['/cliente/servicio-en-progreso', '/cliente/chat', '/cliente/valoracion'],
    },
    { label: 'Historial', href: '/cliente/historial', icon: CalendarClock },
    { label: 'Pagos', href: '/cliente/pagos', icon: CreditCard, matches: ['/cliente/pago/'] },
    { label: 'Mi Perfil', href: '/cliente/perfil', icon: User },
    { label: 'Configuración', href: '/cliente/configuracion', icon: Settings },
  ],
  tecnico: [
    { label: 'Inicio', href: '/tecnico/home', icon: House },
    { label: 'Solicitudes', href: '/tecnico/solicitudes', icon: ClipboardList },
    {
      label: 'Mis Trabajos',
      href: '/tecnico/mis-trabajos',
      icon: Briefcase,
      matches: ['/tecnico/servicio-en-progreso', '/tecnico/servicio-completado', '/tecnico/valoracion'],
    },
    { label: 'Estadísticas', href: '/tecnico/estadisticas', icon: ChartColumn },
    { label: 'Mi Perfil', href: '/tecnico/perfil', icon: User },
    { label: 'Configuración', href: '/tecnico/configuracion', icon: Settings },
  ],
  admin: [
    { label: 'Certificaciones', href: '/admin/certificaciones', icon: ShieldCheck },
    { label: 'Streaming en vivo', href: '/admin/streaming', icon: Activity },
  ],
};

export function isNavItemActive(item: NavItem, pathname: string) {
  if (pathname === item.href || pathname.startsWith(`${item.href}/`)) return true;
  return item.matches?.some((m) => pathname.startsWith(m)) ?? false;
}
