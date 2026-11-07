'use client';

import { useEffect } from 'react';
import { CreditCard, Hourglass } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';

// ============================================================
// EsperandoPago — la valoración se habilita recién con el pago
// ============================================================
// Flujo de finalización: servicio completado → pago solicitado → pago procesado
// → valoración. El estado PAGADA lo fija ms-solicitudes al recibir el evento
// pago.procesado (asíncrono), por eso se consulta cada pocos segundos.

const CONSULTA_MS = 4000;

interface EsperandoPagoProps {
  vista: 'cliente' | 'tecnico';
  solicitudId: string;
  /** Vuelve a consultar la solicitud */
  onRefrescar: () => void;
  volverHref: string;
}

export function EsperandoPago({ vista, solicitudId, onRefrescar, volverHref }: EsperandoPagoProps) {
  useEffect(() => {
    const intervalo = setInterval(onRefrescar, CONSULTA_MS);
    return () => clearInterval(intervalo);
  }, [onRefrescar]);

  return (
    <Card padding="lg" className="mx-auto flex w-full flex-col items-center text-center md:max-w-md">
      <div className="flex size-14 items-center justify-center rounded-pill bg-warning-light text-warning-dark">
        <Hourglass className="size-7" aria-hidden />
      </div>
      <h2 className="mt-4 text-lg font-semibold text-ink">
        {vista === 'tecnico' ? 'Esperando el pago del cliente' : 'Pago pendiente'}
      </h2>
      <p role="status" className="mt-2 text-sm text-subtle">
        {vista === 'tecnico'
          ? 'Podrás valorar al cliente y cerrar el servicio cuando se reciba el pago. Esta pantalla se actualiza sola.'
          : 'Para valorar al técnico primero debes pagar el servicio. Si ya pagaste, espera unos segundos mientras se confirma.'}
      </p>
      <div className="mt-6 flex w-full flex-col gap-3">
        {vista === 'cliente' && (
          <Button href={`/cliente/pago/${solicitudId}`} variant="success" width="full" leftIcon={<CreditCard className="size-4" />}>
            Pagar servicio
          </Button>
        )}
        <Button href={volverHref} variant="ghost" width="full">
          Volver
        </Button>
      </div>
    </Card>
  );
}
