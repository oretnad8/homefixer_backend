'use client';

import { PerfilView } from '@/components/shared/PerfilView';
import { usePageTitle } from '@/hooks/usePageTitle';

export default function PerfilTecnicoPage() {
  usePageTitle('Mi Perfil');
  return <PerfilView />;
}
