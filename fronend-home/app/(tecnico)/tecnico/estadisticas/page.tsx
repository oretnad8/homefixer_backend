'use client';

import { Briefcase, ClipboardList, Star, Wallet } from 'lucide-react';
import { Card } from '@/components/ui/Card';
import { ErrorState, LoadingRegion, Skeleton } from '@/components/ui/States';
import { StatCard } from '@/components/shared/StatCard';
import { useEstadisticasTecnico } from '@/hooks/useTecnicos';
import { usePageTitle } from '@/hooks/usePageTitle';
import { formatCurrency } from '@/lib/utils';

export default function EstadisticasPage() {
  usePageTitle('Estadísticas');
  const { data, isLoading, error, refetch } = useEstadisticasTecnico();

  if (isLoading) {
    return (
      <LoadingRegion className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {[0, 1, 2, 3].map((i) => (
          <Skeleton key={i} className="h-32 rounded-card" />
        ))}
      </LoadingRegion>
    );
  }
  if (error || !data) return <ErrorState message={error?.message} onRetry={refetch} />;

  return (
    <div className="space-y-6">
      <div className="grid gap-3 sm:grid-cols-2 lg:gap-4 xl:grid-cols-4">
        <StatCard label="Trabajos hoy" value={data.trabajosHoy} icon={<Briefcase />} />
        <StatCard label="Ganancias de la semana" value={formatCurrency(data.gananciasSemana)} icon={<Wallet />} tone="success" />
        <StatCard label="Calificación" value={data.calificacion.toFixed(1)} icon={<Star />} tone="gold" />
        <StatCard label="Completados" value={data.trabajosCompletados} icon={<ClipboardList />} tone="warning" />
      </div>

      <Card padding="none" className="max-w-2xl">
        <h2 className="px-5 pt-5 text-lg font-semibold text-ink md:px-6">Ganancias por día</h2>
        <table className="mt-3 w-full text-sm">
          <caption className="sr-only">Ganancias de la semana por día</caption>
          <tbody className="divide-y divide-border">
            {data.gananciasPorDia.map((d) => (
              <tr key={d.dia}>
                <th scope="row" className="px-5 py-3 text-left font-medium text-subtle md:px-6">
                  {d.dia}
                </th>
                <td className="px-5 py-3 text-right font-semibold tabular-nums text-ink md:px-6">{formatCurrency(d.monto)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
    </div>
  );
}
