import { Wrench } from 'lucide-react';
import { cn } from '@/lib/utils';

interface LogoProps {
  /** "light" para fondos con degradado, "dark" para fondos claros */
  tone?: 'light' | 'dark';
  size?: 'md' | 'lg';
  className?: string;
}

export function Logo({ tone = 'dark', size = 'md', className }: LogoProps) {
  return (
    <span className={cn('inline-flex items-center gap-2.5', className)}>
      <span
        className={cn(
          'flex items-center justify-center rounded-input',
          size === 'lg' ? 'size-12' : 'size-9',
          tone === 'light' ? 'bg-surface/20 text-white ring-1 ring-white/30' : 'bg-gradient-brand text-white'
        )}
        aria-hidden
      >
        <Wrench className={size === 'lg' ? 'size-6' : 'size-5'} />
      </span>
      <span
        className={cn(
          'font-bold tracking-tight',
          size === 'lg' ? 'text-3xl' : 'text-xl',
          tone === 'light' ? 'text-white' : 'text-ink'
        )}
      >
        Home<span className={tone === 'light' ? 'text-primary-100' : 'text-primary-500'}>fixer</span>
      </span>
    </span>
  );
}
