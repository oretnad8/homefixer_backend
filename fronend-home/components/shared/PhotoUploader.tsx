/* eslint-disable @next/next/no-img-element -- previsualización de object URLs locales */
'use client';

import { useId } from 'react';
import { ImagePlus, ImageIcon, X } from 'lucide-react';
import { cn } from '@/lib/utils';

interface PhotoUploaderProps {
  label: string;
  fotos: string[];
  onAdd?: (url: string) => void;
  onRemove?: (url: string) => void;
  max?: number;
}

/**
 * Selector de fotos con previsualización. En solo lectura (sin onAdd)
 * muestra las fotos o un placeholder si no hay ninguna.
 */
export function PhotoUploader({ label, fotos, onAdd, onRemove, max = 4 }: PhotoUploaderProps) {
  const inputId = useId();
  const editable = !!onAdd;

  return (
    <fieldset>
      <legend className="mb-2 text-sm font-semibold text-ink">
        {label} <span className="font-normal text-subtle">({fotos.length}{editable ? `/${max}` : ''})</span>
      </legend>
      <div className="grid grid-cols-3 gap-2 sm:grid-cols-4 lg:grid-cols-3 xl:grid-cols-4">
        {fotos.map((url, i) => (
          <div key={url} className="relative aspect-square overflow-hidden rounded-input border border-border">
            <img src={url} alt={`${label} ${i + 1}`} className="size-full object-cover" />
            {onRemove && (
              <button
                type="button"
                onClick={() => onRemove(url)}
                className="absolute right-1 top-1 flex size-7 items-center justify-center rounded-pill bg-ink/70 text-white hover:bg-ink"
                aria-label={`Eliminar ${label.toLowerCase()} ${i + 1}`}
              >
                <X className="size-4" />
              </button>
            )}
          </div>
        ))}

        {editable && fotos.length < max && (
          <label
            htmlFor={inputId}
            className="flex aspect-square cursor-pointer flex-col items-center justify-center gap-1 rounded-input border-2 border-dashed border-primary-200 bg-primary-50 text-primary-600 transition-colors hover:border-primary-400 focus-within:outline-2 focus-within:outline-primary-500"
          >
            <ImagePlus className="size-6" aria-hidden />
            <span className="text-xs font-medium">Agregar</span>
            <input
              id={inputId}
              type="file"
              accept="image/*"
              multiple
              className="sr-only"
              onChange={(e) => {
                Array.from(e.target.files ?? [])
                  .slice(0, max - fotos.length)
                  .forEach((f) => onAdd(URL.createObjectURL(f)));
                e.target.value = '';
              }}
            />
          </label>
        )}

        {!editable && fotos.length === 0 && (
          <div className={cn('col-span-full flex aspect-[3/1] flex-col items-center justify-center gap-1 rounded-input bg-surface-muted text-subtle')}>
            <ImageIcon className="size-6" aria-hidden />
            <span className="text-xs">Sin fotos</span>
          </div>
        )}
      </div>
    </fieldset>
  );
}
