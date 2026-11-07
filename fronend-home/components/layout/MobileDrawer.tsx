'use client';

import { useEffect } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { X } from 'lucide-react';
import type { UserRole } from '@/lib/types';
import { useUIStore } from '@/store/uiStore';
import { NavContent } from './NavContent';

/**
 * Drawer deslizante para < lg. Se abre desde el botón hamburguesa del Topbar
 * y comparte contenido (NavContent) con el Sidebar de desktop.
 */
export function MobileDrawer({ rol }: { rol: UserRole }) {
  const open = useUIStore((s) => s.sidebarOpen);
  const close = useUIStore((s) => s.closeSidebar);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && close();
    // Si la ventana crece a desktop con el drawer abierto, se cierra.
    const mql = window.matchMedia('(min-width: 1024px)');
    const onResize = () => mql.matches && close();
    const overflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    window.addEventListener('keydown', onKey);
    mql.addEventListener('change', onResize);
    return () => {
      document.body.style.overflow = overflow;
      window.removeEventListener('keydown', onKey);
      mql.removeEventListener('change', onResize);
    };
  }, [open, close]);

  return (
    <AnimatePresence>
      {open && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <motion.div
            className="absolute inset-0 bg-ink/50"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={close}
            aria-hidden
          />
          <motion.aside
            id="mobile-drawer"
            role="dialog"
            aria-modal="true"
            aria-label="Menú"
            className="absolute inset-y-0 left-0 w-[82%] max-w-xs bg-surface shadow-card-lg"
            initial={{ x: '-100%' }}
            animate={{ x: 0 }}
            exit={{ x: '-100%' }}
            transition={{ type: 'spring', damping: 32, stiffness: 320 }}
          >
            <button
              type="button"
              onClick={close}
              autoFocus
              className="absolute right-3 top-3 z-10 flex size-10 items-center justify-center rounded-pill text-subtle hover:bg-surface-muted hover:text-ink"
              aria-label="Cerrar menú"
            >
              <X className="size-5" />
            </button>
            <NavContent rol={rol} onNavigate={close} />
          </motion.aside>
        </div>
      )}
    </AnimatePresence>
  );
}
