'use client';

import { useEffect, useId, useRef, useSyncExternalStore, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { X } from 'lucide-react';
import { useIsDesktopish } from '@/hooks/useMediaQuery';
import { cn } from '@/lib/utils';

// ============================================================
// Modal — bottom sheet en mobile, modal centrado desde md
// ============================================================

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  children: ReactNode;
  footer?: ReactNode;
  size?: 'sm' | 'md' | 'lg';
}

const widths = { sm: 'md:max-w-sm', md: 'md:max-w-md', lg: 'md:max-w-2xl' };

export function Modal({ open, onClose, title, description, children, footer, size = 'md' }: ModalProps) {
  const titleId = useId();
  const descId = useId();
  const panelRef = useRef<HTMLDivElement>(null);
  const desktop = useIsDesktopish();
  const mounted = useSyncExternalStore(
    () => () => {},
    () => true,
    () => false
  );

  // Escape para cerrar, bloqueo de scroll y foco inicial / retorno del foco
  useEffect(() => {
    if (!open) return;
    const previo = document.activeElement as HTMLElement | null;
    const overflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    panelRef.current?.focus();
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => {
      document.body.style.overflow = overflow;
      window.removeEventListener('keydown', onKey);
      previo?.focus();
    };
  }, [open, onClose]);

  const animacion = desktop
    ? { initial: { opacity: 0, scale: 0.96 }, animate: { opacity: 1, scale: 1 }, exit: { opacity: 0, scale: 0.96 } }
    : { initial: { y: '100%' }, animate: { y: 0 }, exit: { y: '100%' } };

  // Portal al <body>: evita que un ancestro con transform/backdrop-filter
  // (p. ej. el Topbar) se convierta en el contenedor del position: fixed.
  if (!mounted) return null;

  return createPortal(
    <AnimatePresence>
      {open && (
        <div className="fixed inset-0 z-50 flex items-end justify-center md:items-center md:p-6">
          <motion.div
            className="absolute inset-0 bg-ink/50"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            aria-hidden
          />
          <motion.div
            ref={panelRef}
            role="dialog"
            aria-modal="true"
            aria-labelledby={titleId}
            aria-describedby={description ? descId : undefined}
            tabIndex={-1}
            {...animacion}
            transition={{ type: 'spring', damping: 30, stiffness: 320 }}
            className={cn(
              'relative flex max-h-[90dvh] w-full flex-col bg-surface shadow-card-lg outline-none',
              'rounded-t-3xl md:rounded-card',
              widths[size]
            )}
          >
            {/* Asa del bottom sheet (solo mobile) */}
            <div className="mx-auto mt-3 h-1.5 w-12 rounded-pill bg-border md:hidden" aria-hidden />
            <div className="flex items-start justify-between gap-4 px-5 pb-2 pt-4 md:px-6 md:pt-6">
              <div>
                <h2 id={titleId} className="text-lg font-semibold text-ink">
                  {title}
                </h2>
                {description && (
                  <p id={descId} className="mt-1 text-sm text-subtle">
                    {description}
                  </p>
                )}
              </div>
              <button
                type="button"
                onClick={onClose}
                className="-mr-2 flex size-10 shrink-0 items-center justify-center rounded-pill text-subtle hover:bg-surface-muted hover:text-ink"
                aria-label="Cerrar"
              >
                <X className="size-5" />
              </button>
            </div>
            <div className="scrollbar-thin overflow-y-auto px-5 pb-5 md:px-6 md:pb-6">{children}</div>
            {footer && (
              <div className="flex flex-col-reverse gap-3 border-t border-border px-5 py-4 pb-[max(1rem,env(safe-area-inset-bottom))] md:flex-row md:justify-end md:px-6">
                {footer}
              </div>
            )}
          </motion.div>
        </div>
      )}
    </AnimatePresence>,
    document.body
  );
}
