'use client';

import { useState } from 'react';
import { isAxiosError } from 'axios';
import { BriefcaseBusiness, CircleCheck, LogOut, TriangleAlert, UserX } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Modal } from '@/components/ui/Modal';
import { useAuth } from '@/hooks/useAuth';
import api, { USE_MOCKS } from '@/lib/api';
import type { UserRole } from '@/lib/types';
import { delay } from '@/lib/utils';

// ============================================================
// CambioRolCard — "Trabaja con nosotros" (cliente) / "Renunciar como técnico" (técnico)
// ============================================================
// Los roles son excluyentes: el usuario pasa de un rol al otro.
//   POST   /tecnicos/postular/:usuarioId  → pasa de cliente a técnico
//   DELETE /tecnicos/:id/renunciar        → vuelve a cliente; 409 si tiene trabajos activos

type Paso = 'confirmar-renuncia' | 'exito' | 'cerrar-sesion' | 'error' | null;

function mensajeError(e: unknown): string {
  if (isAxiosError(e)) {
    const msg = (e.response?.data as { message?: string } | undefined)?.message;
    if (msg) return msg;
  }
  return 'No se pudo completar la operación. Intenta nuevamente.';
}

export function CambioRolCard() {
  const { user, logout } = useAuth();
  const [paso, setPaso] = useState<Paso>(null);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState('');

  if (!user) return null;
  const esTecnico = user.rol === 'tecnico';
  /** Login al que se envía al usuario tras el cambio de rol */
  const nuevoRol: UserRole = esTecnico ? 'cliente' : 'tecnico';

  const ejecutar = async () => {
    setEnviando(true);
    try {
      if (USE_MOCKS) await delay(600);
      else if (esTecnico) await api.delete(`/tecnicos/${user.id}/renunciar`);
      else await api.post(`/tecnicos/postular/${user.id}`);
      setPaso('exito');
    } catch (e) {
      setError(mensajeError(e));
      setPaso('error');
    } finally {
      setEnviando(false);
    }
  };

  const cerrar = () => setPaso(null);

  return (
    <>
      <Card className="lg:col-span-2">
        <div className="flex flex-col items-center gap-4 text-center md:flex-row md:text-left">
          <div className="flex size-12 shrink-0 items-center justify-center rounded-pill bg-primary-50 text-primary-600">
            {esTecnico ? <UserX className="size-6" aria-hidden /> : <BriefcaseBusiness className="size-6" aria-hidden />}
          </div>
          <div className="flex-1">
            <h2 className="text-base font-semibold text-ink">
              {esTecnico ? 'Dejar de ser técnico' : '¿Quieres ofrecer tus servicios?'}
            </h2>
            <p className="text-sm text-subtle">
              {esTecnico
                ? 'Puedes renunciar a tu perfil de técnico si no tienes trabajos activos. Tu cuenta volverá a ser de cliente.'
                : 'Únete a HomeFixer como técnico y recibe solicitudes de clientes cerca de ti. Tu cuenta pasará a ser de técnico y ya no podrás ingresar como cliente.'}
            </p>
          </div>
          {esTecnico ? (
            <Button variant="outline-danger" onClick={() => setPaso('confirmar-renuncia')}>
              Renunciar como técnico
            </Button>
          ) : (
            <Button onClick={ejecutar} loading={enviando} leftIcon={<BriefcaseBusiness className="size-4" />}>
              Trabaja con nosotros
            </Button>
          )}
        </div>
      </Card>

      {/* Confirmación de renuncia */}
      <Modal
        open={paso === 'confirmar-renuncia'}
        onClose={cerrar}
        title="¿Renunciar como técnico?"
        description="Dejarás de recibir solicitudes. Solo es posible si no tienes trabajos activos."
        size="sm"
        footer={
          <>
            <Button variant="outline" onClick={cerrar}>
              Cancelar
            </Button>
            <Button variant="danger" onClick={ejecutar} loading={enviando}>
              Sí, renunciar
            </Button>
          </>
        }
      >
        <div className="flex justify-center py-2">
          <TriangleAlert className="size-14 text-danger" aria-hidden />
        </div>
      </Modal>

      {/* Paso 1: resultado */}
      <Modal
        open={paso === 'exito'}
        onClose={() => setPaso('cerrar-sesion')}
        title={esTecnico ? 'Renuncia registrada' : 'Solicitud enviada'}
        size="sm"
        footer={
          <Button width="full" onClick={() => setPaso('cerrar-sesion')}>
            Aceptar
          </Button>
        }
      >
        <div className="flex flex-col items-center py-4 text-center">
          <CircleCheck className="size-16 text-success" aria-hidden />
          <p className="mt-3 text-sm text-subtle">
            {esTecnico
              ? 'Ya no formas parte del equipo de técnicos de HomeFixer.'
              : 'Tu solicitud para trabajar con nosotros fue enviada. ¡Bienvenido al equipo de técnicos!'}
          </p>
        </div>
      </Modal>

      {/* Paso 2: aviso de cierre de sesión → login del nuevo rol */}
      <Modal
        open={paso === 'cerrar-sesion'}
        onClose={() => logout(nuevoRol)}
        title="Se cerrará la sesión"
        size="sm"
        footer={
          <Button width="full" onClick={() => logout(nuevoRol)} leftIcon={<LogOut className="size-4" />}>
            Aceptar
          </Button>
        }
      >
        <p className="py-2 text-center text-sm text-subtle">
          Se cerrará sesión para que inicies desde tu nuevo entorno de {esTecnico ? 'cliente' : 'trabajo'}.
        </p>
      </Modal>

      {/* Error (p. ej. trabajos activos al renunciar) */}
      <Modal
        open={paso === 'error'}
        onClose={cerrar}
        title={esTecnico ? 'No es posible renunciar' : 'No se pudo enviar la solicitud'}
        size="sm"
        footer={
          <Button width="full" onClick={cerrar}>
            Entendido
          </Button>
        }
      >
        <div className="flex flex-col items-center py-4 text-center">
          <TriangleAlert className="size-14 text-danger" aria-hidden />
          <p className="mt-3 text-sm text-subtle">{error}</p>
        </div>
      </Modal>
    </>
  );
}
