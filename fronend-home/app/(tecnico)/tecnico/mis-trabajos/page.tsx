'use client';

import { Button } from '@/components/ui/Button';
import { SolicitudesList } from '@/components/shared/SolicitudesList';
import { useMisTrabajos } from '@/hooks/useSolicitudes';
import { usePageTitle } from '@/hooks/usePageTitle';

export default function MisTrabajosPage() {
  usePageTitle('Mis Trabajos');
  const query = useMisTrabajos();

  return (
    <SolicitudesList
      query={query}
      hrefDe={(s) =>
        s.estado === 'completada' || s.estado === 'pagada' ? `/tecnico/servicio-completado/${s.id}` : `/tecnico/servicio-en-progreso/${s.id}`
      }
      empty={{
        title: 'Aún no tienes trabajos',
        description: 'Acepta una solicitud para comenzar.',
        action: (
          <Button href="/tecnico/solicitudes" size="sm" width="auto">
            Ver solicitudes
          </Button>
        ),
      }}
    />
  );
}
