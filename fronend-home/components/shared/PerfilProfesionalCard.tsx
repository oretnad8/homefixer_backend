'use client';

import { useState } from 'react';
import { CircleCheck } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { Input, Select } from '@/components/ui/Input';
import { actualizarPerfilProfesional } from '@/hooks/useTecnicos';
import type { CategoriaServicio, Tecnico } from '@/lib/types';
import { CATEGORIA_LABELS, aCategoria } from '@/lib/utils';
import { useAuthStore } from '@/store/authStore';

const CATEGORIAS = Object.keys(CATEGORIA_LABELS) as CategoriaServicio[];

/** Oficio y tarifa base del técnico (PATCH /tecnicos/:id/perfil-profesional). */
export function PerfilProfesionalCard() {
  const user = useAuthStore((s) => s.user) as Tecnico | null;
  const updateUser = useAuthStore((s) => s.updateUser);
  const actual = user?.categoria ?? aCategoria(user?.especialidad);
  const [categoria, setCategoria] = useState<CategoriaServicio>(actual);
  const [precio, setPrecio] = useState(String(user?.precioBase ?? ''));
  const [guardando, setGuardando] = useState(false);
  const [estado, setEstado] = useState<'ok' | string | null>(null);

  const precioNum = Number(precio);
  const precioValido = precio.trim() !== '' && Number.isFinite(precioNum) && precioNum >= 0;
  const cambios = categoria !== actual || (precioValido && precioNum !== (user?.precioBase ?? 0));

  const guardar = async () => {
    if (!precioValido) return setEstado('Ingresa un precio válido.');
    setGuardando(true);
    setEstado(null);
    try {
      await actualizarPerfilProfesional(categoria, precioNum);
      const datos: Partial<Tecnico> = { categoria, especialidad: CATEGORIA_LABELS[categoria], precioBase: precioNum };
      updateUser(datos);
      setEstado('ok');
    } catch {
      setEstado('No se pudo guardar. Intenta de nuevo.');
    } finally {
      setGuardando(false);
    }
  };

  return (
    <Card padding="lg" className="lg:col-start-2">
      <h2 className="text-lg font-semibold text-ink">Perfil profesional</h2>
      <p className="mb-5 mt-1 text-sm text-subtle">
        Solo recibirás solicitudes de tu especialidad y solo los clientes que la buscan te verán en el mapa.
      </p>
      <div className="grid gap-4 md:grid-cols-2">
        <Select
          label="Especialidad"
          value={categoria}
          onChange={(e) => {
            setCategoria(e.target.value as CategoriaServicio);
            setEstado(null);
          }}
          options={CATEGORIAS.map((c) => ({ value: c, label: CATEGORIA_LABELS[c] }))}
        />
        <Input
          label="Precio base (CLP)"
          type="number"
          inputMode="numeric"
          min={0}
          step={1000}
          value={precio}
          onChange={(e) => {
            setPrecio(e.target.value);
            setEstado(null);
          }}
          hint="Tarifa mínima por visita."
        />
      </div>
      <div className="mt-4 flex flex-col items-center gap-3 md:flex-row md:justify-end">
        {estado === 'ok' && (
          <p role="status" className="flex items-center gap-1 text-sm font-medium text-success">
            <CircleCheck className="size-4" aria-hidden /> Perfil profesional actualizado
          </p>
        )}
        {estado && estado !== 'ok' && (
          <p role="alert" className="text-sm font-medium text-danger">
            {estado}
          </p>
        )}
        <Button onClick={guardar} loading={guardando} disabled={!cambios}>
          Guardar especialidad
        </Button>
      </div>
    </Card>
  );
}
