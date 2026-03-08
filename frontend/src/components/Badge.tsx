import { Clock, XCircle, CheckCircle2 } from 'lucide-react';

type BadgeStatus = 'ACTIVE' | 'OVERDUE' | 'RETURNED' | 'PENDING';

interface BadgeProps {
  status: string;
}

const styles: Record<BadgeStatus, string> = {
  ACTIVE: 'bg-blue-50 text-blue-700 border-blue-200',
  OVERDUE: 'bg-red-50 text-red-700 border-red-200',
  RETURNED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  PENDING: 'bg-yellow-50 text-yellow-700 border-yellow-200',
};

const labels: Record<BadgeStatus, string> = {
  ACTIVE: 'Em curso',
  OVERDUE: 'Atrasado',
  RETURNED: 'Devolvido',
  PENDING: 'Aguardando',
};

const icons: Record<BadgeStatus, React.ReactNode> = {
  ACTIVE: <Clock className="w-3.5 h-3.5" />,
  OVERDUE: <XCircle className="w-3.5 h-3.5" />,
  RETURNED: <CheckCircle2 className="w-3.5 h-3.5" />,
  PENDING: <Clock className="w-3.5 h-3.5" />,
};

export function Badge({ status }: BadgeProps) {
  const key = status as BadgeStatus;
  const style = styles[key] || 'bg-slate-50 text-slate-700 border-slate-200';
  const label = labels[key] || status;
  const icon = icons[key] || null;

  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border ${style}`}
    >
      {icon} {label}
    </span>
  );
}
