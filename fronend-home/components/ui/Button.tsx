import Link from 'next/link';
import { forwardRef, type ButtonHTMLAttributes, type ReactNode } from 'react';
import { LoaderCircle } from 'lucide-react';
import { cn } from '@/lib/utils';

// ============================================================
// Button — primary | success | danger | outline | outline-danger | ghost | white
// ============================================================
// width:
//   'responsive' (default) → ancho completo en mobile, automático desde md
//   'full'                 → siempre ancho completo
//   'auto'                 → siempre ancho automático
// Si se pasa `href` se renderiza un <Link> con el mismo estilo.

export type ButtonVariant = 'primary' | 'success' | 'danger' | 'outline' | 'outline-danger' | 'ghost' | 'white';
export type ButtonSize = 'sm' | 'md' | 'lg';
type ButtonWidth = 'responsive' | 'full' | 'auto';

const variants: Record<ButtonVariant, string> = {
  primary: 'bg-primary-500 text-white hover:bg-primary-600 active:bg-primary-700 shadow-card',
  success: 'bg-success text-white hover:bg-success-dark shadow-card',
  danger: 'bg-danger text-white hover:bg-danger-dark shadow-card',
  outline: 'border-2 border-primary-500 text-primary-600 bg-surface hover:bg-primary-50',
  'outline-danger': 'border-2 border-danger/40 text-danger bg-surface hover:bg-danger-light',
  ghost: 'text-primary-600 hover:bg-primary-50',
  white: 'bg-surface text-primary-700 hover:bg-primary-50 shadow-card',
};

const sizes: Record<ButtonSize, string> = {
  sm: 'h-9 px-4 text-sm gap-1.5',
  md: 'h-11 px-5 text-sm gap-2',
  lg: 'h-12 px-6 text-base gap-2',
};

const widths: Record<ButtonWidth, string> = {
  responsive: 'w-full md:w-auto',
  full: 'w-full',
  auto: 'w-auto',
};

interface CommonProps {
  variant?: ButtonVariant;
  size?: ButtonSize;
  width?: ButtonWidth;
  loading?: boolean;
  leftIcon?: ReactNode;
  rightIcon?: ReactNode;
  className?: string;
  children?: ReactNode;
}

type ButtonProps = CommonProps &
  ButtonHTMLAttributes<HTMLButtonElement> & {
    href?: undefined;
  };

type LinkButtonProps = CommonProps & {
  href: string;
  'aria-label'?: string;
  onClick?: () => void;
};

export function buttonClasses({
  variant = 'primary',
  size = 'md',
  width = 'responsive',
  className,
}: Pick<CommonProps, 'variant' | 'size' | 'width' | 'className'>) {
  return cn(
    'inline-flex shrink-0 items-center justify-center rounded-pill font-semibold transition-colors',
    'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary-500',
    'disabled:pointer-events-none disabled:opacity-50',
    variants[variant],
    sizes[size],
    widths[width],
    className
  );
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps | LinkButtonProps>(function Button(
  props,
  ref
) {
  const { variant, size, width, loading, leftIcon, rightIcon, className, children, ...rest } = props;
  const classes = buttonClasses({ variant, size, width, className });
  const content = (
    <>
      {loading ? <LoaderCircle className="size-4 animate-spin" aria-hidden /> : leftIcon}
      {children}
      {!loading && rightIcon}
    </>
  );

  if ('href' in rest && rest.href !== undefined) {
    const { href, ...linkRest } = rest as LinkButtonProps;
    return (
      <Link href={href} className={classes} {...linkRest}>
        {content}
      </Link>
    );
  }

  const { disabled, type = 'button', ...buttonRest } = rest as ButtonHTMLAttributes<HTMLButtonElement>;
  return (
    <button
      ref={ref}
      type={type}
      className={classes}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      {...buttonRest}
    >
      {content}
    </button>
  );
});
