'use client';

import { useParams } from 'next/navigation';
import { ErrorState, LoadingRegion, Skeleton } from '@/components/ui/States';
import { EsperandoPago } from '@/components/shared/EsperandoPago';
import { ValoracionForm } from '@/components/shared/ValoracionForm';
import { useSolicitud } from '@/hooks/useSolicitudes';
import { usePageTitle } from '@/hooks/usePageTitle';
import { CATEGORIA_LABELS } from '@/lib/utils';

// Valoración (Técnico → Cliente). Mobile apilado; desktop tarjeta max-w-md centrada.
export default function ValoracionTecnicoPage() {
  usePageTitle('Valoración');
  const { id } = useParams<{ id: string }>();
  const { data: s, isLoading, error, refetch } = useSolicitud(id);

  if (isLoading && !s) {
    return (
      <LoadingRegion className="mx-auto w-full md:max-w-md">
        <Skeleton className="h-[34rem] rounded-card" />
      </LoadingRegion>
    );
  }
  if (error || !s) return <ErrorState message={error?.message} onRetry={refetch} />;
  if (s.estado !== 'pagada') {
    return <EsperandoPago vista="tecnico" solicitudId={s.id} onRefrescar={refetch} volverHref={`/tecnico/servicio-completado/${s.id}`} />;
  }

  const [nombre, apellido] = (s.clienteNombre ?? 'Cliente').split(' ');
  return (
    <ValoracionForm
      solicitudId={s.id}
      persona={{ nombre, apellido, subtitulo: `Cliente · ${CATEGORIA_LABELS[s.categoria]}` }}
      pregunta="¿Cómo fue trabajar con este cliente?"
      tags={['Amable', 'Puntual', 'Buena comunicación', 'Pago rápido']}
      volverHref="/tecnico/home"
    />
  );
}
