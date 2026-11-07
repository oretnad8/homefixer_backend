'use client';

import { useState, type ReactNode } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Banknote, CircleCheck, CreditCard, Landmark, Lock, ShieldCheck, Wallet } from 'lucide-react';
import { Avatar } from '@/components/ui/Avatar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { ErrorState, LoadingRegion, Skeleton } from '@/components/ui/States';
import { COMISION_SERVICIO, procesarPago, usePagoDeSolicitud } from '@/hooks/usePagos';
import { useSolicitud } from '@/hooks/useSolicitudes';
import { usePageTitle } from '@/hooks/usePageTitle';
import type { MetodoPago } from '@/lib/types';
import { CATEGORIA_LABELS, cn, formatCurrency } from '@/lib/utils';

// ============================================================
// Pago del Servicio
// ============================================================
// Mobile: resumen arriba y métodos de pago apilados debajo.
// Desktop: tarjeta centrada con dos columnas — resumen a la izquierda,
//          método de pago + "Procesar Pago" a la derecha.

const METODOS: { id: MetodoPago; label: string; detalle: string; icon: ReactNode }[] = [
  { id: 'tarjeta', label: 'Tarjeta', detalle: 'Crédito o débito', icon: <CreditCard /> },
  { id: 'efectivo', label: 'Efectivo', detalle: 'Paga al técnico', icon: <Banknote /> },
  { id: 'transferencia', label: 'Transferencia', detalle: 'SPEI', icon: <Landmark /> },
  { id: 'paypal', label: 'PayPal', detalle: 'Cuenta PayPal', icon: <Wallet /> },
];

const tarjetaSchema = z.object({
  titular: z.string().trim().min(3, 'Nombre del titular'),
  numero: z.string().regex(/^(\d{4} ?){4}$/, 'Número de 16 dígitos'),
  vencimiento: z.string().regex(/^(0[1-9]|1[0-2])\/\d{2}$/, 'Formato MM/AA'),
  cvv: z.string().regex(/^\d{3,4}$/, '3 o 4 dígitos'),
});
type TarjetaValues = z.infer<typeof tarjetaSchema>;

