import type { UserRole } from '@/lib/types';
import { NavContent } from './NavContent';

/** Sidebar fijo a la izquierda, solo visible desde lg (≥1024px) */
export function Sidebar({ rol }: { rol: UserRole }) {
  return (
    <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 border-r border-border bg-surface lg:block">
      <NavContent rol={rol} />
    </aside>
  );
}
