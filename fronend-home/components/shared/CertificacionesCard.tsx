'use client';

import { useState } from 'react';
import { isAxiosError } from 'axios';
import { Award, ExternalLink, FileText, Lock, Upload } from 'lucide-react';
import { Badge, type BadgeVariant } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Input, Select } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { Skeleton } from '@/components/ui/States';
import {
  NIVELES,
  NIVEL_LABELS,
  NIVEL_REQUISITOS,
  alcanzaNivel,
  subirCertificacion,
  useCertificaciones,
  useInsignias,
  useNivelMinimo,
} from '@/hooks/useMaestria';
import { MAX_DOCUMENTO_MB, TIPOS_DOCUMENTO } from '@/lib/storage';
import type { CategoriaServicio, EstadoCertificacion, Tecnico } from '@/lib/types';
import { CATEGORIA_LABELS, aCategoria, cn, formatDate } from '@/lib/utils';
import { useAuthStore } from '@/store/authStore';
import { InsigniaEmblema } from './InsigniaEmblema';

// ============================================================
// CertificacionesCard — crecimiento profesional del técnico
// ============================================================
// Nivel de reputación → (si alcanza) sube certificación → un admin la valida
// → si la aprueba se otorga la insignia "Maestro en <oficio>".

const CATEGORIAS = Object.keys(CATEGORIA_LABELS) as CategoriaServicio[];

const ESTADO_CERT: Record<EstadoCertificacion, { label: string; variant: BadgeVariant }> = {
  pendiente: { label: 'En revisión', variant: 'pendiente' },
  aprobada: { label: 'Aprobada', variant: 'completado' },
  rechazada: { label: 'Rechazada', variant: 'danger' },
};

function mensajeError(e: unknown): string {
  if (isAxiosError(e)) {
    const msg = (e.response?.data as { message?: string } | undefined)?.message;
    if (msg) return msg;
  }
  if (e instanceof Error && e.message) return e.message;
  return 'No se pudo subir la certificación. Intenta nuevamente.';
}

