import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';
import type { CategoriaServicio, EstadoSolicitud, NivelUrgencia } from './types';
import type { BadgeVariant } from '@/components/ui/Badge';

// ============================================================
// UTILIDADES GENERALES
// ============================================================

/** Combina clases de Tailwind de forma segura */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/** Formatea un número como moneda en pesos mexicanos */
export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('es-MX', {
    style: 'currency',
    currency: 'MXN',
    minimumFractionDigits: 0,
  }).format(amount);
}

/** Formatea una fecha relativa en español */
export function formatRelativeDate(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffMins < 1) return 'Ahora mismo';
  if (diffMins < 60) return `Hace ${diffMins} min`;
  if (diffHours < 24) return `Hace ${diffHours}h`;
  if (diffDays < 7) return `Hace ${diffDays} días`;
  return date.toLocaleDateString('es-MX', { day: 'numeric', month: 'short' });
}

/** Formatea una fecha completa */
export function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('es-MX', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

/** Devuelve las iniciales de un nombre completo */
export function getInitials(nombre: string, apellido?: string): string {
  const n = nombre.trim().charAt(0).toUpperCase();
  const a = apellido?.trim().charAt(0).toUpperCase() ?? '';
  return `${n}${a}`;
}

/** Etiqueta legible por categoría */
export const CATEGORIA_LABELS: Record<CategoriaServicio, string> = {
  plomeria: 'Plomería',
  electricidad: 'Electricidad',
  carpinteria: 'Carpintería',
  pintura: 'Pintura',
  cerrajeria: 'Cerrajería',
  limpieza: 'Limpieza',
  jardineria: 'Jardinería',
  refrigeracion: 'Refrigeración',
  otros: 'Otros',
};

/** Etiqueta y color por urgencia */
export const URGENCIA_CONFIG: Record<NivelUrgencia, { label: string; color: string }> = {
  normal: { label: 'Normal', color: 'text-subtle' },
  urgente: { label: 'Urgente', color: 'text-warning' },
  emergencia: { label: 'Emergencia', color: 'text-danger' },
};

/** Etiqueta y variante de Badge por estado de solicitud */
export const ESTADO_CONFIG: Record<EstadoSolicitud, { label: string; variant: BadgeVariant }> = {
  // Publicada a los especialistas cercanos; ninguno la ha aceptado todavía
  pendiente: { label: 'Esperando especialista', variant: 'pendiente' },
  aceptada: { label: 'Aceptada', variant: 'progreso' },
  en_progreso: { label: 'En Progreso', variant: 'progreso' },
  completada: { label: 'Pago pendiente', variant: 'pendiente' },
  pagada: { label: 'Pagada', variant: 'completado' },
  cancelada: { label: 'Cancelada', variant: 'danger' },
};

/** Simula latencia de red para los mocks */
export function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

/** Formatea hora corta (10:22) */
export function formatTime(dateStr: string): string {
  return new Date(dateStr).toLocaleTimeString('es-MX', {
    hour: '2-digit',
    minute: '2-digit',
  });
}

/** "Plomería" → "Técnico Plomero" (texto bajo el nombre en el menú del técnico) */
const OFICIO: Record<string, string> = {
  'Plomería': 'Plomero',
  'Electricidad': 'Electricista',
  'Carpintería': 'Carpintero',
  'Pintura': 'Pintor',
  'Cerrajería': 'Cerrajero',
  'Jardinería': 'Jardinero',
  'Refrigeración': 'en Refrigeración',
  'Limpieza': 'de Limpieza',
};

export function rolTecnicoLabel(especialidad?: string): string {
  if (!especialidad || especialidad === 'otros' || especialidad === 'Otros') return 'Técnico';
  const label = CATEGORIA_LABELS[especialidad as CategoriaServicio] ?? especialidad;
  return `Técnico ${OFICIO[label] ?? label}`;
}

/** Clave de categoría válida o 'otros' ("Electricidad" y "electricidad" → "electricidad") */
export function aCategoria(valor?: string | null): CategoriaServicio {
  const v = (valor ?? '').trim().toLowerCase();
  if (v in CATEGORIA_LABELS) return v as CategoriaServicio;
  const porLabel = (Object.keys(CATEGORIA_LABELS) as CategoriaServicio[]).find((k) => CATEGORIA_LABELS[k].toLowerCase() === v);
  return porLabel ?? 'otros';
}
