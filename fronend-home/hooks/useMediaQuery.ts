'use client';

import { useSyncExternalStore } from 'react';

/** true si el media query coincide (false en el servidor) */
export function useMediaQuery(query: string): boolean {
  return useSyncExternalStore(
    (onChange) => {
      const mql = window.matchMedia(query);
      mql.addEventListener('change', onChange);
      return () => mql.removeEventListener('change', onChange);
    },
    () => window.matchMedia(query).matches,
    () => false
  );
}

/** Breakpoint md de Tailwind (≥ 768px) */
export const useIsDesktopish = () => useMediaQuery('(min-width: 768px)');
