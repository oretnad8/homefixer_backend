'use client';

import { useEffect, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { LoaderCircle } from 'lucide-react';
import { OAUTH_PENDIENTE_KEY, RolNoAsignadoError, useAuth } from '@/hooks/useAuth';
import { LOGIN_PATH } from '@/lib/session';
import type { UserRole } from '@/lib/types';

// ============================================================
// /auth/callback — vuelta del login con Google/Facebook (Supabase)
// ============================================================
// Supabase redirige aquí con la sesión en el fragmento de la URL:
//   /auth/callback#access_token=…&refresh_token=…&expires_in=…
// o con #error=…&error_description=… si el usuario canceló.

export default function OAuthCallbackPage() {
  const router = useRouter();
  const { completarOAuth } = useAuth();
  const procesado = useRef(false);

  useEffect(() => {
    if (procesado.current) return; // evita doble ejecución en StrictMode
    procesado.current = true;

    let rol: UserRole = 'cliente';
    let next: string | null = null;
    try {
      const pendiente = JSON.parse(sessionStorage.getItem(OAUTH_PENDIENTE_KEY) ?? '{}');
      if (pendiente.rol === 'tecnico' || pendiente.rol === 'admin') rol = pendiente.rol;
      next = pendiente.next ?? null;
    } catch {
      // sin datos guardados: se asume cliente
    }
    sessionStorage.removeItem(OAUTH_PENDIENTE_KEY);

    const params = new URLSearchParams(window.location.hash.slice(1));
    const accessToken = params.get('access_token');
    // Se limpia el token de la barra de direcciones / historial
    window.history.replaceState(null, '', window.location.pathname);

    const volverAlLogin = (error: string) => router.replace(`${LOGIN_PATH[rol]}?error=${error}`);

    if (!accessToken) {
      volverAlLogin('oauth');
      return;
    }

    completarOAuth(accessToken, rol, next).catch((e) => {
      volverAlLogin(
        e instanceof RolNoAsignadoError ? `rol-no-asignado&rol-actual=${e.rolActual ?? 'ninguno'}` : 'oauth'
      );
    });
  }, [completarOAuth, router]);

  return (
    <div className="flex min-h-dvh flex-col items-center justify-center gap-3 bg-surface-muted" role="status">
      <LoaderCircle className="size-8 animate-spin text-primary-500" aria-hidden />
      <p className="text-sm text-subtle">Iniciando sesión…</p>
    </div>
  );
}