export default function PagoPage() {
  usePageTitle('Pago del Servicio');
  const router = useRouter();
  const { id } = useParams<{ id: string }>();
  const solicitud = useSolicitud(id);
  const pago = usePagoDeSolicitud(id);
  const [metodo, setMetodo] = useState<MetodoPago>('tarjeta');
  const [procesando, setProcesando] = useState(false);
  const [errorPago, setErrorPago] = useState<string | null>(null);
  const [exito, setExito] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<TarjetaValues>({ resolver: zodResolver(tarjetaSchema) });

  if (solicitud.isLoading || pago.isLoading) {
    return (
      <LoadingRegion className="mx-auto grid max-w-5xl gap-4 lg:grid-cols-2 lg:gap-8">
        <Skeleton className="h-80 rounded-card" />
        <Skeleton className="h-96 rounded-card" />
      </LoadingRegion>
    );
  }
  const error = solicitud.error ?? pago.error;
  if (error || !solicitud.data || !pago.data) {
    return <ErrorState message={error?.message} onRetry={() => (solicitud.refetch(), pago.refetch())} />;
  }

  const s = solicitud.data;
  const t = s.tecnico;
  const subtotal = pago.data.monto;
  const comision = Math.round(subtotal * COMISION_SERVICIO);
  const total = subtotal + comision;

  const cobrar = async () => {
    setErrorPago(null);
    setProcesando(true);
    try {
      await procesarPago(pago.data!.id, metodo);
      setExito(true);
    } catch {
      setErrorPago('El pago fue rechazado. Verifica los datos o elige otro método.');
    } finally {
      setProcesando(false);
    }
  };

  const onSubmit = metodo === 'tarjeta' ? handleSubmit(cobrar) : (e: React.FormEvent) => (e.preventDefault(), void cobrar());

  return (
    <div className="mx-auto max-w-5xl">
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2 lg:gap-0 lg:overflow-hidden lg:rounded-card lg:border lg:border-border lg:bg-surface lg:shadow-card-md">
        {/* Resumen */}
        <Card className="lg:rounded-none lg:border-0 lg:border-r lg:bg-surface-muted lg:p-8 lg:shadow-none">
          <h2 className="text-lg font-semibold text-ink">Resumen del servicio</h2>
          {t && (
            <div className="mt-4 flex items-center gap-3">
              <Avatar nombre={t.nombre} apellido={t.apellido} />
              <div>
                <p className="font-semibold text-ink">
                  {t.nombre} {t.apellido}
                </p>
                <p className="text-sm text-subtle">{t.especialidad}</p>
              </div>
            </div>
          )}
          <div className="mt-5 rounded-input bg-surface p-4 text-sm lg:border lg:border-border">
            <p className="font-medium text-ink">{CATEGORIA_LABELS[s.categoria]}</p>
            <p className="mt-1 text-subtle">{pago.data.descripcion}</p>
          </div>
          <dl className="mt-5 space-y-3 text-sm">
            <div className="flex justify-between">
              <dt className="text-subtle">Mano de obra y material</dt>
              <dd className="text-ink">{formatCurrency(subtotal)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-subtle">Tarifa de servicio ({COMISION_SERVICIO * 100}%)</dt>
              <dd className="text-ink">{formatCurrency(comision)}</dd>
            </div>
            <div className="flex justify-between border-t border-border pt-3 text-base">
              <dt className="font-semibold text-ink">Total</dt>
              <dd className="text-xl font-bold text-primary-600">{formatCurrency(total)}</dd>
            </div>
          </dl>
          <p className="mt-6 flex items-center gap-2 text-xs text-subtle">
            <ShieldCheck className="size-4 text-success" aria-hidden />
            Pago protegido: el técnico recibe el dinero cuando confirmas el trabajo.
          </p>
        </Card>

        {/* Método de pago */}
        <Card className="lg:rounded-none lg:border-0 lg:p-8 lg:shadow-none">
          <form onSubmit={onSubmit} noValidate>
            <fieldset>
              <legend className="text-lg font-semibold text-ink">Método de pago</legend>
              <div className="mt-4 grid grid-cols-2 gap-3">
                {METODOS.map((m) => {
                  const on = metodo === m.id;
                  return (
                    <label
                      key={m.id}
                      className={cn(
                        'relative flex cursor-pointer flex-col gap-1 rounded-input border-2 p-3 transition-colors focus-within:outline-2 focus-within:outline-primary-500',
                        on ? 'border-primary-500 bg-primary-50' : 'border-border hover:border-primary-200'
                      )}
                    >
                      <input type="radio" name="metodo" value={m.id} checked={on} onChange={() => setMetodo(m.id)} className="sr-only" />
                      <span className={cn('[&>svg]:size-6', on ? 'text-primary-600' : 'text-subtle')} aria-hidden>
                        {m.icon}
                      </span>
                      <span className="text-sm font-semibold text-ink">{m.label}</span>
                      <span className="text-xs text-subtle">{m.detalle}</span>
                      {on && <CircleCheck className="absolute right-2 top-2 size-5 text-primary-500" aria-hidden />}
                    </label>
                  );
                })}
              </div>
            </fieldset>

            {metodo === 'tarjeta' && (
              <div className="mt-5 space-y-4">
                <Input label="Titular de la tarjeta" autoComplete="cc-name" error={errors.titular?.message} {...register('titular')} />
                <Input
                  label="Número de tarjeta"
                  inputMode="numeric"
                  autoComplete="cc-number"
                  maxLength={19}
                  error={errors.numero?.message}
                  {...register('numero')}
                />
                <div className="grid grid-cols-2 gap-3">
                  <Input label="Vencimiento (MM/AA)" autoComplete="cc-exp" maxLength={5} error={errors.vencimiento?.message} {...register('vencimiento')} />
                  <Input label="CVV" type="password" inputMode="numeric" autoComplete="cc-csc" maxLength={4} error={errors.cvv?.message} {...register('cvv')} />
                </div>
              </div>
            )}
            {metodo === 'efectivo' && (
              <p className="mt-5 rounded-input bg-warning-light px-4 py-3 text-sm text-warning-dark">
                Entrega {formatCurrency(total)} al técnico al terminar. Confirmaremos el cobro cuando él lo registre.
              </p>
            )}
            {(metodo === 'transferencia' || metodo === 'paypal') && (
              <p className="mt-5 rounded-input bg-primary-50 px-4 py-3 text-sm text-primary-700">
                Te redirigiremos para completar el pago de forma segura.
              </p>
            )}

            {errorPago && (
              <p role="alert" className="mt-4 rounded-input bg-danger-light px-4 py-3 text-sm font-medium text-danger-dark">
                {errorPago}
              </p>
            )}

            <Button type="submit" variant="success" width="full" size="lg" className="mt-6" loading={procesando} leftIcon={<Lock className="size-4" />}>
              Procesar Pago · {formatCurrency(total)}
            </Button>
          </form>
        </Card>
      </div>

      <Modal
        open={exito}
        onClose={() => router.push(`/cliente/valoracion/${s.id}`)}
        title="¡Pago realizado!"
        size="sm"
        footer={
          <Button href={`/cliente/valoracion/${s.id}`} variant="success" width="full">
            Valorar al técnico
          </Button>
        }
      >
        <div className="flex flex-col items-center py-4 text-center">
          <CircleCheck className="size-16 text-success" aria-hidden />
          <p className="mt-3 text-2xl font-bold text-ink">{formatCurrency(total)}</p>
          <p className="mt-1 text-sm text-subtle">Recibirás el comprobante en tu correo.</p>
        </div>
      </Modal>
    </div>
  );
}