export function CertificacionesCard() {
  const user = useAuthStore((s) => s.user) as Tecnico | null;
  const certificaciones = useCertificaciones(user?.id);
  const insignias = useInsignias(user?.id);
  const { data: nivelMinimo = 'BUENO' } = useNivelMinimo();
  const [abierto, setAbierto] = useState(false);

  if (!user) return null;
  const nivel = user.nivelReputacion ?? 'NUEVO';
  const habilitado = alcanzaNivel(nivel, nivelMinimo);
  const conInsignia = new Set(insignias.data?.map((i) => i.categoria));

  return (
    <Card padding="lg" className="lg:col-start-2">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-ink">Certificaciones e insignias</h2>
          <p className="mt-1 text-sm text-subtle">
            Sube tus certificados: al aprobarlos obtienes la insignia de maestría del oficio y apareces primero en las búsquedas.
          </p>
        </div>
        <Button
          onClick={() => setAbierto(true)}
          disabled={!habilitado}
          leftIcon={habilitado ? <Upload className="size-4" /> : <Lock className="size-4" />}
        >
          Subir certificación
        </Button>
      </div>

      {/* Nivel de reputación */}
      <section aria-labelledby="titulo-nivel" className="mt-5 rounded-card bg-surface-muted p-4">
        <h3 id="titulo-nivel" className="text-sm font-semibold text-ink">
          Nivel de reputación: {NIVEL_LABELS[nivel]}
        </h3>
        <ol className="mt-3 grid grid-cols-3 gap-2" aria-label="Niveles">
          {NIVELES.map((n) => (
            <li key={n} className="min-w-0">
              <div className={cn('h-1.5 rounded-pill', alcanzaNivel(nivel, n) ? 'bg-primary-500' : 'bg-border')} />
              <p className={cn('mt-1.5 text-xs font-semibold', n === nivel ? 'text-primary-700' : 'text-subtle')}>{NIVEL_LABELS[n]}</p>
              <p className="text-xs text-subtle">{NIVEL_REQUISITOS[n]}</p>
            </li>
          ))}
        </ol>
        {!habilitado && (
          <p className="mt-3 flex items-start gap-2 text-sm text-subtle">
            <Lock className="mt-0.5 size-4 shrink-0" aria-hidden />
            Necesitas nivel {NIVEL_LABELS[nivelMinimo]} para postular certificaciones. Sube de nivel con buenas valoraciones de tus clientes.
          </p>
        )}
      </section>

      {/* Insignias */}
      <section aria-labelledby="titulo-insignias" className="mt-6">
        <h3 id="titulo-insignias" className="mb-3 text-sm font-semibold text-ink">
          Insignias de maestría
        </h3>
        {insignias.isLoading && !insignias.data ? (
          <Skeleton className="h-20 rounded-card" />
        ) : insignias.data?.length ? (
          <ul className="flex flex-wrap gap-5">
            {insignias.data.map((i) => (
              <li key={i.id}>
                <InsigniaEmblema categoria={i.categoria} conNombre />
              </li>
            ))}
          </ul>
        ) : (
          <p className="flex items-center gap-2 text-sm text-subtle">
            <Award className="size-4" aria-hidden /> Aún no tienes insignias. Se otorgan al aprobarse una certificación.
          </p>
        )}
      </section>

      {/* Certificaciones */}
      <section aria-labelledby="titulo-certificaciones" className="mt-6">
        <h3 id="titulo-certificaciones" className="mb-3 text-sm font-semibold text-ink">
          Mis certificaciones
        </h3>
        {certificaciones.isLoading && !certificaciones.data ? (
          <Skeleton className="h-24 rounded-card" />
        ) : certificaciones.error ? (
          <p role="alert" className="text-sm text-danger">
            No se pudieron cargar tus certificaciones.
          </p>
        ) : certificaciones.data?.length ? (
          <ul className="divide-y divide-border rounded-card border border-border">
            {certificaciones.data.map((c) => (
              <li key={c.id} className="flex flex-col gap-2 p-4 md:flex-row md:items-start md:justify-between">
                <div className="min-w-0">
                  <p className="font-medium text-ink">{c.titulo}</p>
                  <p className="text-sm text-subtle">
                    {c.institucion} · {CATEGORIA_LABELS[c.categoria]}
                    {c.fechaEmision && ` · ${formatDate(c.fechaEmision)}`}
                  </p>
                  {c.estado === 'rechazada' && c.motivoRechazo && (
                    <p className="mt-1 text-sm text-danger-dark">Motivo: {c.motivoRechazo}</p>
                  )}
                  {c.urlDocumento && (
                    <a
                      href={c.urlDocumento}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="mt-1 inline-flex items-center gap-1 text-sm font-medium text-primary-600 hover:underline"
                    >
                      <FileText className="size-4" aria-hidden /> Ver documento <ExternalLink className="size-3" aria-hidden />
                    </a>
                  )}
                </div>
                <Badge variant={ESTADO_CERT[c.estado].variant} className="self-start">
                  {ESTADO_CERT[c.estado].label}
                </Badge>
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-sm text-subtle">Todavía no has subido certificaciones.</p>
        )}
      </section>

      <SubirCertificacionModal
        open={abierto}
        onClose={() => setAbierto(false)}
        categoriaInicial={user.categoria ?? aCategoria(user.especialidad)}
        bloqueadas={conInsignia}
        onSubida={() => {
          setAbierto(false);
          certificaciones.refetch();
        }}
      />
    </Card>
  );
}

interface SubirCertificacionModalProps {
  open: boolean;
  onClose: () => void;
  categoriaInicial: CategoriaServicio;
  /** Oficios en los que ya tiene insignia */
  bloqueadas: Set<CategoriaServicio>;
  onSubida: () => void;
}

function SubirCertificacionModal({ open, onClose, categoriaInicial, bloqueadas, onSubida }: SubirCertificacionModalProps) {
  const [titulo, setTitulo] = useState('');
  const [institucion, setInstitucion] = useState('');
  const [categoria, setCategoria] = useState<CategoriaServicio>(categoriaInicial);
  const [fechaEmision, setFechaEmision] = useState('');
  const [archivo, setArchivo] = useState<File | null>(null);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const cerrar = () => {
    setError(null);
    onClose();
  };

  const elegirArchivo = (f: File | undefined) => {
    setError(null);
    if (!f) return setArchivo(null);
    if (!TIPOS_DOCUMENTO.includes(f.type)) return setError('El documento debe ser PDF, JPG, PNG o WEBP.');
    if (f.size > MAX_DOCUMENTO_MB * 1024 * 1024) return setError(`El documento no puede superar ${MAX_DOCUMENTO_MB} MB.`);
    setArchivo(f);
  };

  const enviar = async () => {
    if (!titulo.trim() || !institucion.trim()) return setError('Completa el título y la institución.');
    if (!archivo) return setError('Adjunta el documento de la certificación.');
    if (bloqueadas.has(categoria)) return setError(`Ya tienes la insignia de ${CATEGORIA_LABELS[categoria]}.`);
    setEnviando(true);
    setError(null);
    try {
      await subirCertificacion({ titulo: titulo.trim(), institucion: institucion.trim(), categoria, fechaEmision, archivo });
      setTitulo('');
      setInstitucion('');
      setFechaEmision('');
      setArchivo(null);
      onSubida();
    } catch (e) {
      setError(mensajeError(e));
    } finally {
      setEnviando(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={cerrar}
      title="Subir certificación"
      description="Un administrador la revisará. Si la aprueba, recibirás la insignia de maestría del oficio."
      footer={
        <>
          <Button variant="outline" onClick={cerrar} disabled={enviando}>
            Cancelar
          </Button>
          <Button onClick={enviar} loading={enviando} leftIcon={<Upload className="size-4" />}>
            Enviar a revisión
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <Input label="Título del certificado" value={titulo} onChange={(e) => setTitulo(e.target.value)} />
        <Input label="Institución que lo emite" value={institucion} onChange={(e) => setInstitucion(e.target.value)} />
        <div className="grid gap-4 md:grid-cols-2">
          <Select
            label="Oficio que acredita"
            value={categoria}
            onChange={(e) => setCategoria(e.target.value as CategoriaServicio)}
            options={CATEGORIAS.map((c) => ({
              value: c,
              label: bloqueadas.has(c) ? `${CATEGORIA_LABELS[c]} (ya tienes la insignia)` : CATEGORIA_LABELS[c],
            }))}
          />
          <Input label="Fecha de emisión" type="date" value={fechaEmision} onChange={(e) => setFechaEmision(e.target.value)} />
        </div>
        <label className="flex cursor-pointer flex-col items-center gap-2 rounded-card border-2 border-dashed border-border p-5 text-center text-sm text-subtle hover:border-primary-300 focus-within:border-primary-500">
          <FileText className="size-6 text-primary-500" aria-hidden />
          <span className="font-medium text-ink">{archivo ? archivo.name : 'Adjuntar documento'}</span>
          <span>PDF o imagen, hasta {MAX_DOCUMENTO_MB} MB</span>
          <input
            type="file"
            accept={TIPOS_DOCUMENTO.join(',')}
            className="sr-only"
            onChange={(e) => elegirArchivo(e.target.files?.[0])}
          />
        </label>
        {error && (
          <p role="alert" className="rounded-input bg-danger-light px-4 py-3 text-sm font-medium text-danger-dark">
            {error}
          </p>
        )}
      </div>
    </Modal>
  );
}
