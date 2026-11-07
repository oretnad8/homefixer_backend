import type { ReactNode } from 'react';
import { CircleCheck, Clock, Crown, LoaderCircle, TriangleAlert } from 'lucide-react';
import { cn } from '@/lib/utils';

// ============================================================
// Badge — urgente | maestro | progreso | completado | pendiente | danger | neutral
// ============================================================

export type BadgeVariant = 'urgente' | 'maestro' | 'progreso' | 'completado' | 'pendiente' | 'danger' | 'neutral';

const styles: Record<BadgeVariant, string> = {
  urgente: 'bg-warning-light text-warning-dark',
  pendiente: 'bg-warning-light text-warning-dark',
  maestro: 'bg-gold-light text-gold-dark ring-1 ring-gold/30',
  progreso: 'bg-primary-100 text-primary-700',
  completado: 'bg-success-light text-success-dark',
  danger: 'bg-danger-light text-danger-dark',
  neutral: 'bg-surface-muted text-subtle ring-1 ring-border',
};

const icons: Partial<Record<BadgeVariant, ReactNode>> = {
  urgente: <TriangleAlert aria-hidden />,
  maestro: <Crown aria-hidden />,
  progreso: <LoaderCircle aria-hidden />,
  completado: <CircleCheck aria-hidden />,
  pendiente: <Clock aria-hidden />,
};

interface BadgeProps {
  variant?: BadgeVariant;
  children: ReactNode;
  icon?: boolean;
  className?: string;
}

export function Badge({ variant = 'neutral', children, icon = true, className }: BadgeProps) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1 whitespace-nowrap rounded-pill px-2.5 py-1 text-xs font-semibold [&>svg]:size-3.5',
        styles[variant],
        className
      )}
    >
      {icon && icons[variant]}
      {children}
    </span>
  );
}
