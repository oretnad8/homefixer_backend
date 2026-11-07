// ============================================================
// Cliente mínimo de Supabase Auth (API REST GoTrue)
// ============================================================
//   POST /auth/v1/signup                    → registro email/contraseña
//   POST /auth/v1/token?grant_type=password → login email/contraseña
//   GET  /auth/v1/authorize?provider=google → OAuth (vuelve a /auth/callback#access_token=…)
//   GET  /auth/v1/user                      → usuario dueño del token

const SUPABASE_URL = process.env.NEXT_PUBLIC_SUPABASE_URL as string;
const SUPABASE_KEY = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY as string;

export type OAuthProvider = 'google' | 'facebook';

export interface SupabaseUser {
  id: string;
  email: string;
  user_metadata?: {
    nombre?: string;
    apellido?: string;
    telefono?: string;
    full_name?: string;
    name?: string;
    avatar_url?: string;
    rol_solicitado?: string;
  };
}

export interface SupabaseSession {
  access_token: string;
  user: SupabaseUser;
}

export class SupabaseAuthError extends Error {
  constructor(
    message: string,
    public code?: string
  ) {
    super(message);
    this.name = 'SupabaseAuthError';
  }
}

async function request<T>(path: string, init: RequestInit = {}, token?: string): Promise<T> {
  const res = await fetch(`${SUPABASE_URL}/auth/v1${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      apikey: SUPABASE_KEY,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init.headers,
    },
  });
  const body = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new SupabaseAuthError(
      body.msg ?? body.error_description ?? body.message ?? 'Error de autenticación',
      body.error_code ?? body.code ?? body.error
    );
  }
  return body as T;
}

export function signInWithPassword(email: string, password: string) {
  return request<SupabaseSession>('/token?grant_type=password', {
    method: 'POST',
    body: JSON.stringify({ email, password }),
  });
}

/**
 * Crea la cuenta en Supabase. Si el proyecto exige confirmar el correo no devuelve
 * sesión (access_token): el usuario debe abrir el enlace del correo y luego iniciar sesión.
 */
export function signUp(email: string, password: string, metadata: Record<string, string>, redirectTo: string) {
  return request<Partial<SupabaseSession> & { id?: string }>(
    `/signup?redirect_to=${encodeURIComponent(redirectTo)}`,
    { method: 'POST', body: JSON.stringify({ email, password, data: metadata }) }
  );
}

export function getUser(accessToken: string) {
  return request<SupabaseUser>('/user', {}, accessToken);
}

/** Verifica que el proveedor esté habilitado en Supabase antes de redirigir. */
export async function providerHabilitado(provider: OAuthProvider): Promise<boolean> {
  const settings = await request<{ external?: Record<string, boolean> }>('/settings');
  return settings.external?.[provider] === true;
}

export function oauthAuthorizeUrl(provider: OAuthProvider, redirectTo: string) {
  return `${SUPABASE_URL}/auth/v1/authorize?provider=${provider}&redirect_to=${encodeURIComponent(redirectTo)}`;
}
