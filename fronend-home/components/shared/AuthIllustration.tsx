import { Hammer, MapPin, Star, Wrench, Zap } from 'lucide-react';

/** Ilustración del panel de marca en el login (solo desktop) */
export function AuthIllustration({ variante }: { variante: 'cliente' | 'tecnico' }) {
  return (
    <div aria-hidden className="relative h-56 w-full max-w-sm">
      <div className="absolute inset-x-6 top-4 rounded-card bg-white/15 p-5 ring-1 ring-white/25 backdrop-blur">
        <div className="flex items-center gap-3">
          <span className="flex size-12 items-center justify-center rounded-pill bg-white text-primary-600">
            {variante === 'cliente' ? <Wrench className="size-6" /> : <Hammer className="size-6" />}
          </span>
          <div className="flex-1 space-y-2">
            <div className="h-2.5 w-2/3 rounded-pill bg-white/70" />
            <div className="h-2 w-1/2 rounded-pill bg-white/40" />
          </div>
        </div>
        <div className="mt-4 flex gap-1">
          {[0, 1, 2, 3, 4].map((i) => (
            <Star key={i} className="size-4 fill-gold-light text-gold-light" />
          ))}
        </div>
      </div>
      <div className="absolute bottom-6 left-0 flex items-center gap-2 rounded-pill bg-white px-4 py-2 text-sm font-semibold text-primary-700 shadow-card-lg">
        <MapPin className="size-4" />
        {variante === 'cliente' ? 'Técnico a 1.2 km' : 'Nueva solicitud cerca'}
      </div>
      <div className="absolute bottom-0 right-2 flex items-center gap-2 rounded-pill bg-success px-4 py-2 text-sm font-semibold text-white shadow-card-lg">
        <Zap className="size-4" />
        {variante === 'cliente' ? 'Llega en 15 min' : '+$850 hoy'}
      </div>
    </div>
  );
}
