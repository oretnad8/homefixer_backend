import type {
  CategoriaServicio,
  EstadoPago,
  EstadoSolicitud,
  Mensaje,
  MetodoPago,
  NivelUrgencia,
  Notificacion,
  Pago,
  Solicitud,
  Tecnico,
  Valoracion,
} from './types';

// ============================================================
// Mapeo de las respuestas de los microservicios a los tipos del front
// ============================================================

const CATEGORIAS: CategoriaServicio[] = [
  'plomeria', 'electricidad', 'carpinteria', 'pintura', 'cerrajeria', 'limpieza', 'jardineria', 'refrigeracion', 'otros',
];
const URGENCIAS: NivelUrgencia[] = ['normal', 'urgente', 'emergencia'];

const ESTADOS_SOLICITUD: Record<string, EstadoSolicitud> = {
  CREADA: 'pendiente',
  BUSCANDO_TECNICO: 'pendiente',
  ACEPTADA: 'aceptada',
  EN_PROCESO: 'en_progreso',
  COMPLETADA: 'completada',
  PAGADA: 'pagada',
  CANCELADA: 'cancelada',
};

const ESTADOS_PAGO: Record<string, EstadoPago> = {
  PENDIENTE: 'pendiente',
  PROCESADO: 'completado',
  RECHAZADO: 'fallido',
};

/** Solicitud tal como la devuelve ms-solicitudes */
export interface SolicitudApi {
  id: number;
  clienteId: number;
  tecnicoId?: number | null;
  descripcion: string;
  estadoSolicitud: string;
  precioEstimado?: number | null;
  fecha: string;
  fechaActualizacion?: string | null;
  coordenadas?: string | null;
  categoria?: string | null;
  urgencia?: string | null;
  direccion?: string | null;
  clienteNombre?: string | null;
  tecnicoNombre?: string | null;
  tecnicoTelefono?: string | null;
  tecnicoEspecialidad?: string | null;
  tecnicoCalificacion?: number | null;
}

export interface PagoApi {
  id: number;
  solicitudId: number;
  clienteId?: number | null;
  tecnicoId?: number | null;
  monto: number;
  estadoPago: string;
  metodoPago: string;
  descripcion?: string | null;
  fecha?: string | null;
}

export interface MensajeApi {
  id: number;
  solicitudId: number;
  remitenteId: number;
  remitenteNombre: string;
  esCliente: boolean;
  texto: string;
  creadoEn: string;
}

export interface NotificacionApi {
  id: number;
  titulo: string;
  detalle: string;
  creadoEn: string;
  leida: boolean;
}

export interface ValoracionApi {
  id: number;
  solicitudId: number;
  clienteId: number;
  tecnicoId: number;
  puntuacion: number;
  comentario?: string | null;
  fecha?: string | null;
}

const str = (v: number | null | undefined) => (v === null || v === undefined ? undefined : String(v));

function coordenadas(valor?: string | null): { latitud?: number; longitud?: number } {
  const partes = valor?.split(',').map((p) => parseFloat(p.trim()));
  if (partes?.length === 2 && partes.every((n) => !isNaN(n))) return { latitud: partes[0], longitud: partes[1] };
  return {};
}

export function mapSolicitud(s: SolicitudApi): Solicitud {
  const categoria = CATEGORIAS.includes(s.categoria as CategoriaServicio) ? (s.categoria as CategoriaServicio) : 'otros';
  const urgencia = URGENCIAS.includes(s.urgencia as NivelUrgencia) ? (s.urgencia as NivelUrgencia) : 'normal';

  const tecnico: Tecnico | undefined = s.tecnicoId
    ? {
        id: String(s.tecnicoId),
        nombre: s.tecnicoNombre ?? `Técnico #${s.tecnicoId}`,
        apellido: '',
        email: '',
        telefono: s.tecnicoTelefono ?? undefined,
        rol: 'tecnico',
        creadoEn: s.fecha,
        especialidad: s.tecnicoEspecialidad ?? categoria,
        calificacion: s.tecnicoCalificacion ?? 0,
        totalValoraciones: 0,
        disponible: true,
        precioBase: s.precioEstimado ?? 0,
        trabajosCompletados: 0,
      }
    : undefined;

  return {
    id: String(s.id),
    clienteId: String(s.clienteId),
    clienteNombre: s.clienteNombre ?? `Cliente #${s.clienteId}`,
    tecnicoId: str(s.tecnicoId),
    tecnico,
    categoria,
    urgencia,
    descripcion: s.descripcion,
    // Filas antiguas solo tienen las coordenadas
    direccion: s.direccion ?? s.coordenadas ?? 'Ubicación no especificada',
    ...coordenadas(s.coordenadas),
    estado: ESTADOS_SOLICITUD[s.estadoSolicitud] ?? 'pendiente',
    presupuestoEstimado: s.precioEstimado || undefined,
    creadoEn: s.fecha,
    actualizadoEn: s.fechaActualizacion ?? s.fecha,
  };
}

export function mapPago(p: PagoApi): Pago {
  return {
    id: String(p.id),
    solicitudId: String(p.solicitudId),
    clienteId: str(p.clienteId) ?? '',
    tecnicoId: str(p.tecnicoId) ?? '',
    monto: p.monto,
    metodoPago: (['tarjeta', 'efectivo', 'transferencia', 'paypal'].includes(p.metodoPago) ? p.metodoPago : 'tarjeta') as MetodoPago,
    estado: ESTADOS_PAGO[p.estadoPago] ?? 'pendiente',
    descripcion: p.descripcion ?? `Servicio · solicitud #${p.solicitudId}`,
    creadoEn: p.fecha ?? new Date().toISOString(),
  };
}

export function mapMensaje(m: MensajeApi): Mensaje {
  return { ...m, id: String(m.id), solicitudId: String(m.solicitudId), remitenteId: String(m.remitenteId) };
}

export function mapNotificacion(n: NotificacionApi): Notificacion {
  return { ...n, id: String(n.id) };
}

export function mapValoracion(v: ValoracionApi): Valoracion {
  return {
    id: String(v.id),
    solicitudId: String(v.solicitudId),
    clienteId: String(v.clienteId),
    tecnicoId: String(v.tecnicoId),
    calificacion: v.puntuacion,
    comentario: v.comentario ?? undefined,
    creadoEn: v.fecha ?? new Date().toISOString(),
  };
}
