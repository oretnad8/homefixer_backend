import type { ReactNode } from 'react';
import { cn } from '@/lib/utils';

/**
 * Header de página tipo "app" en mobile: continúa el azul del Topbar con un
 * degradado vertical y esquinas inferiores redondeadas. Desde lg se vuelve
 * una cabecera normal (sin fondo) dentro del área de contenido.
 */
export function PageHero({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <section
      className={cn(
        '-mx-4 -mt-4 rounded-b-3xl bg-gradient-brand-v px-4 pb-6 pt-2 text-white',
        'md:-mx-6 md:-mt-6 md:px-6',
        'lg:mx-0 lg:mt-0 lg:rounded-none lg:bg-none lg:p-0 lg:text-ink',
        className
      )}
    >
      {children}
    </section>
  );
}

/** Título de sección dentro de una página */
export function SectionTitle({ children, action, className }: { children: ReactNode; action?: ReactNode; className?: string }) {
  return (
    <div className={cn('mb-3 flex items-center justify-between gap-3 md:mb-4', className)}>
      <h2 className="text-lg font-semibold text-ink md:text-xl">{children}</h2>
      {action}
    </div>
  );
}
