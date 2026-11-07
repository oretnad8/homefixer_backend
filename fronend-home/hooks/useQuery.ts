'use client';

import { useCallback, useEffect, useEffectEvent, useState } from 'react';

// ============================================================
// useQuery — carga asíncrona mínima con loading / error / refetch
// ============================================================
// Todos los hooks de datos se apoyan en este. Si más adelante se adopta
// TanStack Query o SWR, basta con reimplementar este archivo.

export interface QueryResult<T> {
  data: T | undefined;
  error: Error | null;
  isLoading: boolean;
  refetch: () => void;
}

interface Resultado<T> {
  key: string;
  data?: T;
  error: Error | null;
}

/**
 * @param key Identificador de la consulta; cambiarlo vuelve a cargar.
 *            Pasar null deja la consulta en espera (no hace fetch).
 */
export function useQuery<T>(key: string | null, fetcher: () => Promise<T>): QueryResult<T> {
  const [version, setVersion] = useState(0);
  const [resultado, setResultado] = useState<Resultado<T> | null>(null);
  const currentKey = key === null ? null : `${key}#${version}`;

  const ejecutar = useEffectEvent(() => fetcher());

  useEffect(() => {
    if (currentKey === null) return;
    let cancelado = false;
    ejecutar()
      .then((data) => {
        if (!cancelado) setResultado({ key: currentKey, data, error: null });
      })
      .catch((err: unknown) => {
        if (!cancelado) {
          const error = err instanceof Error ? err : new Error('Error desconocido');
          setResultado({ key: currentKey, error });
        }
      });
    return () => {
      cancelado = true;
    };
  }, [currentKey]);

  const refetch = useCallback(() => setVersion((v) => v + 1), []);

  // Mientras llega la nueva respuesta se conservan los datos anteriores
  // de la misma consulta (evita parpadeos al hacer refetch).
  const mismaConsulta = resultado !== null && key !== null && resultado.key.startsWith(`${key}#`);

  return {
    data: mismaConsulta ? resultado.data : undefined,
    error: resultado?.key === currentKey ? resultado.error : null,
    isLoading: currentKey !== null && resultado?.key !== currentKey,
    refetch,
  };
}
