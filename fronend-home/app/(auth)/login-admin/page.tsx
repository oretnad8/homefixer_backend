import { Suspense } from 'react';
import type { Metadata } from 'next';
import { ShieldCheck } from 'lucide-react';
import { AuthLayout } from '@/components/layout/AuthLayout';
import { LoginForm } from '@/components/shared/LoginForm';

export const metadata: Metadata = { title: 'Administración · HomeFixer' };

export default function LoginAdminPage() {
  return (
    <AuthLayout
      title="Administración"
      subtitle="Acceso solo para administradores de la plataforma"
      heroTitle="Valida a los técnicos que hacen crecer HomeFixer."
      heroPoints={['Revisa certificaciones pendientes', 'Otorga insignias de maestría', 'Notifica cada decisión al técnico']}
      illustration={
        <div className="flex size-24 items-center justify-center rounded-card bg-white/15 text-white">
          <ShieldCheck className="size-12" aria-hidden />
        </div>
      }
    >
      <Suspense>
        <LoginForm rol="admin" />
      </Suspense>
    </AuthLayout>
  );
}
