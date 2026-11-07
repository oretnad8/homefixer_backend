'use client';

import { PerfilView } from '@/components/shared/PerfilView';
import { usePageTitle } from '@/hooks/usePageTitle';

export default function PerfilClientePage() {
  usePageTitle('Mi Perfil');
  return <PerfilView />;
}
