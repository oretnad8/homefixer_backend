'use client';

import { useEffect } from 'react';
import { usePathname } from 'next/navigation';
import { USE_MOCKS } from '@/lib/api';
import { distanciaKm, geolocalizacionSoportada, mensajeErrorGeo } from '@/lib/geo';
import type { Tecnico } from '@/lib/types';
import { CATEGORIA_LABELS, aCategoria } from '@/lib/utils';
import { dejarDeCompartirUbicacion, obtenerPerfilTecnico, reportarUbicacion } from '@/hooks/useTecnicos';
import { obtenerInsignias } from '@/hooks/useMaestria';
import { useAuthStore } from '@/store/authStore';
import { useUbicacionStore } from '@/store/ubicacionStore';
import { useUIStore } from '@/store/uiStore';

// ============================================================
// SesionTecnico — tareas de fondo del portal técnico (no renderiza nada)
// ============================================================
// 1. Carga el perfil real del técnico (especialidad, tarifa, disponibilidad,
//    nivel de reputación) y sus insignias de maestría.
// 2. Comparte su ubicación GPS con ms-ubicacion mientras está "Disponible" o
//    atendiendo un servicio, para que los clientes lo vean en el mapa.

/** Envío periódico aunque no se mueva (ms-ubicacion lo da por desconectado a los 120 s). */
const LATIDO_MS = 30_000;
/** Mínimo entre envíos si se está moviendo */
const MIN_INTERVALO_MS = 4_000;
/** Envío inmediato si se movió al menos esto */
const MIN_METROS = 20;
/** Envío aunque no se haya movido lo suficiente */
const MAX_INTERVALO_MS = 15_000;

export function SesionTecnico() {
  const tecnicoId = useAuthStore((s) => s.user?.id);
  const updateUser = useAuthStore((s) => s.updateUser);
  const disponible = useUIStore((s) => s.disponible);
  const setDisponible = useUIStore((s) => s.setDisponible);
  const pathname = usePathname();
  const compartir = disponible || pathname.startsWith('/tecnico/servicio-en-progreso');

  // 1. Perfil real desde ms-usuarios
  useEffect(() => {
    if (!tecnicoId || USE_MOCKS) return;
    let cancelado = false;
    obtenerPerfilTecnico(tecnicoId)
      .then((p) => {
        if (cancelado) return;
        const categoria = aCategoria(p.especialidad);
        const datos: Partial<Tecnico> = {
          especialidad: CATEGORIA_LABELS[categoria],
          categoria,
          precioBase: p.precioBase ?? 0,
          calificacion: p.calificacionPromedio ?? 0,
          validado: p.validado,
          nivelReputacion: p.nivelReputacion === 'BUENO' || p.nivelReputacion === 'EXPERTO' ? p.nivelReputacion : 'NUEVO',
        };
        updateUser(datos);
        setDisponible(p.disponible !== false);
      })
      .catch((e) => console.error('No se pudo cargar el perfil del técnico:', e));
    obtenerInsignias(tecnicoId)
      .then((lista) => {
        if (cancelado) return;
        updateUser({ insignias: lista.map((i) => i.categoria), esMaestro: lista.length > 0 } as Partial<Tecnico>);
      })
      .catch((e) => console.error('No se pudieron cargar las insignias:', e));
    return () => {
      cancelado = true;
    };
  }, [tecnicoId, updateUser, setDisponible]);

  // 2. Compartir ubicación
  useEffect(() => {
    const { setEstado, setPosicion, reset } = useUbicacionStore.getState();
    if (!tecnicoId || !compartir) {
      reset();
      return;
    }
    if (!geolocalizacionSoportada()) {
      setEstado('no-soportada', 'Tu navegador no permite compartir la ubicación.');
      return;
    }

    setEstado('solicitando');
    let ultimoEnvio: { lat: number; lng: number; t: number } | null = null;
    let ultimaLectura: GeolocationCoordinates | null = null;
    let enviando = false;

    const enviar = (c: GeolocationCoordinates, forzar = false) => {
      const ahora = Date.now();
      const punto = { lat: c.latitude, lng: c.longitude };
      if (!forzar && ultimoEnvio) {
        const transcurrido = ahora - ultimoEnvio.t;
        const metros = distanciaKm(ultimoEnvio, punto) * 1000;
        const toca = transcurrido >= MAX_INTERVALO_MS || (metros >= MIN_METROS && transcurrido >= MIN_INTERVALO_MS);
        if (!toca) return;
      }
      if (enviando) return;
      enviando = true;
      ultimoEnvio = { ...punto, t: ahora };
      reportarUbicacion(tecnicoId, punto.lat, punto.lng, c.accuracy)
        .then(() => {
          setEstado('activa');
          setPosicion({ ...punto, precision: c.accuracy, enviadaEn: ahora });
        })
        .catch(() => setEstado('error', 'No se pudo enviar tu ubicación. Reintentando…'))
        .finally(() => {
          enviando = false;
        });
    };

    const watchId = navigator.geolocation.watchPosition(
      (pos) => {
        ultimaLectura = pos.coords;
        const anterior = useUbicacionStore.getState().posicion;
        setPosicion({ lat: pos.coords.latitude, lng: pos.coords.longitude, precision: pos.coords.accuracy, enviadaEn: anterior?.enviadaEn });
        enviar(pos.coords);
      },
      (err) => {
        setEstado(err.code === err.PERMISSION_DENIED ? 'denegada' : 'error', mensajeErrorGeo(err));
      },
      { enableHighAccuracy: true, maximumAge: 10_000, timeout: 30_000 }
    );

    // Si el técnico está quieto watchPosition puede no disparar: se reenvía la última lectura
    const latido = setInterval(() => {
      if (ultimaLectura) enviar(ultimaLectura, true);
    }, LATIDO_MS);

    return () => {
      navigator.geolocation.clearWatch(watchId);
      clearInterval(latido);
      reset();
      // Al cerrar sesión ya no hay token: ms-ubicacion lo dará por desconectado al vencer el TTL
      if (useAuthStore.getState().token) void dejarDeCompartirUbicacion(tecnicoId).catch(() => undefined);
    };
  }, [tecnicoId, compartir]);

  return null;
}
