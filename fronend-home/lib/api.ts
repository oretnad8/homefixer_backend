import axios from 'axios';
import { TOKEN_KEY, clearSession } from './session';

// ============================================================
// CLIENTE AXIOS CON INTERCEPTORES
// ============================================================

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Solución para que Axios no elimine el path '/api' cuando se usa url que empieza con '/'
api.interceptors.request.use((config) => {
  if (config.url?.startsWith('/')) {
    config.url = config.url.substring(1);
  }
  if (!config.baseURL?.endsWith('/')) {
    config.baseURL += '/';
  }
  return config;
});

// Interceptor de solicitud — añade el token JWT de la sesión si existe.
// No pisa un Authorization explícito: el login envía a /usuarios/sync el token recién
// emitido por Supabase, y aquí podría quedar el de una sesión anterior (otra cuenta).
api.interceptors.request.use(
  (config) => {
    if (typeof window !== 'undefined' && !config.headers.Authorization) {
      const token = localStorage.getItem(TOKEN_KEY);
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Interceptor de respuesta — maneja 401 y muestra errores en tiempo real
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // MUESTRA EL ERROR EXACTO DEL BACKEND EN LA CONSOLA DEL NAVEGADOR
    if (error.response) {
      console.error(
        "🔥 [API ERROR] %s %s devolvió %d:\n",
        error.config.method?.toUpperCase(),
        error.config.url,
        error.response.status,
        error.response.data
      );
    } else {
      console.error("🔥 [API ERROR] Red o Servidor caído:", error.message);
    }

    if (error.response?.status === 401) {
      if (typeof window !== 'undefined') {
        clearSession();
        const esTecnico = window.location.pathname.startsWith('/tecnico');
        window.location.href = esTecnico ? '/login-tecnico' : '/login-cliente';
      }
    }
    return Promise.reject(error);
  }
);

export default api;

/** true mientras se usan los datos de lib/mock-data.ts (NEXT_PUBLIC_USE_MOCKS=false para usar el backend) */
export const USE_MOCKS = process.env.NEXT_PUBLIC_USE_MOCKS !== 'false';
