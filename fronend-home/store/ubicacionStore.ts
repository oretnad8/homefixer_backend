'use client';

import { create } from 'zustand';

// ============================================================
// STORE DE UBICACIÓN DEL TÉCNICO
// ============================================================
// Lo alimenta components/layout/CompartirUbicacion (watchPosition del navegador).

export type EstadoUbicacion =
  /** No se está compartiendo (técnico no disponible) */
  | 'inactiva'
  /** Esperando permiso / primera lectura del GPS */
  | 'solicitando'
  /** Compartiendo: los clientes cercanos te ven en el mapa */
  | 'activa'
  | 'denegada'
  | 'no-soportada'
  | 'error';

export interface PosicionTecnico {
  lat: number;
  lng: number;
  precision: number;
  /** Último envío exitoso a ms-ubicacion (epoch ms) */
  enviadaEn?: number;
}

interface UbicacionState {
  estado: EstadoUbicacion;
  posicion: PosicionTecnico | null;
  mensaje: string | null;
  setEstado: (estado: EstadoUbicacion, mensaje?: string | null) => void;
  setPosicion: (posicion: PosicionTecnico) => void;
  reset: () => void;
}

export const useUbicacionStore = create<UbicacionState>((set) => ({
  estado: 'inactiva',
  posicion: null,
  mensaje: null,
  setEstado: (estado, mensaje = null) => set({ estado, mensaje }),
  setPosicion: (posicion) => set({ posicion }),
  reset: () => set({ estado: 'inactiva', posicion: null, mensaje: null }),
}));
