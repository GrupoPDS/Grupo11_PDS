import { useEffect, useState } from 'react';
import { Search, Download } from 'lucide-react';
import { api } from '../services/api';
import { useDebounce } from '../hooks/useDebounce';
import { usePageTitle } from '../hooks/usePageTitle';
import { Badge } from '../components/Badge';

export interface LoanResponse {
  id: number;
  loanDate: string;
  dueDate: string;
  returnDate: string | null;
  status: string;
  userId: number;
  userName: string;
  bookId: number;
  bookTitle: string;
  bookIsbn: string;
  copyCode: string | null;
}

export default function ReturnsPage() {
  usePageTitle('Devoluções');
  const [loans, setLoans] = useState<LoanResponse[]>([]);
  const [searchInput, setSearchInput] = useState('');
  const [activeTab, setActiveTab] = useState<'ALL' | 'OVERDUE'>('ALL');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Modal de confirmação
  const [confirmModal, setConfirmModal] = useState<{
    loanId: number;
    bookTitle: string;
    userName: string;
  } | null>(null);
  const [returning, setReturning] = useState(false);

  const debouncedSearch = useDebounce(searchInput, 300);

  const fetchActiveLoans = async () => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      if (debouncedSearch) params.set('search', debouncedSearch);
      if (activeTab === 'OVERDUE') params.set('status', 'OVERDUE');
      const qs = params.toString();
      const url = qs ? `/loans?${qs}` : '/loans';
      const res = await api(url);
      if (!res.ok) throw new Error('Falha ao buscar empréstimos');
      const data = await res.json();
      setLoans(data);
    } catch (err) {
      setError('Erro ao carregar os empréstimos ativos.');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchActiveLoans();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedSearch, activeTab]);

  const openReturnModal = (loan: LoanResponse) => {
    setConfirmModal({ loanId: loan.id, bookTitle: loan.bookTitle, userName: loan.userName });
  };

  const handleConfirmReturn = async () => {
    if (!confirmModal) return;
    setReturning(true);
    try {
      const res = await api(`/loans/${confirmModal.loanId}/return`, { method: 'PATCH' });
      if (res.ok) {
        setSuccessMsg('Devolução registrada com sucesso!');
        setTimeout(() => setSuccessMsg(null), 3000);
        fetchActiveLoans();
      } else {
        const errData = await res.json().catch(() => ({}));
        setError(errData.error || 'Erro ao registrar devolução.');
        setTimeout(() => setError(null), 3000);
      }
    } catch (err) {
      setError('Falha de conexão ao registrar devolução.');
      console.error(err);
    } finally {
      setReturning(false);
      setConfirmModal(null);
    }
  };

  const handleExportCSV = () => {
    const overdueLoans = loans.filter((l) => l.status === 'OVERDUE');
    if (overdueLoans.length === 0) return;

    let csvContent = 'Leitor,Email,Livro,ISBN,Data Emprestimo,Data Vencimento\n';
    overdueLoans.forEach((l) => {
      csvContent += `"${l.userName}","N/A","${l.bookTitle}","${l.bookIsbn}","${l.loanDate}","${l.dueDate}"\n`;
    });

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute(
      'download',
      `relatorio_atrasos_${new Date().toISOString().split('T')[0]}.csv`,
    );
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-800">Controle de Devoluções e Atrasos</h1>
        <p className="text-sm text-slate-500">Gestão operacional de devoluções de exemplares</p>
      </div>

      {successMsg && (
        <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-700 rounded-md text-sm font-medium">
          {successMsg}
        </div>
      )}
      {error && (
        <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded-md text-sm font-medium">
          {error}
        </div>
      )}

      <div className="bg-white rounded-lg border border-slate-200 shadow-sm overflow-hidden">
        {/* Toolbar & Tabs */}
        <div className="border-b border-slate-200 px-6 py-4 flex flex-col md:flex-row justify-between items-center gap-4 bg-slate-50/50">
          <div className="flex gap-2 p-1 bg-slate-200/50 rounded-md">
            <button
              onClick={() => setActiveTab('ALL')}
              className={`px-4 py-1.5 rounded text-sm font-medium transition-all cursor-pointer ${
                activeTab === 'ALL'
                  ? 'bg-white text-slate-800 shadow-sm'
                  : 'text-slate-600 hover:text-slate-800'
              }`}
            >
              Todos em Andamento
            </button>
            <button
              onClick={() => setActiveTab('OVERDUE')}
              className={`px-4 py-1.5 rounded text-sm font-medium transition-all cursor-pointer ${
                activeTab === 'OVERDUE'
                  ? 'bg-white text-red-600 shadow-sm'
                  : 'text-slate-600 hover:text-slate-800'
              }`}
            >
              Relatório de Atrasos
            </button>
          </div>

          <div className="flex gap-3 w-full md:w-auto">
            <div className="relative w-full md:w-64">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
              <input
                type="text"
                placeholder="Buscar Email ou ISBN..."
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                className="w-full pl-9 pr-3 py-2 bg-white border border-slate-200 rounded-md text-sm outline-none focus:border-[#003399]"
              />
            </div>
            {activeTab === 'OVERDUE' && loans.length > 0 && (
              <button
                onClick={handleExportCSV}
                className="flex items-center gap-2 px-3 py-2 bg-emerald-50 text-emerald-700 border border-emerald-200 rounded-md text-sm font-medium hover:bg-emerald-100 cursor-pointer"
              >
                <Download className="w-4 h-4" /> Exportar CSV
              </button>
            )}
          </div>
        </div>

        {/* Table */}
        {loading ? (
          <p className="text-slate-500 text-center py-8">Carregando...</p>
        ) : (
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-slate-50 text-xs uppercase tracking-wider text-slate-500 font-semibold border-b border-slate-200">
                <th className="px-6 py-4">Leitor</th>
                <th className="px-6 py-4">Livro (ISBN)</th>
                <th className="px-6 py-4">Empréstimo</th>
                <th className="px-6 py-4">Prazo</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4 text-right">Ação</th>
              </tr>
            </thead>
            <tbody className="text-sm">
              {loans.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-6 py-8 text-center text-slate-400">
                    {activeTab === 'OVERDUE'
                      ? 'Nenhum empréstimo em atraso.'
                      : 'Nenhum empréstimo encontrado.'}
                  </td>
                </tr>
              ) : (
                loans.map((loan) => {
                  const isOverdue =
                    loan.status === 'OVERDUE' ||
                    (new Date(loan.dueDate) < new Date() && loan.status !== 'RETURNED');
                  return (
                    <tr
                      key={loan.id}
                      className={`border-b border-slate-100 ${
                        isOverdue ? 'bg-red-50/30' : 'hover:bg-slate-50'
                      }`}
                    >
                      <td className="px-6 py-4">
                        <p className="font-semibold text-slate-800 m-0">{loan.userName}</p>
                      </td>
                      <td className="px-6 py-4">
                        <p className="font-medium text-slate-800 m-0">{loan.bookTitle}</p>
                        <p className="text-xs text-slate-500 m-0">{loan.bookIsbn}</p>
                        {loan.copyCode && (
                          <span className="inline-block mt-1 px-1.5 py-0.5 bg-slate-100 text-slate-600 text-[10px] rounded font-mono">
                            Exemplar: {loan.copyCode}
                          </span>
                        )}
                      </td>
                      <td className="px-6 py-4 text-slate-600">
                        {new Date(loan.loanDate).toLocaleDateString()}
                      </td>
                      <td className="px-6 py-4 font-medium text-slate-800">
                        {new Date(loan.dueDate).toLocaleDateString()}
                      </td>
                      <td className="px-6 py-4">
                        <Badge status={loan.status} />
                      </td>
                      <td className="px-6 py-4 text-right">
                        <button
                          onClick={() => openReturnModal(loan)}
                          className="px-3 py-1.5 bg-slate-100 text-[#003399] font-medium text-xs rounded border border-slate-200 hover:bg-[#003399] hover:text-white transition-colors cursor-pointer"
                        >
                          📦 Registrar Devolução
                        </button>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        )}
      </div>

      {/* Modal de Confirmação */}
      {confirmModal && (
        <div
          className="fixed inset-0 bg-slate-900/50 backdrop-blur-sm flex items-center justify-center z-50"
          onClick={() => !returning && setConfirmModal(null)}
        >
          <div
            className="bg-white rounded-lg shadow-xl max-w-md w-full p-6"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 className="text-lg font-bold text-slate-900 mb-2">Confirmar Devolução</h3>
            <p className="text-sm text-slate-600 mb-6">
              Deseja confirmar a devolução do livro <strong>{confirmModal.bookTitle}</strong> do
              aluno <strong>{confirmModal.userName}</strong>?
            </p>
            <div className="flex justify-end gap-3">
              <button
                onClick={() => setConfirmModal(null)}
                disabled={returning}
                className="px-4 py-2 bg-slate-100 text-slate-700 rounded-md text-sm font-medium hover:bg-slate-200 cursor-pointer disabled:opacity-50"
              >
                Cancelar
              </button>
              <button
                onClick={handleConfirmReturn}
                disabled={returning}
                className="px-4 py-2 bg-emerald-600 text-white rounded-md text-sm font-medium hover:bg-emerald-700 cursor-pointer disabled:opacity-50"
              >
                {returning ? 'Processando...' : 'Confirmar Devolução'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
