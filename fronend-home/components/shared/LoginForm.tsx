'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import type { ReactNode } from 'react';
import { Lock, Mail, UserX } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { RolNoAsignadoError, useAuth } from '@/hooks/useAuth';
import { LOGIN_PATH } from '@/lib/session';
import { SupabaseAuthError } from '@/lib/supabase';
import type { UserRole } from '@/lib/types';
import { RegisterModal } from './RegisterModal';
import { SocialButtons } from './SocialButtons';

const loginSchema = z.object({
  email: z.string().trim().min(1, 'Ingresa tu correo').email('Correo no válido'),
  password: z.string().min(6, 'Mínimo 6 caracteres'),
});

type LoginValues = z.infer<typeof loginSchema>;

const ROLES: readonly UserRole[] = ['cliente', 'tecnico', 'admin'];

interface AvisoRol {
  titulo: string;
  texto: ReactNode;
  /** Portal al que sí puede entrar la cuenta (botón principal del modal) */
  destino?: { rol: UserRole; etiqueta: string };
}

/** Texto del modal según el portal usado y el rol que realmente tiene la cuenta. */
function avisoRol(rol: UserRole, rolActual: UserRole | null): AvisoRol {
  if (rol === 'admin') {
    return {
      titulo: 'Sin acceso de administrador',
      texto: (
        <>
          Tu cuenta no tiene permisos de administración. Los administradores se configuran en{' '}
          <strong>ADMIN_EMAILS</strong> del servicio de usuarios.
        </>
      ),
    };
  }
  if (rolActual === 'admin') {
    return {
      titulo: 'Tu cuenta es de administrador',
      texto: (
        <>
          Este correo está en <strong>ADMIN_EMAILS</strong>, por lo que solo tiene el rol de administrador y no puede
          ingresar como {rol === 'tecnico' ? 'técnico' : 'cliente'}. Ingresa por el portal de administración.
        </>
      ),
      destino: { rol: 'admin', etiqueta: 'Ir a administración' },
    };
  }
  if (rolActual === null) {
    return {
      titulo: 'Tu cuenta no tiene un rol asignado',
      texto: 'Tu cuenta no está registrada como cliente ni como técnico. Contacta a un administrador.',
    };
  }
  if (rol === 'tecnico') {
    return {
      titulo: 'No estás registrado como técnico',
      texto: (
        <>
          Tu cuenta no tiene el perfil de técnico. Ingresa como cliente y, desde <strong>Mi Perfil</strong>, pulsa{' '}
          <strong>Trabaja con nosotros</strong> para postular.
        </>
      ),
      destino: { rol: 'cliente', etiqueta: 'Ingresar como cliente' },
    };
  }
  return {
    titulo: 'Tu cuenta es de técnico',
    texto: (
      <>
        Tu cuenta está registrada como técnico, por lo que no puedes ingresar como cliente. Si quieres volver a ser
        cliente, ingresa como técnico y, desde <strong>Mi Perfil</strong>, pulsa <strong>Renunciar como técnico</strong>.
      </>
    ),
    destino: { rol: 'tecnico', etiqueta: 'Ingresar como técnico' },
  };
}

