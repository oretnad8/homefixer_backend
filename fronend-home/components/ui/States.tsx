import type { ReactNode } from 'react';
import { CircleAlert, Inbox, RotateCcw } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Button } from './Button';

// ============================================================
// Skeleton, EmptyState y ErrorState — estados de carga compartidos
// ============================================================

export function Skeleton({ className }: { className?: string }) {
  return <div className={cn('skeleton rounded-input', className)} aria-hidden />;
}

/** Envoltorio accesible para un bloque de skeletons */
export function LoadingRegion({ label = 'Cargando…', children, className }: { label?: string; children: ReactNode; className?: string }) {
  return (
    <div role="status" aria-live="polite" aria-busy="true" className={className}>
      <span className="sr-only">{label}</span>
      {children}
    </div>
  );
}

interface EmptyStateProps {
  title: string;
  description?: string;
  icon?: ReactNode;
  action?: ReactNode;
  className?: string;
}

export function EmptyState({ title, description, icon, action, className }: EmptyStateProps) {
  return (
    <div className={cn('flex flex-col items-center rounded-card border border-dashed border-border bg-surface px-6 py-12 text-center', className)}>
      <span className="mb-4 flex size-14 items-center justify-center rounded-pill bg-primary-50 text-primary-500 [&>svg]:size-7" aria-hidden>
        {icon ?? <Inbox />}
      </span>
      <h3 className="text-base font-semibold text-ink">{title}</h3>
      {description && <p className="mt-1 max-w-sm text-sm text-subtle">{description}</p>}
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}

export function ErrorState({ message, onRetry, className }: { message?: string; onRetry?: () => void; className?: string }) {
  return (
    <div role="alert" className={cn('flex flex-col items-center rounded-card border border-danger/20 bg-danger-light px-6 py-10 text-center', className)}>
      <CircleAlert className="mb-3 size-8 text-danger" aria-hidden />
      <h3 className="text-base font-semibold text-danger-dark">Algo salió mal</h3>
      <p className="mt-1 max-w-sm text-sm text-danger-dark/80">{message ?? 'No pudimos cargar la información.'}</p>
      {onRetry && (
        <Button variant="danger" size="sm" width="auto" className="mt-5" leftIcon={<RotateCcw className="size-4" />} onClick={onRetry}>
          Reintentar
        </Button>
      )}
    </div>
  );
}
