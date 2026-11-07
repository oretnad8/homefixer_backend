'use client';

import { create } from 'zustand';

// ============================================================
// STORE DEL SERVICIO EN CURSO (técnico)
// ============================================================
// Guarda fotos antes/después y notas entre "Servicio en Progreso" y
// "Servicio Completado". Las URLs son object URLs locales; al conectar el
// backend se reemplazan por las URLs devueltas al subir los archivos.

export interface FotosServicio {
  antes: string[];
  despues: string[];
}

interface ServicioState {
  fotos: Record<string, FotosServicio>;
  notas: Record<string, string>;
  addFoto: (id: string, tipo: keyof FotosServicio, url: string) => void;
  removeFoto: (id: string, tipo: keyof FotosServicio, url: string) => void;
  setNotas: (id: string, notas: string) => void;
}

const vacio: FotosServicio = { antes: [], despues: [] };

export const useServicioStore = create<ServicioState>((set) => ({
  fotos: {},
  notas: {},
  addFoto: (id, tipo, url) =>
    set((s) => {
      const actual = s.fotos[id] ?? vacio;
      return { fotos: { ...s.fotos, [id]: { ...actual, [tipo]: [...actual[tipo], url] } } };
    }),
  removeFoto: (id, tipo, url) =>
    set((s) => {
      const actual = s.fotos[id] ?? vacio;
      URL.revokeObjectURL(url);
      return {
        fotos: { ...s.fotos, [id]: { ...actual, [tipo]: actual[tipo].filter((u) => u !== url) } },
      };
    }),
  setNotas: (id, notas) => set((s) => ({ notas: { ...s.notas, [id]: notas } })),
}));

export const FOTOS_VACIAS = vacio;
