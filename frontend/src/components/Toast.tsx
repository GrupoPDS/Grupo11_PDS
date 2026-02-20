import './Toast.css';

export type ToastType = 'success' | 'error' | 'info';

interface ToastProps {
  message: string;
  type: ToastType;
  onClose?: () => void;
}

/**
 * Componente Toast para exibir notificações ao usuário
 * - success: Verde (operação bem-sucedida)
 * - error: Vermelho (erro na operação)
 * - info: Azul (informação)
 */
export function Toast({ message, type }: ToastProps) {
  const icons = {
    success: '✓',
    error: '✕',
    info: 'ℹ',
  };

  return (
    <div className={`toast toast-${type}`} role="alert">
      <span className="toast-icon">{icons[type]}</span>
      <span className="toast-message">{message}</span>
    </div>
  );
}
