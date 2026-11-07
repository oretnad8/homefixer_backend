'use client';

import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { User, UserRole } from '@/lib/types';

// ============================================================
// STORE DE AUTENTICACIÓN
// ============================================================

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;

  login: (user: User, token: string) => void;
  logout: () => void;
  setLoading: (loading: boolean) => void;
  updateUser: (data: Partial<User>) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      isAuthenticated: false,
      isLoading: false,

      login: (user, token) => {
        set({ user, token, isAuthenticated: true, isLoading: false });
      },

      logout: () => {
        set({ user: null, token: null, isAuthenticated: false });
      },

      setLoading: (loading) => set({ isLoading: loading }),

      updateUser: (data) => {
        const current = get().user;
        if (current) {
          set({ user: { ...current, ...data } });
        }
      },
    }),
    {
      name: 'homefixer-auth',
      partialize: (state) => ({
        user: state.user,
        token: state.token,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);

/** Selector de conveniencia para el rol */
export const useUserRole = (): UserRole | null => {
  return useAuthStore((s) => s.user?.rol ?? null);
};
