'use client';

import { useParams } from 'next/navigation';
import { ErrorState, LoadingRegion, Skeleton } from '@/components/ui/States';
import { EsperandoPago } from '@/components/shared/EsperandoPago';
import { ValoracionForm } from '@/components/shared/ValoracionForm';
import { useSolicitud } from '@/hooks/useSolicitudes';
import { usePageTitle } from '@/hooks/usePageTitle';

// Valoración (Cliente → Técnico). Mobile apilado; desktop tarjeta max-w-md centrada.
export default function ValoracionClientePage() {
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
    return <EsperandoPago vista="cliente" solicitudId={s.id} onRefrescar={refetch} volverHref={`/cliente/servicio-en-progreso/${s.id}`} />;
  }

  const t = s.tecnico;
  return (
    <ValoracionForm
      solicitudId={s.id}
      persona={{ nombre: t?.nombre ?? 'Tu técnico', apellido: t?.apellido, subtitulo: t?.especialidad ?? '' }}
      pregunta="¿Cómo fue tu experiencia?"
      tags={['Puntual', 'Profesional', 'Limpio', 'Buen precio', 'Amable']}
      volverHref="/cliente/home"
    />
  );
}
