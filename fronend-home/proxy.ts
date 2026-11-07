import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { HOME_PATH, LOGIN_PATH, ROLE_COOKIE } from '@/lib/session';
import type { UserRole } from '@/lib/types';

// ============================================================
// PROTECCIÓN DE RUTAS POR ROL (Next 16: "proxy" reemplaza a "middleware")
// ============================================================
// /cliente/*  → requiere rol cliente
// /tecnico/*  → requiere rol tecnico
// /admin/*    → requiere rol admin
// /login-*    → si ya hay sesión, envía al home del rol

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const raw = request.cookies.get(ROLE_COOKIE)?.value;
  const rol: UserRole | null = raw === 'cliente' || raw === 'tecnico' || raw === 'admin' ? raw : null;

  const requerido: UserRole | null = pathname.startsWith('/cliente')
    ? 'cliente'
    : pathname.startsWith('/tecnico')
      ? 'tecnico'
      : pathname.startsWith('/admin')
        ? 'admin'
        : null;

  if (requerido && rol !== requerido) {
    const url = new URL(LOGIN_PATH[requerido], request.url);
    url.searchParams.set('next', pathname);
    return NextResponse.redirect(url);
  }

  if (rol && (pathname === '/login-cliente' || pathname === '/login-tecnico' || pathname === '/login-admin')) {
    return NextResponse.redirect(new URL(HOME_PATH[rol], request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: ['/cliente/:path*', '/tecnico/:path*', '/admin/:path*', '/login-cliente', '/login-tecnico', '/login-admin'],
};
