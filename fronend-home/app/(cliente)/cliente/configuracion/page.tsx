'use client';

import { ConfiguracionView } from '@/components/shared/ConfiguracionView';
import { usePageTitle } from '@/hooks/usePageTitle';

export default function ConfiguracionClientePage() {
  usePageTitle('Configuración');
  return <ConfiguracionView rol="cliente" />;
}
