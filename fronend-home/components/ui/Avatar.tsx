/* eslint-disable @next/next/no-img-element -- avatares remotos de origen arbitrario */
import { cn, getInitials } from '@/lib/utils';

interface AvatarProps {
  nombre: string;
  apellido?: string;
  src?: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  /** Punto verde de "disponible" */
  online?: boolean;
  className?: string;
}

const sizes = {
  sm: 'size-9 text-xs',
  md: 'size-12 text-sm',
  lg: 'size-16 text-lg',
  xl: 'size-24 text-2xl',
};

export function Avatar({ nombre, apellido, src, size = 'md', online, className }: AvatarProps) {
  const alt = `${nombre} ${apellido ?? ''}`.trim();
  return (
    <span className={cn('relative inline-flex shrink-0', className)}>
      {src ? (
        <img src={src} alt={alt} className={cn('rounded-pill object-cover', sizes[size])} />
      ) : (
        <span
          role="img"
          aria-label={alt}
          className={cn(
            'flex items-center justify-center rounded-pill bg-gradient-brand font-semibold text-white',
            sizes[size]
          )}
        >
          {getInitials(nombre, apellido)}
        </span>
      )}
      {online !== undefined && (
        <span
          className={cn(
            'absolute bottom-0 right-0 size-3 rounded-pill ring-2 ring-surface',
            online ? 'bg-success' : 'bg-subtle'
          )}
          aria-label={online ? 'Disponible' : 'No disponible'}
          role="status"
        />
      )}
    </span>
  );
}
