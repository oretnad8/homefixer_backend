'use client';

import { SolicitudesList } from '@/components/shared/SolicitudesList';
import { useMisSolicitudes } from '@/hooks/useSolicitudes';
import { usePageTitle } from '@/hooks/usePageTitle';

export default function HistorialPage() {
  usePageTitle('Historial');
  const query = useMisSolicitudes();

  return (
    <SolicitudesList
      vista="cliente"
      query={query}
      filtro={(s) => s.estado === 'pagada' || s.estado === 'cancelada'}
      hrefDe={(s) => `/cliente/servicio-en-progreso/${s.id}`}
      empty={{ title: 'Tu historial está vacío', description: 'Aquí aparecerán los servicios que ya terminaron.' }}
    />
  );
}
