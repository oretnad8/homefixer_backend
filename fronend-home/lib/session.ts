import type { UserRole } from './types';

// ============================================================
// COOKIE DE SESIÓN (lectura en proxy.ts para proteger rutas)
// ============================================================
// Mientras se usan mocks, el frontend escribe una cookie con el rol para que
// el proxy pueda redirigir antes de renderizar. Cuando el servicio de auth
// devuelva su propia cookie httpOnly, basta con leerla en proxy.ts y eliminar
// setSessionCookie/clearSessionCookie.

export const ROLE_COOKIE = 'hf_role';
export const TOKEN_KEY = 'hf_token';

export const LOGIN_PATH: Record<UserRole, string> = {
  cliente: '/login-cliente',
  tecnico: '/login-tecnico',
  admin: '/login-admin',
};

export const HOME_PATH: Record<UserRole, string> = {
  cliente: '/cliente/home',
  tecnico: '/tecnico/home',
  admin: '/admin/certificaciones',
};

export function setSession(rol: UserRole, token: string) {
  localStorage.setItem(TOKEN_KEY, token);
  document.cookie = `${ROLE_COOKIE}=${rol}; path=/; max-age=${60 * 60 * 24 * 7}; samesite=lax`;
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  document.cookie = `${ROLE_COOKIE}=; path=/; max-age=0; samesite=lax`;
}
