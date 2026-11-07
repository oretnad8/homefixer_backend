'use client';

import { useEffect, useSyncExternalStore, type ReactNode } from 'react';
import { useRouter } from 'next/navigation';
import { LoaderCircle } from 'lucide-react';
import { LOGIN_PATH, clearSession } from '@/lib/session';
import type { UserRole } from '@/lib/types';
import { useAuthStore } from '@/store/authStore';

// ============================================================
// RoleGuard — segunda línea de protección (la primera es proxy.ts)
// ============================================================
// Espera a que Zustand rehidrate la sesión desde localStorage y, si no hay
// usuario o el rol no coincide, redirige al login correspondiente.

function useHydrated() {
  return useSyncExternalStore(
    (cb) => useAuthStore.persist.onFinishHydration(cb),
    () => useAuthStore.persist.hasHydrated(),
    () => false
  );
}

export function RoleGuard({ rol, children }: { rol: UserRole; children: ReactNode }) {
  const router = useRouter();
  const hydrated = useHydrated();
  const user = useAuthStore((s) => s.user);
  const autorizado = hydrated && user?.rol === rol;

  useEffect(() => {
    if (hydrated && user?.rol !== rol) {
      // Se limpia la cookie: si quedara, proxy.ts devolvería del login al
      // home y se produciría un bucle de redirecciones.
      clearSession();
      router.replace(LOGIN_PATH[rol]);
    }
  }, [hydrated, user, rol, router]);

  if (!autorizado) {
    return (
      <div className="flex min-h-dvh items-center justify-center bg-surface-muted" role="status">
        <LoaderCircle className="size-8 animate-spin text-primary-500" aria-hidden />
        <span className="sr-only">Verificando sesión…</span>
      </div>
    );
  }

  return <>{children}</>;
}
