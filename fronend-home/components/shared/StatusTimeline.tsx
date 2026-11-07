import { Check } from 'lucide-react';
import { cn } from '@/lib/utils';

interface StatusTimelineProps {
  steps: string[];
  /** Índice del paso actual (los anteriores quedan completados) */
  current: number;
}

/** Línea de tiempo del estado del servicio: horizontal en mobile, igual en desktop */
export function StatusTimeline({ steps, current }: StatusTimelineProps) {
  return (
    <ol className="flex items-start" aria-label="Estado del servicio">
      {steps.map((step, i) => {
        const done = i < current;
        const active = i === current;
        return (
          <li key={step} className="relative flex flex-1 flex-col items-center text-center" aria-current={active ? 'step' : undefined}>
            {i > 0 && (
              <span
                aria-hidden
                className={cn('absolute right-1/2 top-4 h-0.5 w-full -translate-y-1/2', i <= current ? 'bg-primary-500' : 'bg-border')}
              />
            )}
            <span
              className={cn(
                'relative z-10 flex size-8 items-center justify-center rounded-pill border-2 text-xs font-bold',
                done && 'border-primary-500 bg-primary-500 text-white',
                active && 'border-primary-500 bg-surface text-primary-600 ring-4 ring-primary-100',
                !done && !active && 'border-border bg-surface text-subtle'
              )}
            >
              {done ? <Check className="size-4" aria-hidden /> : i + 1}
            </span>
            <span className={cn('mt-2 px-1 text-[11px] leading-tight md:text-xs', active ? 'font-semibold text-ink' : 'text-subtle')}>
              {step}
              {done && <span className="sr-only"> (completado)</span>}
            </span>
          </li>
        );
      })}
    </ol>
  );
}
