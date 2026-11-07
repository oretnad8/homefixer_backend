import type { HTMLAttributes, ReactNode } from 'react';
import { cn } from '@/lib/utils';

// ============================================================
// Card — contenedor base para listas mobile y grids desktop
// ============================================================

interface CardProps extends HTMLAttributes<HTMLElement> {
  /** Aumenta la sombra y marca foco/hover (cards clicables) */
  interactive?: boolean;
  padding?: 'none' | 'sm' | 'md' | 'lg';
  as?: 'div' | 'section' | 'article';
}

const paddings = {
  none: '',
  sm: 'p-3',
  md: 'p-4 md:p-5',
  lg: 'p-5 md:p-8',
};

export function Card({ interactive, padding = 'md', as: Tag = 'div', className, ...rest }: CardProps) {
  return (
    <Tag
      className={cn(
        'rounded-card border border-border bg-surface shadow-card',
        interactive && 'transition-shadow hover:shadow-card-md focus-within:shadow-card-md',
        paddings[padding],
        className
      )}
      {...rest}
    />
  );
}

export function CardHeader({ title, action, className }: { title: ReactNode; action?: ReactNode; className?: string }) {
  return (
    <div className={cn('mb-4 flex items-center justify-between gap-3', className)}>
      <h2 className="text-base font-semibold text-ink md:text-lg">{title}</h2>
      {action}
    </div>
  );
}
