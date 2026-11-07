'use client';

import { useCallback } from 'react';
import { useRouter } from 'next/navigation';
import api, { USE_MOCKS } from '@/lib/api';
import { mockCliente, mockTecnico } from '@/lib/mock-data';
import { HOME_PATH, LOGIN_PATH, clearSession, setSession } from '@/lib/session';
import {
  SupabaseAuthError,
  getUser,
  oauthAuthorizeUrl,
  providerHabilitado,
  signInWithPassword,
  signUp,
  type OAuthProvider,
  type SupabaseUser,
} from '@/lib/supabase';
import type { Credenciales, DatosRegistro, User, UserRole } from '@/lib/types';
import { delay } from '@/lib/utils';
import { useAuthStore } from '@/store/authStore';

// ============================================================
// useAuth — login, registro, OAuth y logout
// ============================================================
// Las credenciales las gestiona Supabase Auth (lib/supabase.ts). Tras autenticarse,
// el usuario se sincroniza con ms-usuarios:
//   POST /usuarios/sync      → { id, nombre, email, telefono, esCliente, esTecnico, esAdmin } (lo crea si no existe)
//   Los administradores son los correos de ADMIN_EMAILS en ms-usuarios.

interface AuthResponse {
  user: User;
  token: string;
}

export type { OAuthProvider };

/** Clave de sessionStorage con el rol/next mientras el navegador va y vuelve de Google. */
export const OAUTH_PENDIENTE_KEY = 'hf_oauth_pendiente';

/**
 * Se lanza al entrar por el portal de un rol que el usuario no tiene. Los roles son
 * excluyentes: el cambio cliente ⇄ técnico se hace desde Mi Perfil (CambioRolCard).
 * `rolActual` es el rol que sí tiene la cuenta (null si no tiene ninguno).
 */
export class RolNoAsignadoError extends Error {
  constructor(
    readonly rol: UserRole,
    readonly rolActual: UserRole | null
  ) {
    super(`La cuenta no tiene el rol ${rol} (rol actual: ${rolActual ?? 'ninguno'})`);
    this.name = 'RolNoAsignadoError';
  }
}

/** Resultado del registro: sesión iniciada, o pendiente de confirmar el correo. */
export type ResultadoRegistro = 'sesion' | 'confirmar-correo';

interface UsuarioSincronizado {
  id: number;
  nombre: string;
  email: string;
  telefono: string;
  esCliente: boolean;
  esTecnico: boolean;
  esAdmin?: boolean;
}

const TIENE_ROL: Record<UserRole, (u: UsuarioSincronizado) => boolean> = {
  cliente: (u) => u.esCliente,
  tecnico: (u) => u.esTecnico,
  admin: (u) => u.esAdmin === true,
};

const authHeader = (token: string) => ({ headers: { Authorization: `Bearer ${token}` } });

/**
 * Vincula el usuario de Supabase con ms-usuarios y arma el User de la sesión.
 * Lanza RolNoAsignadoError si entra por el portal de un rol que no tiene.
 */
async function construirSesion(sbUser: SupabaseUser, token: string, rol: UserRole): Promise<AuthResponse> {
  const meta = sbUser.user_metadata ?? {};
  // Busca el usuario por email en ms-usuarios (o lo crea) y devuelve su id propio
  const { data } = await api.post<UsuarioSincronizado>('/usuarios/sync', null, authHeader(token));

  if (!TIENE_ROL[rol](data)) {
    const rolActual = (['admin', 'tecnico', 'cliente'] as const).find((r) => TIENE_ROL[r](data)) ?? null;
    throw new RolNoAsignadoError(rol, rolActual);
  }

  return {
    token,
    user: {
      id: String(data.id),
      nombre: data.nombre,
      apellido: '',
      email: sbUser.email,
      telefono: data.telefono,
      avatarUrl: meta.avatar_url,
      rol,
      creadoEn: new Date().toISOString(),
    },
  };
}

