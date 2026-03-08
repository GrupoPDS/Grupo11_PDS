export type ToastType = 'success' | 'error' | 'info';

interface ToastProps {
  message: string;
  type: ToastType;
  onClose?: () => void;
}

const typeStyles: Record<ToastType, string> = {
  success: 'bg-emerald-600 text-white',
  error: 'bg-red-600 text-white',
  info: 'bg-[#003399] text-white',
};

export function Toast({ message, type }: ToastProps) {
  const icons = {
    success: '',
    error: '',
    info: '',
  };

  return (
    <div
      className={`fixed bottom-6 right-6 z-50 flex items-center gap-3 px-5 py-3 rounded-lg shadow-lg animate-[slideUp_0.3s_ease] ${typeStyles[type]}`}
      role="alert"
    >
      <span className="text-lg font-bold">{icons[type]}</span>
      <span className="text-sm font-medium">{message}</span>
    </div>
  );
}
