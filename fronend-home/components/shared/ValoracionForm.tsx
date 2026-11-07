'use client';

import { useState } from 'react';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { AnimatePresence, motion } from 'framer-motion';
import { CircleCheck } from 'lucide-react';
import { Avatar } from '@/components/ui/Avatar';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { TextArea } from '@/components/ui/Input';
import { StarRating } from '@/components/ui/StarRating';
import { enviarValoracion } from '@/hooks/useValoraciones';
import { cn } from '@/lib/utils';

// ============================================================
// ValoracionForm — compartido por cliente (valora al técnico) y
// técnico (valora al cliente). Mobile apilado; desktop tarjeta max-w-md.
// ============================================================

const schema = z.object({
  calificacion: z.number().min(1, 'Selecciona al menos una estrella').max(5),
  comentario: z.string().max(500, 'Máximo 500 caracteres'),
});

type Values = z.infer<typeof schema>;

const ETIQUETAS = ['', 'Muy malo', 'Malo', 'Regular', 'Bueno', '¡Excelente!'];

interface ValoracionFormProps {
  solicitudId: string;
  persona: { nombre: string; apellido?: string; subtitulo: string };
  pregunta: string;
  tags: string[];
  /** Destino al terminar */
  volverHref: string;
}

export function ValoracionForm({ solicitudId, persona, pregunta, tags, volverHref }: ValoracionFormProps) {
  const [seleccionados, setSeleccionados] = useState<string[]>([]);
  const [enviado, setEnviado] = useState(false);
  const {
    control,
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { calificacion: 0, comentario: '' } });

  const calificacion = useWatch({ control, name: 'calificacion' });

  const onSubmit = async (v: Values) => {
    try {
      const extra = seleccionados.length ? `[${seleccionados.join(', ')}] ` : '';
      await enviarValoracion({ solicitudId, calificacion: v.calificacion, comentario: `${extra}${v.comentario}`.trim() });
      setEnviado(true);
    } catch {
      setError('root', { message: 'No se pudo enviar la valoración.' });
    }
  };

  return (
    <Card padding="lg" className="mx-auto w-full md:max-w-md">
      <AnimatePresence mode="wait">
        {enviado ? (
          <motion.div
            key="ok"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="flex flex-col items-center py-6 text-center"
            role="status"
          >
            <CircleCheck className="size-16 text-success" aria-hidden />
            <h2 className="mt-4 text-xl font-bold text-ink">¡Gracias por tu valoración!</h2>
            <p className="mt-1 text-sm text-subtle">Tu opinión ayuda a mantener la calidad de HomeFixer.</p>
            <Button href={volverHref} width="full" size="lg" className="mt-6">
              Volver al inicio
            </Button>
          </motion.div>
        ) : (
          <motion.form key="form" exit={{ opacity: 0 }} onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col items-center text-center">
            <Avatar nombre={persona.nombre} apellido={persona.apellido} size="xl" />
            <h2 className="mt-3 text-lg font-semibold text-ink">
              {persona.nombre} {persona.apellido}
            </h2>
            <p className="text-sm text-subtle">{persona.subtitulo}</p>

            <p className="mt-6 font-medium text-ink">{pregunta}</p>
            <Controller
              control={control}
              name="calificacion"
              render={({ field }) => (
                <StarRating value={field.value} onChange={field.onChange} size="lg" className="mt-3" label={pregunta} />
              )}
            />
            <p className="mt-2 h-5 text-sm font-semibold text-gold-dark" aria-live="polite">
              {ETIQUETAS[calificacion]}
            </p>
            {errors.calificacion && (
              <p role="alert" className="text-xs font-medium text-danger">
                {errors.calificacion.message}
              </p>
            )}

            <div className="mt-5 flex flex-wrap justify-center gap-2" role="group" aria-label="Aspectos destacados">
              {tags.map((t) => {
                const on = seleccionados.includes(t);
                return (
                  <button
                    key={t}
                    type="button"
                    aria-pressed={on}
                    onClick={() => setSeleccionados((s) => (on ? s.filter((x) => x !== t) : [...s, t]))}
                    className={cn(
                      'rounded-pill border px-3 py-1.5 text-sm transition-colors',
                      on ? 'border-primary-500 bg-primary-50 font-medium text-primary-700' : 'border-border text-subtle hover:border-primary-300'
                    )}
                  >
                    {t}
                  </button>
                );
              })}
            </div>

            <TextArea label="Comentario (opcional)" className="mt-5 w-full text-left" rows={3} error={errors.comentario?.message} {...register('comentario')} />

            {errors.root && (
              <p role="alert" className="mt-3 text-sm text-danger">
                {errors.root.message}
              </p>
            )}

            <Button type="submit" variant="success" width="full" size="lg" className="mt-6" loading={isSubmitting}>
              Enviar Valoración
            </Button>
            <Button href={volverHref} variant="ghost" width="full" className="mt-2">
              Omitir
            </Button>
          </motion.form>
        )}
      </AnimatePresence>
    </Card>
  );
}
