import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Permite abrir el servidor de desarrollo desde la IP de red local o Cloudflare
  allowedDevOrigins: [
    "172.28.144.1",
    "homefixer.localfoto.cl",
    "localhost"
  ],

  // Solo en desarrollo: Cloudflare reescribe el "no-cache" de los chunks de Next a
  // "max-age=14400" y, como en dev los chunks no cambian de nombre, el navegador seguía
  // ejecutando código viejo hasta 4 h. Cada página le pide al navegador vaciar su caché HTTP.
  async headers() {
    if (process.env.NODE_ENV === "production") return [];
    return [
      {
        source: "/((?!_next/).*)",
        headers: [{ key: "Clear-Site-Data", value: '"cache"' }],
      },
    ];
  },
};

export default nextConfig;
