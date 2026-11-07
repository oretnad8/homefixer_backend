'use client';

import { useState } from 'react';
import { Star } from 'lucide-react';
import { cn } from '@/lib/utils';

// ============================================================
// StarRating — solo lectura o interactivo (radiogroup accesible)
// ============================================================

interface StarRatingProps {
  value: number;
  onChange?: (value: number) => void;
  size?: 'sm' | 'md' | 'lg';
  /** Muestra el número al lado (solo lectura) */
  showValue?: boolean;
  totalValoraciones?: number;
  className?: string;
  label?: string;
}

const sizes = { sm: 'size-4', md: 'size-6', lg: 'size-10 md:size-12' };

export function StarRating({
  value,
  onChange,
  size = 'sm',
  showValue,
  totalValoraciones,
  className,
  label = 'Calificación',
}: StarRatingProps) {
  const [hover, setHover] = useState(0);

  if (!onChange) {
    return (
      <span className={cn('inline-flex items-center gap-1', className)}>
        <span className="inline-flex" role="img" aria-label={`${label}: ${value.toFixed(1)} de 5`}>
          {[1, 2, 3, 4, 5].map((n) => (
            <Star
              key={n}
              aria-hidden
              className={cn(sizes[size], n <= Math.round(value) ? 'fill-gold text-gold' : 'fill-border text-border')}
            />
          ))}
        </span>
        {showValue && <span className="text-sm font-semibold text-ink">{value.toFixed(1)}</span>}
        {totalValoraciones !== undefined && <span className="text-xs text-subtle">({totalValoraciones})</span>}
      </span>
    );
  }

  const activo = hover || value;
  return (
    <div
      role="radiogroup"
      aria-label={label}
      className={cn('inline-flex gap-1 md:gap-2', className)}
      onMouseLeave={() => setHover(0)}
    >
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          role="radio"
          aria-checked={value === n}
          aria-label={`${n} ${n === 1 ? 'estrella' : 'estrellas'}`}
          onClick={() => onChange(n)}
          onMouseEnter={() => setHover(n)}
          onKeyDown={(e) => {
            if (e.key === 'ArrowRight' || e.key === 'ArrowUp') onChange(Math.min(5, value + 1));
            if (e.key === 'ArrowLeft' || e.key === 'ArrowDown') onChange(Math.max(1, value - 1));
          }}
          tabIndex={value === n || (value === 0 && n === 1) ? 0 : -1}
          className="rounded-input p-1 transition-transform hover:scale-110"
        >
          <Star className={cn(sizes[size], n <= activo ? 'fill-gold text-gold' : 'fill-transparent text-border')} />
        </button>
      ))}
    </div>
  );
}
