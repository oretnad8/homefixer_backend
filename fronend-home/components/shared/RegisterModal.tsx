'use client';

import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { MailCheck } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { useAuth } from '@/hooks/useAuth';
import { SupabaseAuthError } from '@/lib/supabase';
import type { UserRole } from '@/lib/types';

const registroSchema = z
  .object({
    nombre: z.string().trim().min(2, 'Ingresa tu nombre'),
    apellido: z.string().trim().min(2, 'Ingresa tu apellido'),
    email: z.string().trim().min(1, 'Ingresa tu correo').email('Correo no válido'),
    password: z.string().min(8, 'Mínimo 8 caracteres'),
    confirmar: z.string(),
  })
  .refine((d) => d.password === d.confirmar, { path: ['confirmar'], message: 'Las contraseñas no coinciden' });

type RegistroValues = z.infer<typeof registroSchema>;

function mensajeRegistro(e: unknown): string {
  if (e instanceof SupabaseAuthError) {
    if (e.code === 'user_already_exists' || e.code === 'email_exists') return 'Ya existe una cuenta con ese correo.';
    if (e.code === 'weak_password') return 'La contraseña es muy débil.';
    if (e.code === 'over_email_send_rate_limit') return 'Se enviaron demasiados correos. Intenta en unos minutos.';
    return e.message;
  }
  return 'No pudimos crear tu cuenta. Intenta de nuevo.';
}

/** Registro en Supabase: bottom sheet en mobile, modal centrado en desktop */
export function RegisterModal({ rol, open, onClose }: { rol: UserRole; open: boolean; onClose: () => void }) {
  const { register: registrar } = useAuth();
  const [correoEnviado, setCorreoEnviado] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<RegistroValues>({ resolver: zodResolver(registroSchema) });

  const onSubmit = async ({ nombre, apellido, email, password }: RegistroValues) => {
    try {
      const resultado = await registrar({ nombre, apellido, email, password }, rol);
      if (resultado === 'confirmar-correo') setCorreoEnviado(email);
    } catch (e) {
      setError('root', { message: mensajeRegistro(e) });
    }
  };

  const cerrar = () => {
    setCorreoEnviado(null);
    reset();
    onClose();
  };

  if (correoEnviado) {
    return (
      <Modal
        open={open}
        onClose={cerrar}
        title="Confirma tu correo"
        size="sm"
        footer={
          <Button width="full" onClick={cerrar}>
            Entendido
          </Button>
        }
      >
        <div className="flex flex-col items-center py-4 text-center">
          <MailCheck className="size-16 text-success" aria-hidden />
          <p className="mt-3 text-sm text-subtle">
            Te enviamos un enlace de confirmación a <strong className="text-ink">{correoEnviado}</strong>. Ábrelo y
            luego inicia sesión con tu correo y contraseña.
          </p>
        </div>
      </Modal>
    );
  }

  return (
    <Modal
      open={open}
      onClose={cerrar}
      title="Crear Cuenta"
      description={rol === 'cliente' ? 'Solicita técnicos en minutos.' : 'Recibe solicitudes de clientes cerca de ti.'}
    >
      <form id="form-registro" onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4 pt-2">
        <div className="grid gap-4 md:grid-cols-2">
          <Input label="Nombre" autoComplete="given-name" error={errors.nombre?.message} {...register('nombre')} />
          <Input label="Apellido" autoComplete="family-name" error={errors.apellido?.message} {...register('apellido')} />
        </div>
        <Input label="Correo electrónico" type="email" autoComplete="email" error={errors.email?.message} {...register('email')} />
        <Input label="Contraseña" type="password" autoComplete="new-password" error={errors.password?.message} {...register('password')} />
        <Input label="Confirmar contraseña" type="password" autoComplete="new-password" error={errors.confirmar?.message} {...register('confirmar')} />
        {errors.root && (
          <p role="alert" className="text-sm font-medium text-danger">
            {errors.root.message}
          </p>
        )}
        <Button type="submit" width="full" size="lg" loading={isSubmitting}>
          Crear Cuenta
        </Button>
      </form>
    </Modal>
  );
}
