'use client';

import { useState } from 'react';
import { useAuth, type OAuthProvider } from '@/hooks/useAuth';
import { SupabaseAuthError } from '@/lib/supabase';
import type { UserRole } from '@/lib/types';
import { Button } from '@/components/ui/Button';

// Logos oficiales de marca (sus colores son parte de la marca, no del tema).
function GoogleLogo() {
  return (
    <svg viewBox="0 0 24 24" className="size-5" aria-hidden>
      <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.76h3.56c2.08-1.92 3.28-4.74 3.28-8.09Z" />
      <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.56-2.76c-.98.66-2.23 1.06-3.72 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84A11 11 0 0 0 12 23Z" />
      <path fill="#FBBC05" d="M5.84 14.11A6.6 6.6 0 0 1 5.5 12c0-.73.13-1.44.34-2.11V7.05H2.18A11 11 0 0 0 1 12c0 1.77.43 3.45 1.18 4.95l3.66-2.84Z" />
      <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15A10.96 10.96 0 0 0 12 1 11 11 0 0 0 2.18 7.05l3.66 2.84C6.71 7.3 9.14 5.38 12 5.38Z" />
    </svg>
  );
}

function FacebookLogo() {
  return (
    <svg viewBox="0 0 24 24" className="size-5" aria-hidden>
      <path
        fill="#1877F2"
        d="M24 12.07C24 5.4 18.63 0 12 0S0 5.4 0 12.07C0 18.1 4.39 23.1 10.13 24v-8.44H7.08v-3.49h3.04V9.41c0-3.02 1.8-4.7 4.54-4.7 1.31 0 2.68.24 2.68.24v2.97h-1.5c-1.5 0-1.96.93-1.96 1.89v2.26h3.33l-.53 3.49h-2.8V24C19.62 23.1 24 18.1 24 12.07Z"
      />
    </svg>
  );
}

interface SocialButtonsProps {
  rol: UserRole;
  next?: string | null;
  onError?: (mensaje: string) => void;
}

export function SocialButtons({ rol, next, onError }: SocialButtonsProps) {
  const { loginWithProvider } = useAuth();
  const [pendiente, setPendiente] = useState<OAuthProvider | null>(null);

  const entrar = async (p: OAuthProvider) => {
    setPendiente(p);
    try {
      await loginWithProvider(p, rol, next); // redirige a Google: no vuelve si todo va bien
    } catch (e) {
      onError?.(e instanceof SupabaseAuthError ? e.message : 'No se pudo iniciar sesión con el proveedor.');
      setPendiente(null);
    }
  };

  return (
    <div className="grid grid-cols-2 gap-3">
      <Button
        variant="outline"
        width="full"
        className="border border-border text-ink hover:bg-surface-muted"
        leftIcon={<GoogleLogo />}
        loading={pendiente === 'google'}
        disabled={pendiente !== null}
        onClick={() => entrar('google')}
      >
        Google
      </Button>
      <Button
        variant="outline"
        width="full"
        className="border border-border text-ink hover:bg-surface-muted"
        leftIcon={<FacebookLogo />}
        loading={pendiente === 'facebook'}
        disabled={pendiente !== null}
        onClick={() => entrar('facebook')}
      >
        Facebook
      </Button>
    </div>
  );
}
