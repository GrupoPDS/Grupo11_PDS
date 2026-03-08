import { useEffect, useState, useCallback } from 'react';
import { BookOpen, Clock, CheckCircle2, AlertCircle, BarChart3 } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { usePageTitle } from '../hooks/usePageTitle';
import { api } from '../services/api';
import { Badge } from '../components/Badge';
import { Toast, type ToastType } from '../components/Toast';

/* ── Tipos ── */

interface LoanHistoryItem {
  id: number;
  bookTitle: string;
  bookAuthor: string;
  bookIsbn: string;
  loanDate: string;
  dueDate: string;
  returnDate: string | null;
  status: string;
  durationDays: number;
  returnedOnTime: boolean | null;
  copyCode: string | null;
}

interface LoanSummary {
  totalLoans: number;
  activeLoans: number;
  returnedLoans: number;
  overdueLoans: number;
  onTimeReturnRate: number;
}

interface PagedResult {
  content: LoanHistoryItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

interface ReservationResponse {
  id: number;
  bookId: number;
  bookTitle: string;
  reservationDate: string;
  status: string;
  queuePosition: number | null;
  expiresAt: string | null;
}

type StatusFilter = 'ALL' | 'ACTIVE' | 'RETURNED' | 'OVERDUE';

/* ── Componente ── */

export default function MyLoansPage() {
  usePageTitle('Meus Empréstimos');
  const { user } = useAuth();

  /* Estado: Histórico paginado */
  const [pagedData, setPagedData] = useState<PagedResult | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [loadingHistory, setLoadingHistory] = useState(false);

  /* Estado: Resumo */
  const [summary, setSummary] = useState<LoanSummary | null>(null);

  /* Estado: Reservas */
  const [reservations, setReservations] = useState<ReservationResponse[]>([]);

  const [error, setError] = useState<string | null>(null);
  const [toast, setToast] = useState<{ message: string; type: ToastType; id: string } | null>(null);

  const PAGE_SIZE = 6;

  const showToast = (message: string, type: ToastType) => {
    const id = Math.random().toString(36).substr(2, 9);
    setToast({ message, type, id });
    setTimeout(() => setToast(null), 4000);
  };

  /* ── Fetch: Histórico paginado ── */
  const fetchHistory = useCallback(
    async (page: number, status: StatusFilter) => {
      if (!user?.id) return;
      setLoadingHistory(true);
      setError(null);
      try {
        const params = new URLSearchParams();
        params.set('page', String(page));
        params.set('size', String(PAGE_SIZE));
        if (status !== 'ALL') {
          params.set('status', status);
        }
        const res = await api(`/loans/my-history?${params.toString()}`);
        if (!res.ok) throw new Error('Falha ao carregar histórico');
        const data: PagedResult = await res.json();
        setPagedData(data);
      } catch (err) {
        console.error(err);
        setError('Erro ao carregar o seu histórico de leituras.');
      } finally {
        setLoadingHistory(false);
      }
    },
    [user?.id],
  );

  /* ── Fetch: Resumo ── */
  const fetchSummary = useCallback(async () => {
    if (!user?.id) return;
    try {
      const res = await api('/loans/my-summary');
      if (res.ok) {
        setSummary(await res.json());
      }
    } catch (err) {
      console.error('Falha ao obter resumo:', err);
    }
  }, [user?.id]);

  /* ── Fetch: Reservas ── */
  const fetchReservations = useCallback(async () => {
    if (!user?.id) return;
    try {
      const res = await api(`/reservations/user/${user.id}`);
      if (res.ok) {
        setReservations(await res.json());
      }
    } catch (err) {
      console.error('Falha ao obter reservas:', err);
    }
  }, [user?.id]);

  /* ── Effects ── */
  useEffect(() => {
    fetchSummary();
    fetchReservations();
  }, [fetchSummary, fetchReservations]);

  useEffect(() => {
    fetchHistory(currentPage, statusFilter);
  }, [fetchHistory, currentPage, statusFilter]);

  const [cancelingReservationId, setCancelingReservationId] = useState<number | null>(null);
  const [borrowingFromReservation, setBorrowingFromReservation] = useState<number | null>(null);

  /* ── Handlers ── */
  const handleBorrowFromReservation = async (bookId: number, reservationId: number) => {
    if (borrowingFromReservation !== null) return;
    setBorrowingFromReservation(reservationId);
    try {
      const res = await api('/loans/borrow', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ bookId }),
      });
      if (res.status === 201) {
        setError(null);
        showToast('Empréstimo confirmado com sucesso! Prazo: 14 dias.', 'success');
        fetchReservations();
        fetchSummary();
        fetchHistory(currentPage, statusFilter);
      } else {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.error || 'Falha ao registrar empréstimo.');
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Erro ao emprestar.';
      setError(message);
    } finally {
      setBorrowingFromReservation(null);
    }
  };

  const handleFilterChange = (filter: StatusFilter) => {
    setStatusFilter(filter);
    setCurrentPage(0);
  };

  const handleCancelReservation = async (reservationId: number) => {
    if (cancelingReservationId !== null) return;
    if (!confirm('Tem certeza que deseja cancelar esta reserva?')) return;
    setCancelingReservationId(reservationId);
    try {
      const res = await api(`/reservations/${reservationId}`, { method: 'DELETE' });
      if (!res.ok) {
        const body = await res.json().catch(() => null);
        throw new Error(body?.error || 'Erro ao cancelar reserva');
      }
      showToast('Reserva cancelada com sucesso.', 'success');
      fetchReservations();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Erro ao cancelar reserva.';
      setError(message);
    } finally {
      setCancelingReservationId(null);
    }
  };

  const formatRate = (rate: number) => `${Math.round(rate * 100)}%`;

  const summaryCards = summary
    ? [
        {
          icon: <BookOpen />,
          label: 'Total',
          val: String(summary.totalLoans),
          color: 'text-slate-800',
        },
        {
          icon: <Clock />,
          label: 'Em andamento',
          val: String(summary.activeLoans),
          color: 'text-blue-600',
        },
        {
          icon: <CheckCircle2 />,
          label: 'Devolvidos',
          val: String(summary.returnedLoans),
          color: 'text-emerald-600',
        },
        {
          icon: <AlertCircle />,
          label: 'Em atraso',
          val: String(summary.overdueLoans),
          color: 'text-red-600',
          border:
            summary.overdueLoans === 0
              ? 'border-l-4 border-l-emerald-500'
              : 'border-l-4 border-l-red-500',
        },
        {
          icon: <BarChart3 />,
          label: 'No prazo (%)',
          val: formatRate(summary.onTimeReturnRate),
          color: 'text-slate-800',
        },
      ]
    : [];

  const filterLabels: Record<StatusFilter, string> = {
    ALL: 'Todos',
    ACTIVE: 'Ativos',
    RETURNED: 'Devolvidos',
    OVERDUE: 'Atrasados',
  };

  const activeReservations = reservations.filter(
    (r) => r.status === 'PENDING' || r.status === 'AVAILABLE_FOR_PICKUP',
  );

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-slate-800">Meus Empréstimos</h1>
        <p className="text-sm text-slate-500">Acompanhe seu histórico de leituras e reservas</p>
      </div>

      {error && (
        <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded-md text-sm font-medium">
          {error}
        </div>
      )}

      {/* ── Summary Cards ── */}
      {summary && (
        <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
          {summaryCards.map((card, i) => (
            <div
              key={i}
              className={`bg-white p-4 rounded-lg border border-slate-200 shadow-sm ${card.border || ''}`}
            >
              <div className="flex justify-between items-start mb-2">
                <div className="text-slate-400 [&>svg]:w-5 [&>svg]:h-5">{card.icon}</div>
              </div>
              <h3 className={`text-2xl font-bold ${card.color}`}>{card.val}</h3>
              <p className="text-xs font-medium text-slate-500 uppercase tracking-wider mt-1 m-0">
                {card.label}
              </p>
            </div>
          ))}
        </div>
      )}

      {/* Histórico e Fila Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Tabela de Histórico (Span 2) */}
        <div className="lg:col-span-2 space-y-4">
          <div className="flex justify-between items-end">
            <h2 className="text-lg font-bold text-slate-800 m-0">Histórico de Leituras</h2>
            <div className="flex gap-2">
              {(['ALL', 'ACTIVE', 'RETURNED', 'OVERDUE'] as StatusFilter[]).map((f) => (
                <button
                  key={f}
                  onClick={() => handleFilterChange(f)}
                  className={`px-3 py-1 text-xs rounded-full cursor-pointer transition-colors ${
                    statusFilter === f
                      ? 'bg-slate-800 text-white'
                      : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
                  }`}
                >
                  {filterLabels[f]}
                </button>
              ))}
            </div>
          </div>

          {loadingHistory ? (
            <p className="text-slate-500 text-center py-8">Carregando histórico...</p>
          ) : pagedData && pagedData.content.length > 0 ? (
            <div className="bg-white border border-slate-200 rounded-lg shadow-sm overflow-hidden">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 text-xs uppercase text-slate-500 font-semibold border-b border-slate-200">
                  <tr>
                    <th className="px-4 py-3">Livro</th>
                    <th className="px-4 py-3">Prazos</th>
                    <th className="px-4 py-3">Status</th>
                  </tr>
                </thead>
                <tbody>
                  {pagedData.content.map((loan) => (
                    <tr key={loan.id} className="border-b border-slate-100">
                      <td className="px-4 py-3">
                        <p className="font-semibold text-slate-800 m-0">{loan.bookTitle}</p>
                        <p className="text-xs text-slate-500 m-0">
                          {loan.bookAuthor} · ISBN: {loan.bookIsbn}
                        </p>
                        {loan.copyCode && (
                          <span className="inline-block mt-1 px-1.5 py-0.5 bg-slate-100 text-slate-600 text-[10px] rounded font-mono">
                            Exemplar: {loan.copyCode}
                          </span>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        <p className="text-slate-600 m-0">
                          Retirada: {new Date(loan.loanDate).toLocaleDateString()}
                        </p>
                        <p
                          className={`font-medium m-0 ${
                            loan.status === 'OVERDUE' ? 'text-red-600' : 'text-slate-800'
                          }`}
                        >
                          Prazo: {new Date(loan.dueDate).toLocaleDateString()}
                        </p>
                        {loan.returnDate && (
                          <p className="text-xs text-slate-500 m-0">
                            Devolvido: {new Date(loan.returnDate).toLocaleDateString()}
                            {loan.returnedOnTime === false && ' (atrasado)'}
                          </p>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        <Badge status={loan.status} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {pagedData.totalPages > 1 && (
                <div className="px-4 py-3 bg-slate-50 text-xs text-slate-500 flex justify-between items-center border-t border-slate-200">
                  <button
                    disabled={currentPage === 0}
                    onClick={() => setCurrentPage((p) => p - 1)}
                    className="hover:text-slate-800 cursor-pointer disabled:opacity-40"
                  >
                    ← Anterior
                  </button>
                  <span>
                    Página {currentPage + 1} de {pagedData.totalPages}
                  </span>
                  <button
                    disabled={!pagedData.hasNext}
                    onClick={() => setCurrentPage((p) => p + 1)}
                    className="hover:text-slate-800 cursor-pointer disabled:opacity-40"
                  >
                    Próximo →
                  </button>
                </div>
              )}
            </div>
          ) : (
            <p className="text-slate-400 italic py-4">
              {statusFilter === 'ALL'
                ? 'Você ainda não realizou nenhum empréstimo. Explore o catálogo!'
                : 'Nenhum empréstimo encontrado com esse filtro.'}
            </p>
          )}
        </div>

        {/* Fila de Espera (Span 1) */}
        <div className="space-y-4">
          <h2 className="text-lg font-bold text-slate-800 m-0">Fila de Espera (Reservas)</h2>

          {activeReservations.length === 0 ? (
            <p className="text-slate-400 italic">Você não possui nenhuma reserva na fila.</p>
          ) : (
            <div className="space-y-4">
              {activeReservations.map((res) => (
                <div
                  key={res.id}
                  className="bg-white border border-slate-200 rounded-lg shadow-sm p-4 relative overflow-hidden"
                >
                  <div className="absolute top-0 left-0 w-1 h-full bg-yellow-400"></div>
                  <div className="flex justify-between items-start mb-2">
                    {res.status === 'PENDING' && <Badge status="PENDING" />}
                    {res.status === 'AVAILABLE_FOR_PICKUP' && (
                      <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border bg-blue-50 text-blue-700 border-blue-200">
                        Disponível para Retirada!
                      </span>
                    )}
                    <span className="text-xs text-slate-400">
                      {new Date(res.reservationDate).toLocaleDateString()}
                    </span>
                  </div>
                  <h3 className="font-bold text-slate-800 text-sm mb-1">{res.bookTitle}</h3>

                  {/* Posição na fila */}
                  {res.status === 'PENDING' && res.queuePosition != null && (
                    <div className="bg-yellow-50 p-2 rounded text-xs text-yellow-800 font-medium mb-4 flex items-center gap-2 mt-3">
                      <Clock className="w-4 h-4" /> Posição na fila: {res.queuePosition}º
                    </div>
                  )}

                  {/* Retirada: botão de empréstimo */}
                  {res.status === 'AVAILABLE_FOR_PICKUP' && (
                    <div className="mt-3 space-y-2">
                      <p className="text-xs text-blue-700 font-bold m-0">
                        Sua vez chegou! Confirme o empréstimo abaixo:
                      </p>
                      {res.expiresAt && (
                        <p className="text-xs text-red-600 m-0">
                          Confirme até: {new Date(res.expiresAt).toLocaleString('pt-BR')}
                        </p>
                      )}
                      <button
                        onClick={() => handleBorrowFromReservation(res.bookId, res.id)}
                        disabled={borrowingFromReservation !== null}
                        className="w-full py-2 rounded-md text-sm font-semibold flex justify-center items-center gap-2 transition-colors cursor-pointer bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        <BookOpen className="w-4 h-4" />
                        {borrowingFromReservation === res.id
                          ? 'Processando...'
                          : 'Confirmar Empréstimo'}
                      </button>
                    </div>
                  )}

                  {/* Botão cancelar */}
                  {(res.status === 'PENDING' || res.status === 'AVAILABLE_FOR_PICKUP') && (
                    <button
                      onClick={() => handleCancelReservation(res.id)}
                      disabled={cancelingReservationId !== null}
                      className="w-full mt-4 py-1.5 text-xs font-semibold text-red-600 bg-red-50 hover:bg-red-100 rounded transition-colors border border-red-100 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {cancelingReservationId === res.id ? 'Cancelando...' : 'Cancelar Reserva'}
                    </button>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {toast && <Toast message={toast.message} type={toast.type} />}
    </div>
  );
}
