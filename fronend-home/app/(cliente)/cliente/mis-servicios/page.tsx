'use client';

import { Plus } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { SolicitudesList } from '@/components/shared/SolicitudesList';
import { useMisSolicitudes } from '@/hooks/useSolicitudes';
import { usePageTitle } from '@/hooks/usePageTitle';

export default function MisServiciosPage() {
  usePageTitle('Mis Servicios');
  const query = useMisSolicitudes();

  return (
    <div className="space-y-4 md:space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <p className="text-sm text-subtle">Solicitudes activas y pendientes de asignar.</p>
        <Button href="/cliente/nueva-solicitud" leftIcon={<Plus className="size-4" />}>
          Nueva Solicitud
        </Button>
      </div>
      <SolicitudesList
        vista="cliente"
        query={query}
        filtro={(s) => s.estado !== 'pagada' && s.estado !== 'cancelada'}
        hrefDe={(s) =>
          s.estado === 'pendiente'
            ? `/cliente/tecnicos-disponibles?solicitud=${s.id}&categoria=${s.categoria}`
            : `/cliente/servicio-en-progreso/${s.id}`
        }
        empty={{ title: 'No tienes servicios activos', description: 'Cuando solicites un técnico, lo verás aquí.' }}
      />
    </div>
  );
}
