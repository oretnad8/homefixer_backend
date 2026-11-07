'use client';

import { cn } from '@/lib/utils';

interface ToggleProps {
  checked: boolean;
  onChange: (value: boolean) => void;
  label: string;
  /** Oculta visualmente el label (queda para lectores de pantalla) */
  hideLabel?: boolean;
  className?: string;
}

export function Toggle({ checked, onChange, label, hideLabel, className }: ToggleProps) {
  return (
    <label className={cn('inline-flex cursor-pointer items-center gap-3', className)}>
      <span className={cn('text-sm font-medium', hideLabel && 'sr-only')}>{label}</span>
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        aria-label={hideLabel ? label : undefined}
        onClick={() => onChange(!checked)}
        className={cn(
          'relative h-7 w-12 shrink-0 rounded-pill transition-colors',
          checked ? 'bg-success' : 'bg-border'
        )}
      >
        <span
          className={cn(
            'absolute left-0.5 top-0.5 size-6 rounded-pill bg-surface shadow-card transition-transform',
            checked && 'translate-x-5'
          )}
        />
      </button>
    </label>
  );
}
