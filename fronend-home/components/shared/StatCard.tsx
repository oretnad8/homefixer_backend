import type { ReactNode } from 'react';
import { Card } from '@/components/ui/Card';
import { cn } from '@/lib/utils';

interface StatCardProps {
  label: string;
  value: ReactNode;
  icon: ReactNode;
  tone?: 'primary' | 'success' | 'gold' | 'warning';
  hint?: string;
}

const tones = {
  primary: 'bg-primary-50 text-primary-600',
  success: 'bg-success-light text-success',
  gold: 'bg-gold-light text-gold',
  warning: 'bg-warning-light text-warning',
};

export function StatCard({ label, value, icon, tone = 'primary', hint }: StatCardProps) {
  return (
    <Card className="flex items-center gap-4 md:flex-col md:items-start">
      <span className={cn('flex size-12 shrink-0 items-center justify-center rounded-card [&>svg]:size-6', tones[tone])} aria-hidden>
        {icon}
      </span>
      <div className="min-w-0">
        <p className="text-sm text-subtle">{label}</p>
        <p className="text-2xl font-bold text-ink">{value}</p>
        {hint && <p className="text-xs text-subtle">{hint}</p>}
      </div>
    </Card>
  );
}
