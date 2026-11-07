'use client';

import { useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import { ArrowLeft, Phone, Send } from 'lucide-react';
import { Avatar } from '@/components/ui/Avatar';
import { ErrorState, LoadingRegion, Skeleton } from '@/components/ui/States';
import { useChat } from '@/hooks/useChat';
import { useSolicitud } from '@/hooks/useSolicitudes';
import { usePageTitle } from '@/hooks/usePageTitle';
import { cn, formatTime } from '@/lib/utils';
import { useAuthStore } from '@/store/authStore';

// ============================================================
// ChatView — chat cliente ↔ técnico de una solicitud
// ============================================================
// Mobile: ocupa toda la pantalla bajo el topbar (sin márgenes).
// Desktop: panel dentro del AppShell con cabecera de la otra parte, mensajes en
//          una columna de max ~700px y burbujas de max 60% de ancho.

export function ChatView() {
  usePageTitle('Chat');
  const { id } = useParams<{ id: string }>();
  const user = useAuthStore((s) => s.user);
  const { data: solicitud } = useSolicitud(id);
  const { mensajes, isLoading, error, refetch, enviar, escribiendo } = useChat(id);
  const [texto, setTexto] = useState('');
  const finRef = useRef<HTMLDivElement>(null);
  const soyCliente = user?.rol !== 'tecnico';

  // La otra parte del chat: el técnico para el cliente y el cliente para el técnico
  const [clienteNombre, ...clienteApellido] = (solicitud?.clienteNombre ?? 'Cliente').split(' ');
  const contraparte = !solicitud
    ? null
    : soyCliente
      ? solicitud.tecnico
        ? { nombre: solicitud.tecnico.nombre, apellido: solicitud.tecnico.apellido, telefono: solicitud.tecnico.telefono }
        : null
      : { nombre: clienteNombre, apellido: clienteApellido.join(' '), telefono: undefined };
  const volverHref = soyCliente ? `/cliente/servicio-en-progreso/${id}` : `/tecnico/servicio-en-progreso/${id}`;

  useEffect(() => {
    finRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [mensajes.length, escribiendo]);

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!texto.trim()) return;
    void enviar(texto);
    setTexto('');
  };

  return (
    <div
      className={cn(
        // Mobile: pantalla completa bajo el topbar (4rem)
        '-mx-4 -my-4 flex h-[calc(100dvh-4rem)] flex-col bg-surface',
        'md:-mx-6 md:-my-6',
        // Desktop: panel con borde dentro del contenido
        'lg:mx-0 lg:my-0 lg:h-[calc(100dvh-8rem)] lg:overflow-hidden lg:rounded-card lg:border lg:border-border lg:shadow-card'
      )}
    >
      {/* Cabecera con la otra parte */}
      <div className="flex shrink-0 items-center gap-3 border-b border-border px-3 py-3 md:px-5">
        <Link
          href={volverHref}
          className="flex size-10 items-center justify-center rounded-pill text-subtle hover:bg-surface-muted hover:text-ink"
          aria-label="Volver al servicio"
        >
          <ArrowLeft className="size-5" />
        </Link>
        {contraparte ? (
          <>
            <Avatar nombre={contraparte.nombre} apellido={contraparte.apellido} size="sm" />
            <div className="min-w-0 flex-1">
              <p className="truncate font-semibold text-ink">
                {contraparte.nombre} {contraparte.apellido}
              </p>
              <p className="text-xs text-subtle">{escribiendo ? 'Escribiendo…' : soyCliente ? 'Tu técnico' : 'Cliente'}</p>
            </div>
            {contraparte.telefono && (
              <a
                href={`tel:${contraparte.telefono}`}
                className="flex size-10 items-center justify-center rounded-pill text-primary-600 hover:bg-primary-50"
                aria-label={`Llamar a ${contraparte.nombre}`}
              >
                <Phone className="size-5" />
              </a>
            )}
          </>
        ) : (
          <Skeleton className="h-9 w-40" />
        )}
      </div>

      {/* Mensajes */}
      <div className="scrollbar-thin flex-1 overflow-y-auto bg-surface-muted px-3 py-4 md:px-6">
        <div className="mx-auto flex max-w-[700px] flex-col gap-3" role="log" aria-live="polite" aria-label="Mensajes">
          {isLoading ? (
            <LoadingRegion label="Cargando mensajes" className="flex flex-col gap-3">
              <Skeleton className="h-12 w-3/5 rounded-card" />
              <Skeleton className="ml-auto h-10 w-2/5 rounded-card" />
              <Skeleton className="h-16 w-1/2 rounded-card" />
            </LoadingRegion>
          ) : error ? (
            <ErrorState message={error.message} onRetry={refetch} />
          ) : mensajes.length === 0 ? (
            <p className="py-10 text-center text-sm text-subtle">
              Aún no hay mensajes. ¡Saluda a tu {soyCliente ? 'técnico' : 'cliente'}!
            </p>
          ) : (
            mensajes.map((m) => {
              const mio = m.esCliente === soyCliente;
              return (
                <div key={m.id} className={cn('flex', mio ? 'justify-end' : 'justify-start')}>
                  <div
                    className={cn(
                      'max-w-[80%] rounded-card px-4 py-2.5 text-sm shadow-card lg:max-w-[60%]',
                      mio ? 'rounded-br-sm bg-primary-500 text-white' : 'rounded-bl-sm bg-surface text-ink'
                    )}
                  >
                    <p className="sr-only">{mio ? 'Tú' : m.remitenteNombre}:</p>
                    <p className="whitespace-pre-wrap break-words">{m.texto}</p>
                    <p className={cn('mt-1 text-right text-[11px]', mio ? 'text-primary-100' : 'text-subtle')}>{formatTime(m.creadoEn)}</p>
                  </div>
                </div>
              );
            })
          )}
          {escribiendo && (
            <div className="flex gap-1 self-start rounded-card bg-surface px-4 py-3 shadow-card" aria-label="Escribiendo">
              {[0, 1, 2].map((i) => (
                <span key={i} className="size-2 animate-bounce rounded-pill bg-subtle" style={{ animationDelay: `${i * 150}ms` }} />
              ))}
            </div>
          )}
          <div ref={finRef} />
        </div>
      </div>

      {/* Entrada */}
      <form onSubmit={onSubmit} className="shrink-0 border-t border-border bg-surface px-3 py-3 pb-[max(0.75rem,env(safe-area-inset-bottom))] md:px-6">
        <div className="mx-auto flex max-w-[700px] items-center gap-2">
          <label htmlFor="mensaje" className="sr-only">
            Escribe un mensaje
          </label>
          <input
            id="mensaje"
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            placeholder="Escribe un mensaje…"
            autoComplete="off"
            className="h-12 flex-1 rounded-pill border border-border bg-surface-muted px-5 text-sm text-ink outline-none placeholder:text-subtle focus:border-primary-500 focus:ring-2 focus:ring-primary-100"
          />
          <button
            type="submit"
            disabled={!texto.trim()}
            className="flex size-12 shrink-0 items-center justify-center rounded-pill bg-success text-white shadow-card transition-colors hover:bg-success-dark disabled:opacity-50"
            aria-label="Enviar mensaje"
          >
            <Send className="size-5" />
          </button>
        </div>
      </form>
    </div>
  );
}