async function mockAuth(rol: UserRole, email?: string, nombre?: string): Promise<AuthResponse> {
  await delay(700);
  const base = rol === 'cliente' ? mockCliente : mockTecnico;
  return {
    user: { ...base, email: email ?? base.email, nombre: nombre ?? base.nombre },
    token: `mock-token-${rol}-${Date.now()}`,
  };
}

export function useAuth() {
  const router = useRouter();
  const user = useAuthStore((s) => s.user);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const storeLogin = useAuthStore((s) => s.login);
  const storeLogout = useAuthStore((s) => s.logout);

  /** Guarda la sesión y navega al home del rol (o a ?next= si viene del proxy) */
  const finalizar = useCallback(
    ({ user, token }: AuthResponse, next?: string | null) => {
      storeLogin(user, token);
      setSession(user.rol, token);
      const destino = next?.startsWith(`/${user.rol}/`) ? next : HOME_PATH[user.rol];
      router.replace(destino);
    },
    [router, storeLogin]
  );

  const login = useCallback(
    async (creds: Credenciales, rol: UserRole, next?: string | null) => {
      if (USE_MOCKS) {
        finalizar(await mockAuth(rol, creds.email), next);
        return;
      }
      const sesion = await signInWithPassword(creds.email, creds.password);
      finalizar(await construirSesion(sesion.user, sesion.access_token, rol), next);
    },
    [finalizar]
  );

  /** Crea la cuenta en Supabase; el usuario local se crea en su primer inicio de sesión. */
  const register = useCallback(
    async (datos: DatosRegistro, rol: UserRole): Promise<ResultadoRegistro> => {
      if (USE_MOCKS) {
        finalizar(await mockAuth(rol, datos.email, datos.nombre));
        return 'sesion';
      }
      const metadata: Record<string, string> = { nombre: datos.nombre, apellido: datos.apellido, rol_solicitado: rol };
      const res = await signUp(datos.email, datos.password, metadata, `${window.location.origin}${LOGIN_PATH[rol]}`);

      // Proyecto con confirmación de correo activada: no hay sesión hasta confirmar
      if (!res.access_token || !res.user) return 'confirmar-correo';

      finalizar(await construirSesion(res.user, res.access_token, rol));
      return 'sesion';
    },
    [finalizar]
  );

  /** Redirige a Google/Facebook vía Supabase; la vuelta la procesa /auth/callback. */
  const loginWithProvider = useCallback(
    async (provider: OAuthProvider, rol: UserRole, next?: string | null) => {
      if (USE_MOCKS) {
        finalizar(await mockAuth(rol));
        return;
      }
      if (!(await providerHabilitado(provider))) {
        throw new SupabaseAuthError(`El inicio de sesión con ${provider} no está habilitado en Supabase.`, 'provider_disabled');
      }
      sessionStorage.setItem(OAUTH_PENDIENTE_KEY, JSON.stringify({ rol, next: next ?? null }));
      window.location.assign(oauthAuthorizeUrl(provider, `${window.location.origin}/auth/callback`));
    },
    [finalizar]
  );

  /** Completa el login OAuth con el access_token que Supabase devuelve en /auth/callback. */
  const completarOAuth = useCallback(
    async (accessToken: string, rol: UserRole, next?: string | null) => {
      const sbUser = await getUser(accessToken);
      finalizar(await construirSesion(sbUser, accessToken, rol), next);
    },
    [finalizar]
  );

  /** Cierra sesión y va al login del rol actual, o al de `rolDestino` si se indica. */
  const logout = useCallback(
    (rolDestino?: UserRole) => {
      const rol = rolDestino ?? user?.rol ?? 'cliente';
      storeLogout();
      clearSession();
      router.replace(LOGIN_PATH[rol]);
    },
    [router, storeLogout, user?.rol]
  );

  return { user, isAuthenticated, login, register, loginWithProvider, completarOAuth, logout };
}
