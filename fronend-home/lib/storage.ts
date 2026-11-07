// ============================================================
// Supabase Storage (API REST) — documentos de certificaciones
// ============================================================
//   POST /storage/v1/object/:bucket/:ruta   (token del usuario) → sube el archivo
//   GET  /storage/v1/object/public/:bucket/:ruta                → URL pública
// El bucket debe existir y ser público, con una política que permita INSERT
// a usuarios autenticados (ver README / instrucciones de configuración).

const SUPABASE_URL = process.env.NEXT_PUBLIC_SUPABASE_URL as string;
const SUPABASE_KEY = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY as string;

export const BUCKET_CERTIFICACIONES = process.env.NEXT_PUBLIC_SUPABASE_BUCKET_CERTIFICACIONES || 'certificaciones';

/** Tipos y tamaño admitidos para los documentos */
export const TIPOS_DOCUMENTO = ['application/pdf', 'image/jpeg', 'image/png', 'image/webp'];
export const MAX_DOCUMENTO_MB = 10;

/** Sube `archivo` a `bucket/ruta` y devuelve su URL pública. */
export async function subirArchivo(bucket: string, ruta: string, archivo: File, token: string): Promise<string> {
  const res = await fetch(`${SUPABASE_URL}/storage/v1/object/${bucket}/${ruta}`, {
    method: 'POST',
    headers: {
      apikey: SUPABASE_KEY,
      Authorization: `Bearer ${token}`,
      'Content-Type': archivo.type || 'application/octet-stream',
      'x-upsert': 'false',
    },
    body: archivo,
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    const detalle = body.message ?? body.error ?? res.statusText;
    throw new Error(
      res.status === 404 || /bucket/i.test(String(detalle))
        ? `No existe el bucket "${bucket}" en Supabase Storage.`
        : `No se pudo subir el documento: ${detalle}`
    );
  }
  return `${SUPABASE_URL}/storage/v1/object/public/${bucket}/${ruta}`;
}
