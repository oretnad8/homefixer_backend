'use client';

import { useState } from 'react';
import { isAxiosError } from 'axios';
import { CircleCheck, ExternalLink, FileText, ShieldCheck, XCircle } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { TextArea } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { EmptyState, ErrorState, LoadingRegion, Skeleton } from '@/components/ui/States';
import { InsigniaEmblema } from '@/components/shared/InsigniaEmblema';
import { aprobarCertificacion, rechazarCertificacion, useCertificacionesPendientes } from '@/hooks/useMaestria';
import { usePageTitle } from '@/hooks/usePageTitle';
import type { Certificacion } from '@/lib/types';
import { CATEGORIA_LABELS, formatDate, formatRelativeDate } from '@/lib/utils';

// ============================================================
// Certificaciones pendientes (Administrador)
// ============================================================
// Revisar → Aprobar (otorga la insignia de maestría) / Rechazar (con motivo).
// ms-maestria publica la decisión y ms-notificacion avisa al técnico.

function mensajeError(e: unknown): string {
  if (isAxiosError(e)) {
    const msg = (e.response?.data as { message?: string } | undefined)?.message;
    if (msg) return msg;
  }
  return 'No se pudo guardar la decisión. Intenta nuevamente.';
}

export default function CertificacionesAdminPage() {
  usePageTitle('Certificaciones');
  const { data, isLoading, error, refetch } = useCertificacionesPendientes();
  const [procesando, setProcesando] = useState<string | null>(null);
  const [rechazando, setRechazando] = useState<Certificacion | null>(null);
  const [motivo, setMotivo] = useState('');
  const [aviso, setAviso] = useState<{ tipo: 'ok' | 'error'; texto: string } | null>(null);

  const aprobar = async (c: Certificacion) => {
    setProcesando(c.id);
    setAviso(null);
    try {
      await aprobarCertificacion(c.id);
      setAviso({ tipo: 'ok', texto: `Aprobada. ${c.tecnicoNombre ?? 'El técnico'} recibió la insignia Maestro en ${CATEGORIA_LABELS[c.categoria]}.` });
      refetch();
    } catch (e) {
      setAviso({ tipo: 'error', texto: mensajeError(e) });
    } finally {
      setProcesando(null);
    }
  };

  const rechazar = async () => {
    if (!rechazando || !motivo.trim()) return;
    setProcesando(rechazando.id);
    setAviso(null);
    try {
      await rechazarCertificacion(rechazando.id, motivo.trim());
      setAviso({ tipo: 'ok', texto: `Certificación rechazada. Se notificó a ${rechazando.tecnicoNombre ?? 'el técnico'}.` });
      setRechazando(null);
      setMotivo('');
      refetch();
    } catch (e) {
      setAviso({ tipo: 'error', texto: mensajeError(e) });
    } finally {
      setProcesando(null);
    }
  };

  if (isLoading && !data) {
    return (
      <LoadingRegion className="grid gap-4 md:grid-cols-2">
        {[0, 1].map((i) => (
          <Skeleton key={i} className="h-56 rounded-card" />
        ))}
      </LoadingRegion>
    );
  }
  if (error) return <ErrorState message={error.message} onRetry={refetch} />;

  return (
    <div className="space-y-4">
      <p className="text-sm text-subtle">Certificaciones subidas por técnicos, de la más antigua a la más reciente.</p>

      {aviso && (
        <p
          role={aviso.tipo === 'ok' ? 'status' : 'alert'}
          className={
            aviso.tipo === 'ok'
              ? 'rounded-input bg-success-light px-4 py-3 text-sm font-medium text-success-dark'
              : 'rounded-input bg-danger-light px-4 py-3 text-sm font-medium text-danger-dark'
          }
        >
          {aviso.texto}
        </p>
      )}

      {!data?.length ? (
        <EmptyState
          icon={<ShieldCheck />}
          title="No hay certificaciones pendientes"
          description="Cuando un técnico suba una certificación aparecerá aquí para revisarla."
        />
      ) : (
        <ul className="grid gap-4 md:grid-cols-2">
          {data.map((c) => (
            <li key={c.id}>
              <Card as="article" className="flex h-full flex-col gap-4">
                <div className="flex items-start gap-3">
                  <InsigniaEmblema categoria={c.categoria} />
                  <div className="min-w-0">
                    <h2 className="font-semibold text-ink">{c.titulo}</h2>
                    <p className="text-sm text-subtle">{c.institucion}</p>
                    <p className="mt-1 text-sm text-ink">
                      {c.tecnicoNombre ?? `Técnico #${c.tecnicoId}`} · {CATEGORIA_LABELS[c.categoria]}
                    </p>
                  </div>
                </div>
                <dl className="space-y-1 text-sm">
                  {c.fechaEmision && (
                    <div className="flex justify-between gap-4">
                      <dt className="text-subtle">Emitida</dt>
                      <dd className="text-ink">{formatDate(c.fechaEmision)}</dd>
                    </div>
                  )}
                  {c.fechaSolicitud && (
                    <div className="flex justify-between gap-4">
                      <dt className="text-subtle">Enviada</dt>
                      <dd className="text-ink">{formatRelativeDate(c.fechaSolicitud)}</dd>
                    </div>
                  )}
                </dl>
                {c.urlDocumento && (
                  <a
                    href={c.urlDocumento}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-1 text-sm font-medium text-primary-600 hover:underline"
                  >
                    <FileText className="size-4" aria-hidden /> Ver documento <ExternalLink className="size-3" aria-hidden />
                  </a>
                )}
                <div className="mt-auto grid grid-cols-2 gap-2 border-t border-border pt-3">
                  <Button
                    variant="outline-danger"
                    size="sm"
                    width="full"
                    disabled={procesando !== null}
                    onClick={() => setRechazando(c)}
                    leftIcon={<XCircle className="size-4" />}
                  >
                    Rechazar
                  </Button>
                  <Button
                    variant="success"
                    size="sm"
                    width="full"
                    loading={procesando === c.id}
                    disabled={procesando !== null}
                    onClick={() => aprobar(c)}
                    leftIcon={<CircleCheck className="size-4" />}
                  >
                    Aprobar
                  </Button>
                </div>
              </Card>
            </li>
          ))}
        </ul>
      )}

      <Modal
        open={rechazando !== null}
        onClose={() => setRechazando(null)}
        title="Rechazar certificación"
        description="El técnico verá este motivo y podrá subir una nueva certificación."
        size="sm"
        footer={
          <>
            <Button variant="outline" onClick={() => setRechazando(null)}>
              Cancelar
            </Button>
            <Button variant="danger" onClick={rechazar} loading={procesando !== null} disabled={!motivo.trim()}>
              Rechazar
            </Button>
          </>
        }
      >
        <TextArea label="Motivo del rechazo" value={motivo} onChange={(e) => setMotivo(e.target.value)} rows={4} />
      </Modal>
    </div>
  );
}