export function LoginForm({ rol }: { rol: UserRole }) {
  const { login } = useAuth();
  const searchParams = useSearchParams();
  const next = searchParams.get('next');
  // ?error= lo agrega /auth/callback cuando falla el login con Google
  const errorOAuth = searchParams.get('error');
  const [registroOpen, setRegistroOpen] = useState(false);
  // Rol real de la cuenta cuando entró por un portal que no le corresponde (undefined = sin aviso)
  const rolActualOAuth = searchParams.get('rol-actual');
  const [rolNoAsignado, setRolNoAsignado] = useState<{ rolActual: UserRole | null } | undefined>(
    errorOAuth === 'rol-no-asignado'
      ? { rolActual: ROLES.find((r) => r === rolActualOAuth) ?? null }
      : undefined
  );

  // Si la URL de retorno no está en "Redirect URLs" de Supabase, este vuelve a la
  // Site URL (/ → /login-cliente) con la sesión en el #fragmento: se deriva al callback.
  useEffect(() => {
    if (window.location.hash.includes('access_token=')) {
      window.location.replace(`/auth/callback${window.location.hash}`);
    }
  }, []);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginValues>({ resolver: zodResolver(loginSchema) });
  const mensajeError =
    errors.root?.message ?? (errorOAuth === 'oauth' ? 'No se pudo iniciar sesión con el proveedor externo.' : null);

  const onSubmit = async (values: LoginValues) => {
    try {
      await login(values, rol, next);
    } catch (e) {
      if (e instanceof RolNoAsignadoError) {
        setRolNoAsignado({ rolActual: e.rolActual });
        return;
      }
      if (e instanceof SupabaseAuthError && e.code === 'email_not_confirmed') {
        setError('root', { message: 'Debes confirmar tu correo antes de iniciar sesión. Revisa tu bandeja de entrada.' });
        return;
      }
      if (e instanceof SupabaseAuthError) {
        setError('root', { message: 'Correo o contraseña incorrectos.' });
        return;
      }
      setError('root', { message: 'No se pudo conectar con el servidor. Intenta nuevamente.' });
    }
  };

  const otroRol = rol === 'cliente' ? 'tecnico' : 'cliente';
  // El portal de administración no tiene registro ni enlace a los otros portales
  const esAdmin = rol === 'admin';
  const aviso = rolNoAsignado && avisoRol(rol, rolNoAsignado.rolActual);
  const cerrarAviso = () => setRolNoAsignado(undefined);

  return (
    <>
      {/* method="post": si el JS aún no cargó, la contraseña nunca va en la URL */}
      <form method="post" onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
        <Input
          label="Correo electrónico"
          type="email"
          autoComplete="email"
          leftIcon={<Mail />}
          error={errors.email?.message}
          {...register('email')}
        />
        <Input
          label="Contraseña"
          type="password"
          autoComplete="current-password"
          leftIcon={<Lock />}
          error={errors.password?.message}
          {...register('password')}
        />

        <div className="flex justify-end">
          <button type="button" className="text-sm font-medium text-primary-600 hover:underline">
            ¿Olvidaste tu contraseña?
          </button>
        </div>

        {mensajeError && (
          <p role="alert" className="rounded-input bg-danger-light px-4 py-3 text-sm font-medium text-danger-dark">
            {mensajeError}
          </p>
        )}

        <Button type="submit" width="full" size="lg" loading={isSubmitting}>
          Iniciar Sesión
        </Button>
        {!esAdmin && (
          <Button variant="outline" width="full" size="lg" onClick={() => setRegistroOpen(true)}>
            Crear Cuenta
          </Button>
        )}
      </form>

      <div className="my-6 flex items-center gap-3 text-xs uppercase tracking-wide text-subtle">
        <span className="h-px flex-1 bg-border" />o continúa con<span className="h-px flex-1 bg-border" />
      </div>

      <SocialButtons rol={rol} next={next} onError={(message) => setError('root', { message })} />

      {!esAdmin && (
        <p className="mt-6 text-center text-sm text-subtle">
          {rol === 'cliente' ? '¿Eres técnico?' : '¿Buscas un técnico?'}{' '}
          <Link href={`/login-${otroRol}`} className="font-semibold text-primary-600 hover:underline">
            {rol === 'cliente' ? 'Ingresa como técnico' : 'Ingresa como cliente'}
          </Link>
        </p>
      )}

      {!esAdmin && <RegisterModal rol={rol} open={registroOpen} onClose={() => setRegistroOpen(false)} />}

      <Modal
        open={!!aviso}
        onClose={cerrarAviso}
        title={aviso ? aviso.titulo : ''}
        size="sm"
        footer={
          aviso?.destino ? (
            <>
              <Button variant="outline" onClick={cerrarAviso}>
                Cerrar
              </Button>
              <Button href={LOGIN_PATH[aviso.destino.rol]}>{aviso.destino.etiqueta}</Button>
            </>
          ) : (
            <Button onClick={cerrarAviso}>Cerrar</Button>
          )
        }
      >
        <div className="flex flex-col items-center py-4 text-center">
          <UserX className="size-14 text-danger" aria-hidden />
          <p className="mt-3 text-sm text-subtle">{aviso?.texto}</p>
        </div>
      </Modal>
    </>
  );
}
