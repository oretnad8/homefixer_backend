'use client';

import { useEffect } from 'react';
import { useParams } from 'next/navigation';
import { CircleCheck, Hourglass, Star } from 'lucide-react';
import { Avatar } from '@/components/ui/Avatar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorState, LoadingRegion, Skeleton } from '@/components/ui/States';
import { HitosTimeline } from '@/components/shared/HitosTimeline';
import { PhotoUploader } from '@/components/shared/PhotoUploader';
import { useHitos, useSolicitud } from '@/hooks/useSolicitudes';
import { usePageTitle } from '@/hooks/usePageTitle';
import { COMISION_SERVICIO } from '@/hooks/usePagos';
import { CATEGORIA_LABELS, formatCurrency, formatDate } from '@/lib/utils';
import { FOTOS_VACIAS, useServicioStore } from '@/store/servicioStore';

// ============================================================
// Servicio Completado (Técnico)
// ============================================================
// Mobile: resumen apilado. Desktop: tarjeta centrada con resumen a la
// izquierda y fotos antes/después a la derecha.
// Hasta que el cliente paga (PAGADA) no se puede valorar ni cerrar el servicio.

/** Consulta del estado mientras se espera el pago (lo cambia el evento pago.procesado) */
const CONSULTA_PAGO_MS = 5000;

export default function ServicioCompletadoPage() {
  usePageTitle('Servicio Completado');
  const { id } = useParams<{ id: string }>();
  const { data: s, isLoading, error, refetch } = useSolicitud(id);
  const hitos = useHitos(id);
  const fotos = useServicioStore((st) => st.fotos[id] ?? FOTOS_VACIAS);
  const notas = useServicioStore((st) => st.notas[id] ?? '');
  const esperandoPago = s?.estado === 'completada';

  const refetchHitos = hitos.refetch;
  useEffect(() => {
    if (!esperandoPago) return;
    const intervalo = setInterval(() => {
      refetch();
      refetchHitos();
    }, CONSULTA_PAGO_MS);
    return () => clearInterval(intervalo);
  }, [esperandoPago, refetch, refetchHitos]);

  if (isLoading && !s) {
    return (
      <LoadingRegion className="mx-auto max-w-4xl">
        <Skeleton className="h-[32rem] rounded-card" />
      </LoadingRegion>
    );
  }
  if (error || !s) return <ErrorState message={error?.message} onRetry={refetch} />;

  const [nombre, apellido] = (s.clienteNombre ?? 'Cliente').split(' ');
  const monto = s.presupuestoEstimado ?? 0;
  const neto = Math.round(monto * (1 - COMISION_SERVICIO));

  return (
    <Card padding="none" className="mx-auto max-w-4xl overflow-hidden">
      {esperandoPago ? (
        <div className="flex flex-col items-center bg-warning-light px-6 py-8 text-center">
          <Hourglass className="size-14 text-warning-dark" aria-hidden />
          <h2 className="mt-3 text-2xl font-bold text-ink">Trabajo terminado · esperando el pago</h2>
          <p role="status" className="mt-1 text-sm text-warning-dark">
            Se le solicitó el pago al cliente. Podrás valorarlo y cerrar el servicio cuando se reciba.
          </p>
        </div>
      ) : (
        <div className="flex flex-col items-center bg-success-light px-6 py-8 text-center">
          <CircleCheck className="size-14 text-success" aria-hidden />
          <h2 className="mt-3 text-2xl font-bold text-ink">¡Pago recibido!</h2>
          <p role="status" className="mt-1 text-sm text-success-dark">
            El cliente pagó el servicio. Valóralo para finalizar la solicitud.
          </p>
        </div>
      )}

      <div className="grid gap-6 p-5 md:p-8 lg:grid-cols-2 lg:gap-10">
        <section aria-labelledby="titulo-resumen">
          <h3 id="titulo-resumen" className="mb-4 font-semibold text-ink">
            Resumen
          </h3>
          <div className="flex items-center gap-3">
            <Avatar nombre={nombre} apellido={apellido} />
            <div>
              <p className="font-semibold text-ink">{s.clienteNombre}</p>
              <p className="text-sm text-subtle">{s.direccion}</p>
            </div>
          </div>
          <dl className="mt-5 space-y-3 text-sm">
            <div className="flex justify-between gap-4">
              <dt className="text-subtle">Servicio</dt>
              <dd className="text-right text-ink">{CATEGORIA_LABELS[s.categoria]}</dd>
            </div>
            <div className="flex justify-between gap-4">
              <dt className="text-subtle">Finalizado</dt>
              <dd className="text-right text-ink">{formatDate(s.actualizadoEn)}</dd>
            </div>
            <div className="flex justify-between gap-4">
              <dt className="text-subtle">Monto cobrado</dt>
              <dd className="text-right text-ink">{formatCurrency(monto)}</dd>
            </div>
            <div className="flex justify-between gap-4 border-t border-border pt-3">
              <dt className="font-semibold text-ink">Tu ganancia</dt>
              <dd className="text-lg font-bold text-success">{formatCurrency(neto)}</dd>
            </div>
          </dl>
          <div className="mt-6">
            <h3 className="mb-3 font-semibold text-ink">Línea de tiempo</h3>
            <HitosTimeline hitos={hitos.data ?? []} />
          </div>
          {notas && (
            <div className="mt-5 rounded-input bg-surface-muted p-4 text-sm">
              <p className="mb-1 font-medium text-ink">Notas</p>
              <p className="whitespace-pre-wrap text-subtle">{notas}</p>
            </div>
          )}
        </section>

        <section aria-label="Fotos del trabajo" className="space-y-5">
          <PhotoUploader label="Antes" fotos={fotos.antes} />
          <PhotoUploader label="Después" fotos={fotos.despues} />
        </section>
      </div>

      <div className="flex flex-col-reverse gap-3 border-t border-border p-5 md:flex-row md:justify-end md:px-8">
        <Button href="/tecnico/home" variant="ghost">
          Volver al inicio
        </Button>
        {esperandoPago ? (
          <Button disabled leftIcon={<Hourglass className="size-4" />}>
            Esperando el pago
          </Button>
        ) : (
          <Button href={`/tecnico/valoracion/${s.id}`} leftIcon={<Star className="size-4" />}>
            Valorar al cliente
          </Button>
        )}
      </div>
    </Card>
  );
}
