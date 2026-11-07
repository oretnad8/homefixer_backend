'use client';

import { CreditCard } from 'lucide-react';
import { Badge } from '@/components/ui/Badge';
import { Card } from '@/components/ui/Card';
import { EmptyState, ErrorState, LoadingRegion, Skeleton } from '@/components/ui/States';
import { useHistorialPagos } from '@/hooks/usePagos';
import { usePageTitle } from '@/hooks/usePageTitle';
import { formatCurrency, formatDate } from '@/lib/utils';

// Mobile: lista de cards. Desde md: tabla dentro de una card.
export default function PagosPage() {
  usePageTitle('Pagos');
  const { data, isLoading, error, refetch } = useHistorialPagos();

  if (isLoading) {
    return (
      <LoadingRegion className="space-y-3">
        {[0, 1, 2].map((i) => (
          <Skeleton key={i} className="h-20 rounded-card" />
        ))}
      </LoadingRegion>
    );
  }
  if (error) return <ErrorState message={error.message} onRetry={refetch} />;
  if (!data?.length) return <EmptyState icon={<CreditCard />} title="Sin pagos todavía" description="Tus pagos aparecerán aquí." />;

  return (
    <>
      <ul className="space-y-3 md:hidden">
        {data.map((p) => (
          <li key={p.id}>
            <Card padding="sm" className="flex items-center justify-between gap-3">
              <div className="min-w-0">
                <p className="truncate text-sm font-medium text-ink">{p.descripcion}</p>
                <p className="text-xs text-subtle">{formatDate(p.creadoEn)}</p>
              </div>
              <p className="shrink-0 font-semibold text-ink">{formatCurrency(p.monto)}</p>
            </Card>
          </li>
        ))}
      </ul>

      <Card padding="none" className="hidden overflow-hidden md:block">
        <table className="w-full text-left text-sm">
          <caption className="sr-only">Historial de pagos</caption>
          <thead className="bg-surface-muted text-subtle">
            <tr>
              <th scope="col" className="px-6 py-3 font-medium">Servicio</th>
              <th scope="col" className="px-6 py-3 font-medium">Fecha</th>
              <th scope="col" className="px-6 py-3 font-medium">Método</th>
              <th scope="col" className="px-6 py-3 font-medium">Estado</th>
              <th scope="col" className="px-6 py-3 text-right font-medium">Monto</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {data.map((p) => (
              <tr key={p.id}>
                <td className="max-w-xs truncate px-6 py-4 text-ink">{p.descripcion}</td>
                <td className="px-6 py-4 text-subtle">{formatDate(p.creadoEn)}</td>
                <td className="px-6 py-4 capitalize text-subtle">{p.metodoPago}</td>
                <td className="px-6 py-4">
                  <Badge variant="completado">Pagado</Badge>
                </td>
                <td className="px-6 py-4 text-right font-semibold text-ink">{formatCurrency(p.monto)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </>
  );
}
