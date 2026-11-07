# HomeFixer — Frontend

Marketplace web de servicios técnicos a domicilio. Next.js 16 (App Router) · TypeScript estricto · Tailwind v4 (tokens en `tailwind.config.ts`, cargado con `@config` en `app/globals.css`) · React Hook Form + Zod · Zustand · Framer Motion · lucide-react.

```bash
npm install
npm run dev          # http://localhost:3000 → redirige a /login-cliente
```

Con los mocks activos, cualquier correo válido y una contraseña de 6 o más caracteres inician sesión.

## Variables de entorno

| Variable | Default | Uso |
|---|---|---|
| `NEXT_PUBLIC_API_URL` | `http://localhost:4000/api` | Base del cliente axios (`lib/api.ts`) |
| `NEXT_PUBLIC_USE_MOCKS` | `true` | En `false`, los hooks llaman al backend real |

## Rutas

Las carpetas `(cliente)` y `(tecnico)` son route groups. Cada rol cuelga además de su propio segmento (`/cliente/...`, `/tecnico/...`): sin él, `(cliente)/home` y `(tecnico)/home` resolverían ambas a `/home` y Next.js daría error.

```
app/
├── (auth)/login-cliente, login-tecnico
├── (cliente)/cliente/
│   ├── layout.tsx                  AppShell rol="cliente"
│   ├── home, nueva-solicitud, tecnicos-disponibles
│   ├── servicio-en-progreso/[id], chat/[id], pago/[id], valoracion/[id]
│   └── mis-servicios, historial, pagos, perfil, configuracion
└── (tecnico)/tecnico/
    ├── layout.tsx                  AppShell rol="tecnico"
    ├── home, solicitudes
    ├── servicio-en-progreso/[id], servicio-completado/[id], valoracion/[id]
    └── mis-trabajos, estadisticas, perfil, configuracion
```

## Protección por rol

1. **`proxy.ts`** (en Next 16 reemplaza a `middleware.ts`): lee la cookie `hf_role` y redirige antes de renderizar.
2. **`RoleGuard`** (dentro de `AppShell`): espera a que Zustand rehidrate la sesión y comprueba `user.rol`.

Mientras se usan mocks, el frontend escribe la cookie `hf_role` (`lib/session.ts`). Con el backend real, lo ideal es que el servicio de auth emita una cookie httpOnly y que `proxy.ts` la lea.

## Layout responsive

| | < 1024px | ≥ 1024px (`lg`) |
|---|---|---|
| Navegación | Drawer deslizante (`MobileDrawer`) desde la hamburguesa | Sidebar fijo de 256px (`Sidebar`) |
| Topbar | Barra azul fija | Barra blanca integrada en el contenido |
| Contenido | Ancho completo, `px-4` | `max-w-[1200px]` centrado, `px-8` |

`Sidebar` y `MobileDrawer` renderizan el mismo `NavContent`; los items salen de `lib/navigation.ts`. `Modal` se muestra como bottom sheet en mobile y como modal centrado desde `md`.

## Conexión con el backend

Cada hook en `hooks/` tiene una rama mock y una rama axios. Los endpoints esperados están documentados al principio de cada archivo:

- `useAuth.ts` — `/auth/login`, `/auth/register`, `/auth/oauth/:provider`
- `useSolicitudes.ts` — `/solicitudes/*`
- `useTecnicos.ts` — `/tecnicos/*`
- `useChat.ts` — `/chat/:id/mensajes` (aquí va la suscripción WebSocket/SSE)
- `usePagos.ts` — `/pagos/*`
- `useValoraciones.ts` — `/valoraciones`

Todos se apoyan en `hooks/useQuery.ts` (loading / error / refetch). Para migrar a TanStack Query basta con reimplementar ese archivo. Los mocks guardan su estado en `lib/mock-db.ts`, así que el flujo completo funciona sin backend.

El mapa (`components/shared/MapPlaceholder.tsx`) es simulado. Se sustituye por Google Maps, Mapbox o Leaflet manteniendo la misma API de pins.
