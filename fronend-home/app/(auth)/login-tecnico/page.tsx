import { Suspense } from 'react';
import type { Metadata } from 'next';
import { AuthLayout } from '@/components/layout/AuthLayout';
import { AuthIllustration } from '@/components/shared/AuthIllustration';
import { LoginForm } from '@/components/shared/LoginForm';

export const metadata: Metadata = { title: 'Acceso técnicos · HomeFixer' };

export default function LoginTecnicoPage() {
  return (
    <AuthLayout
      title="Portal del Técnico"
      subtitle="Inicia sesión para recibir solicitudes"
      heroTitle="Más clientes, menos tiempo buscando trabajo."
      heroPoints={['Solicitudes cerca de tu zona', 'Cobros seguros y puntuales', 'Construye tu reputación con valoraciones']}
      illustration={<AuthIllustration variante="tecnico" />}
    >
      <Suspense>
        <LoginForm rol="tecnico" />
      </Suspense>
    </AuthLayout>
  );
}
