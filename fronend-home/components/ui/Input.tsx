'use client';

import { forwardRef, useId, useState, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes, type TextareaHTMLAttributes } from 'react';
import { ChevronDown, Eye, EyeOff } from 'lucide-react';
import { cn } from '@/lib/utils';

// ============================================================
// Input / TextArea / Select con label flotante
// ============================================================
// El label se apoya en el placeholder " " + peer-placeholder-shown para
// flotar cuando el campo tiene foco o contenido, sin JavaScript.

interface FieldBaseProps {
  label: string;
  error?: string;
  hint?: string;
}

const fieldBox =
  'peer block w-full rounded-input border bg-surface px-4 pb-2 pt-6 text-sm text-ink outline-none transition-colors ' +
  'placeholder:text-transparent focus:border-primary-500 focus:ring-2 focus:ring-primary-100 ' +
  'disabled:cursor-not-allowed disabled:bg-surface-muted';

const floatingLabel =
  'pointer-events-none absolute left-4 top-2 text-xs font-medium text-subtle transition-all ' +
  'peer-placeholder-shown:top-1/2 peer-placeholder-shown:-translate-y-1/2 peer-placeholder-shown:text-sm ' +
  'peer-focus:top-2 peer-focus:translate-y-0 peer-focus:text-xs peer-focus:text-primary-600';

function FieldMessage({ id, error, hint }: { id: string; error?: string; hint?: string }) {
  if (error)
    return (
      <p id={id} role="alert" className="mt-1.5 text-xs font-medium text-danger">
        {error}
      </p>
    );
  if (hint)
    return (
      <p id={id} className="mt-1.5 text-xs text-subtle">
        {hint}
      </p>
    );
  return null;
}

// ---------- Input ----------

interface InputProps extends FieldBaseProps, Omit<InputHTMLAttributes<HTMLInputElement>, 'placeholder'> {
  leftIcon?: ReactNode;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { label, error, hint, leftIcon, className, type = 'text', id, ...rest },
  ref
) {
  const autoId = useId();
  const inputId = id ?? autoId;
  const msgId = `${inputId}-msg`;
  const [visible, setVisible] = useState(false);
  const esPassword = type === 'password';

  return (
    <div className={className}>
      <div className="relative">
        {leftIcon && (
          <span className="pointer-events-none absolute left-4 top-1/2 -translate-y-1/2 text-subtle [&>svg]:size-5" aria-hidden>
            {leftIcon}
          </span>
        )}
        <input
          ref={ref}
          id={inputId}
          type={esPassword && visible ? 'text' : type}
          placeholder=" "
          aria-invalid={!!error || undefined}
          aria-describedby={error || hint ? msgId : undefined}
          className={cn(
            fieldBox,
            'h-14',
            leftIcon && 'pl-12',
            esPassword && 'pr-12',
            error ? 'border-danger focus:border-danger focus:ring-danger-light' : 'border-border'
          )}
          {...rest}
        />
        <label htmlFor={inputId} className={cn(floatingLabel, leftIcon && 'left-12')}>
          {label}
        </label>
        {esPassword && (
          <button
            type="button"
            onClick={() => setVisible((v) => !v)}
            className="absolute right-2 top-1/2 flex size-10 -translate-y-1/2 items-center justify-center rounded-pill text-subtle hover:text-ink"
            aria-label={visible ? 'Ocultar contraseña' : 'Mostrar contraseña'}
          >
            {visible ? <EyeOff className="size-5" /> : <Eye className="size-5" />}
          </button>
        )}
      </div>
      <FieldMessage id={msgId} error={error} hint={hint} />
    </div>
  );
});

// ---------- TextArea ----------

type TextAreaProps = FieldBaseProps & Omit<TextareaHTMLAttributes<HTMLTextAreaElement>, 'placeholder'>;

export const TextArea = forwardRef<HTMLTextAreaElement, TextAreaProps>(function TextArea(
  { label, error, hint, className, id, rows = 4, ...rest },
  ref
) {
  const autoId = useId();
  const inputId = id ?? autoId;
  const msgId = `${inputId}-msg`;

  return (
    <div className={className}>
      <div className="relative">
        <textarea
          ref={ref}
          id={inputId}
          rows={rows}
          placeholder=" "
          aria-invalid={!!error || undefined}
          aria-describedby={error || hint ? msgId : undefined}
          className={cn(
            fieldBox,
            'resize-y',
            error ? 'border-danger focus:border-danger focus:ring-danger-light' : 'border-border'
          )}
          {...rest}
        />
        <label
          htmlFor={inputId}
          className={cn(floatingLabel, 'peer-placeholder-shown:top-6 peer-placeholder-shown:translate-y-0')}
        >
          {label}
        </label>
      </div>
      <FieldMessage id={msgId} error={error} hint={hint} />
    </div>
  );
});

// ---------- Select ----------

interface SelectProps extends FieldBaseProps, SelectHTMLAttributes<HTMLSelectElement> {
  options: { value: string; label: string }[];
  placeholder?: string;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { label, error, hint, className, id, options, placeholder, ...rest },
  ref
) {
  const autoId = useId();
  const inputId = id ?? autoId;
  const msgId = `${inputId}-msg`;

  return (
    <div className={className}>
      <div className="relative">
        <select
          ref={ref}
          id={inputId}
          aria-invalid={!!error || undefined}
          aria-describedby={error || hint ? msgId : undefined}
          className={cn(
            fieldBox,
            'h-14 appearance-none pr-10',
            error ? 'border-danger focus:border-danger focus:ring-danger-light' : 'border-border'
          )}
          {...rest}
        >
          {placeholder && (
            <option value="" disabled>
              {placeholder}
            </option>
          )}
          {options.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
        {/* En un select el label siempre queda flotando */}
        <label htmlFor={inputId} className="pointer-events-none absolute left-4 top-2 text-xs font-medium text-subtle">
          {label}
        </label>
        <ChevronDown className="pointer-events-none absolute right-4 top-1/2 size-4 -translate-y-1/2 text-subtle" aria-hidden />
      </div>
      <FieldMessage id={msgId} error={error} hint={hint} />
    </div>
  );
});
