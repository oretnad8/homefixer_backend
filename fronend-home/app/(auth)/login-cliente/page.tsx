import { Suspense } from 'react';
import type { Metadata } from 'next';
import { AuthLayout } from '@/components/layout/AuthLayout';
import { AuthIllustration } from '@/components/shared/AuthIllustration';
import { LoginForm } from '@/components/shared/LoginForm';

export const metadata: Metadata = { title: 'Iniciar sesión · HomeFixer' };

export default function LoginClientePage() {
  return (
    <AuthLayout
      title="Bienvenido"
      subtitle="Inicia sesión para solicitar un técnico"
      heroTitle="Técnicos de confianza en tu puerta, en minutos."
      heroPoints={['Profesionales verificados y calificados', 'Seguimiento en tiempo real', 'Pago seguro al terminar el trabajo']}
      illustration={<AuthIllustration variante="cliente" />}
    >
      <Suspense>
        <LoginForm rol="cliente" />
      </Suspense>
    </AuthLayout>
  );
}
