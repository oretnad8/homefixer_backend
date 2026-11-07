'use client';

import { useEffect } from 'react';
import { useUIStore } from '@/store/uiStore';

/** Define el título que muestra el Topbar para la pantalla actual */
export function usePageTitle(title: string) {
  const setPageTitle = useUIStore((s) => s.setPageTitle);
  useEffect(() => {
    setPageTitle(title);
    document.title = `${title} · HomeFixer`;
  }, [title, setPageTitle]);
}
