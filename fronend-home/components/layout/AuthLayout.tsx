import type { ReactNode } from 'react';
import { CircleCheck } from 'lucide-react';
import { Logo } from '@/components/shared/Logo';

// ============================================================
// AuthLayout
// ============================================================
// Mobile: degradado de marca arriba (logo + subtítulo) y la card del
//         formulario superpuesta debajo, como en el mockup.
// Desktop (lg): split-screen. Panel izquierdo con degradado + logo +
//         ilustración; panel derecho con el formulario centrado.

interface AuthLayoutProps {
  title: string;
  subtitle: string;
  /** Texto del panel de marca (desktop) */
  heroTitle: string;
  heroPoints: string[];
  illustration: ReactNode;
  children: ReactNode;
}

export function AuthLayout({ title, subtitle, heroTitle, heroPoints, illustration, children }: AuthLayoutProps) {
  return (
    <div className="min-h-dvh bg-surface-muted lg:grid lg:grid-cols-2 lg:bg-surface">
      {/* Panel de marca */}
      <section className="relative overflow-hidden bg-gradient-brand px-6 pb-24 pt-12 text-white lg:flex lg:flex-col lg:justify-between lg:p-12 xl:p-16">
        <Decoracion />
        <div className="relative flex flex-col items-center text-center lg:items-start lg:text-left">
          <Logo tone="light" size="lg" />
          <p className="mt-3 text-sm text-primary-100 lg:hidden">Servicios técnicos a domicilio</p>
        </div>

        <div className="relative hidden lg:block">
          <div className="mb-10 flex justify-center">{illustration}</div>
          <h2 className="max-w-md text-3xl font-bold leading-tight xl:text-4xl">{heroTitle}</h2>
          <ul className="mt-6 space-y-3">
            {heroPoints.map((p) => (
              <li key={p} className="flex items-center gap-3 text-primary-50">
                <CircleCheck className="size-5 shrink-0 text-white" aria-hidden />
                {p}
              </li>
            ))}
          </ul>
        </div>

        <p className="relative hidden text-sm text-primary-100 lg:block">
          © {new Date().getFullYear()} HomeFixer. Servicios técnicos a domicilio.
        </p>
      </section>

      {/* Formulario */}
      <section className="relative -mt-16 px-4 pb-10 lg:mt-0 lg:flex lg:items-center lg:justify-center lg:px-12 lg:py-12">
        <div className="mx-auto w-full max-w-md rounded-card bg-surface p-6 shadow-card-lg md:p-8 lg:p-0 lg:shadow-none">
          <header className="mb-6">
            <h1 className="text-2xl font-bold text-ink lg:text-3xl">{title}</h1>
            <p className="mt-1 text-sm text-subtle">{subtitle}</p>
          </header>
          {children}
        </div>
      </section>
    </div>
  );
}

function Decoracion() {
  return (
    <div aria-hidden className="pointer-events-none absolute inset-0">
      <div className="absolute -right-16 -top-16 size-64 rounded-pill bg-white/10" />
      <div className="absolute -bottom-24 -left-10 size-72 rounded-pill bg-white/5" />
    </div>
  );
}
