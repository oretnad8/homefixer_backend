'use client';

import { create } from 'zustand';

// ============================================================
// STORE DE UI
// ============================================================

interface UIState {
  /** Drawer lateral en mobile (en desktop el sidebar siempre está visible) */
  sidebarOpen: boolean;
  pageTitle: string;
  notificacionesCount: number;
  /** Disponibilidad del técnico (toggle en topbar desktop / home mobile) */
  disponible: boolean;

  openSidebar: () => void;
  closeSidebar: () => void;
  toggleSidebar: () => void;
  setPageTitle: (title: string) => void;
  setNotificaciones: (count: number) => void;
  setDisponible: (value: boolean) => void;
}

export const useUIStore = create<UIState>((set) => ({
  sidebarOpen: false,
  pageTitle: 'HomeFixer',
  notificacionesCount: 2,
  disponible: true,

  openSidebar: () => set({ sidebarOpen: true }),
  closeSidebar: () => set({ sidebarOpen: false }),
  toggleSidebar: () => set((s) => ({ sidebarOpen: !s.sidebarOpen })),
  setPageTitle: (title) => set({ pageTitle: title }),
  setNotificaciones: (count) => set({ notificacionesCount: count }),
  setDisponible: (value) => set({ disponible: value }),
}));
