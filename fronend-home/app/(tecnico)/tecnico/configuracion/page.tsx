'use client';

import { ConfiguracionView } from '@/components/shared/ConfiguracionView';
import { usePageTitle } from '@/hooks/usePageTitle';

export default function ConfiguracionTecnicoPage() {
  usePageTitle('Configuración');
  return <ConfiguracionView rol="tecnico" />;
}
