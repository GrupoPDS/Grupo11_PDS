import { useEffect, useState, useCallback } from 'react';
import { AlertCircle, Download, Search } from 'lucide-react';
import { api } from '../services/api';
import { useDebounce } from '../hooks/useDebounce';
import { usePageTitle } from '../hooks/usePageTitle';

/* ── Tipos ── */

interface OverdueItem {
  loanId: number;
  userName: string;
  userEmail: string;
  userPhone: string | null;
  bookTitle: string;
  bookIsbn: string;
  loanDate: string;
  dueDate: string;
  daysOverdue: number;
  severity: string;
}

interface OverdueSummary {
  totalOverdue: number;
  totalActiveLoans: number;
  overduePercentage: number;
  averageDaysOverdue: number;
  lowSeverity: number;
  mediumSeverity: number;
  highSeverity: number;
  criticalSeverity: number;
}

interface PagedResult {
  content: OverdueItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

/* ── Componente ── */

export default function OverdueReportPage() {
  usePageTitle('Atrasos');
  const [pagedData, setPagedData] = useState<PagedResult | null>(null);
  const [summary, setSummary] = useState<OverdueSummary | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [searchInput, setSearchInput] = useState('');
  const [severityFilter, setSeverityFilter] = useState<string>('ALL');
  const debouncedSearch = useDebounce(searchInput, 400);
  const [loading, setLoading] = useState(false);
  const [exportingCsv, setExportingCsv] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const PAGE_SIZE = 8;

  /* ── Fetch: Lista paginada ── */
  const fetchOverdueLoans = useCallback(async (page: number, search: string, severity: string) => {
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      params.set('page', String(page));
      params.set('size', String(PAGE_SIZE));
      if (search.trim()) {
        params.set('search', search.trim());
      }
      const minDaysMap: Record<string, number> = {
        LOW: 1,
        MEDIUM: 8,
        HIGH: 15,
        CRITICAL: 31,
      };
      if (severity !== 'ALL' && minDaysMap[severity]) {
        params.set('minDays', String(minDaysMap[severity]));
      }
      const res = await api(`/overdue-report?${params.toString()}`);
      if (!res.ok) throw new Error('Falha ao carregar relatório de atrasos');
      const data: PagedResult = await res.json();
      if (severity !== 'ALL') {
        data.content = data.content.filter((item) => item.severity === severity);
      }
      setPagedData(data);
    } catch (err) {
      console.error(err);
      setError('Erro ao carregar o relatório de atrasos.');
    } finally {
      setLoading(false);
    }
  }, []);

  /* ── Fetch: Resumo ── */
  const fetchSummary = useCallback(async () => {
    try {
      const res = await api('/overdue-report/summary');
      if (res.ok) setSummary(await res.json());
    } catch (err) {
      console.error('Falha ao obter resumo de atrasos:', err);
    }
  }, []);

  /* ── Effects ── */
  useEffect(() => {
    fetchSummary();
  }, [fetchSummary]);

  useEffect(() => {
    fetchOverdueLoans(currentPage, debouncedSearch, severityFilter);
  }, [fetchOverdueLoans, currentPage, debouncedSearch, severityFilter]);

  useEffect(() => {
    setCurrentPage(0);
  }, [debouncedSearch, severityFilter]);

  /* ── CSV Export ── */
  const handleExportCsv = async () => {
    setExportingCsv(true);
    try {
      const res = await api('/overdue-report/export');
      if (!res.ok) throw new Error('Falha ao exportar CSV');
      const blob = await res.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'relatorio-atrasos.csv';
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error(err);
      setError('Erro ao exportar relatório CSV.');
    } finally {
      setExportingCsv(false);
    }
  };

  /* ── Helpers de UI ── */
  const severityBadge = (severity: string) => {
    const map: Record<string, { label: string; className: string }> = {
      LOW: { label: 'Leve', className: 'bg-emerald-100 text-emerald-800' },
      MEDIUM: { label: 'Moderado', className: 'bg-yellow-100 text-yellow-800' },
      HIGH: { label: 'Alto', className: 'bg-orange-100 text-orange-800' },
      CRITICAL: { label: 'Crítico', className: 'bg-red-100 text-red-800' },
    };
    const info = map[severity] || { label: severity, className: 'bg-slate-100 text-slate-700' };
    return (
      <span className={`px-2 py-1 rounded text-xs font-bold ${info.className}`}>{info.label}</span>
    );
  };

  const severityBarPercent = (count: number) =>
    summary && summary.totalOverdue > 0 ? (count / summary.totalOverdue) * 100 : 0;

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-800 flex items-center gap-2">
          <AlertCircle className="w-6 h-6 text-red-600" /> Relatório de Atrasos
        </h1>
        <p className="text-sm text-slate-500">
          Visão analítica de retenções e severidade de multas
        </p>
      </div>

      {error && (
        <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded-md text-sm font-medium">
          {error}
        </div>
      )}

      {/* ── Summary Cards ── */}
      {summary && (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div className="bg-white p-5 rounded-lg border border-red-200 shadow-sm border-l-4 border-l-red-500">
            <p className="text-sm font-medium text-slate-500 mb-1 m-0">Em Atraso</p>
            <h3 className="text-2xl font-bold text-red-600 m-0">{summary.totalOverdue}</h3>
          </div>
          <div className="bg-white p-5 rounded-lg border border-slate-200 shadow-sm">
            <p className="text-sm font-medium text-slate-500 mb-1 m-0">% dos Empréstimos</p>
            <h3 className="text-2xl font-bold text-slate-800 m-0">{summary.overduePercentage}%</h3>
          </div>
          <div className="bg-white p-5 rounded-lg border border-slate-200 shadow-sm">
            <p className="text-sm font-medium text-slate-500 mb-1 m-0">Atraso Médio</p>
            <h3 className="text-2xl font-bold text-slate-800 m-0">
              {summary.averageDaysOverdue} dias
            </h3>
          </div>
          <div className="bg-white p-5 rounded-lg border border-slate-200 shadow-sm">
            <p className="text-sm font-medium text-slate-500 mb-1 m-0">Empréstimos Ativos</p>
            <h3 className="text-2xl font-bold text-slate-800 m-0">{summary.totalActiveLoans}</h3>
          </div>
        </div>
      )}

      {/* ── Severity Distribution ── */}
      {summary && summary.totalOverdue > 0 && (
        <div className="bg-white p-6 rounded-lg border border-slate-200 shadow-sm">
          <h3 className="text-sm font-bold text-slate-800 mb-4 mt-0">
            Distribuição por Severidade
          </h3>
          <div className="flex h-6 rounded-full overflow-hidden border border-slate-100 mb-3 shadow-inner">
            {summary.lowSeverity > 0 && (
              <div
                className="bg-emerald-500 flex items-center justify-center text-[10px] text-white font-bold"
                style={{ width: `${severityBarPercent(summary.lowSeverity)}%` }}
              >
                {summary.lowSeverity} (Leve)
              </div>
            )}
            {summary.mediumSeverity > 0 && (
              <div
                className="bg-yellow-400 flex items-center justify-center text-[10px] text-yellow-900 font-bold"
                style={{ width: `${severityBarPercent(summary.mediumSeverity)}%` }}
              >
                {summary.mediumSeverity} (Mod)
              </div>
            )}
            {summary.highSeverity > 0 && (
              <div
                className="bg-orange-500 flex items-center justify-center text-[10px] text-white font-bold"
                style={{ width: `${severityBarPercent(summary.highSeverity)}%` }}
              >
                {summary.highSeverity} (Alto)
              </div>
            )}
            {summary.criticalSeverity > 0 && (
              <div
                className="bg-red-600 flex items-center justify-center text-[10px] text-white font-bold"
                style={{ width: `${severityBarPercent(summary.criticalSeverity)}%` }}
              >
                {summary.criticalSeverity} (Crit)
              </div>
            )}
          </div>
          <div className="flex justify-between text-xs text-slate-500 px-1">
            <span>Leve (1-7d)</span>
            <span>Moderado (8-14d)</span>
            <span>Alto (15-30d)</span>
            <span>Crítico (31d+)</span>
          </div>
        </div>
      )}

      {/* ── Actions & Table ── */}
      <div className="bg-white rounded-lg border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-200 flex flex-col md:flex-row justify-between gap-4 bg-slate-50/50">
          <div className="flex gap-3 items-center flex-wrap">
            <div className="relative w-full md:w-64">
              <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
              <input
                type="text"
                placeholder="Buscar nome, e-mail, título..."
                value={searchInput}
                onChange={(e) => setSearchInput(e.target.value)}
                className="w-full pl-9 pr-3 py-2 bg-white border border-slate-200 rounded-md text-sm outline-none focus:border-[#003399]"
              />
            </div>
            <div className="flex gap-2 items-center">
              <span className="text-xs font-semibold text-slate-500">Filtros:</span>
              {['ALL', 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL'].map((sev) => (
                <button
                  key={sev}
                  onClick={() => setSeverityFilter(sev)}
                  className={`px-3 py-1 text-xs rounded-full cursor-pointer transition-colors ${
                    severityFilter === sev
                      ? 'bg-slate-800 text-white'
                      : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'
                  }`}
                >
                  {sev === 'ALL' && 'Todos'}
                  {sev === 'LOW' && 'Leve'}
                  {sev === 'MEDIUM' && 'Moderado'}
                  {sev === 'HIGH' && 'Alto'}
                  {sev === 'CRITICAL' && 'Crítico'}
                </button>
              ))}
            </div>
          </div>
          <button
            onClick={handleExportCsv}
            disabled={exportingCsv}
            className="flex items-center gap-2 px-3 py-1.5 bg-emerald-600 text-white rounded-md text-sm font-medium hover:bg-emerald-700 transition-colors cursor-pointer disabled:opacity-50 whitespace-nowrap"
          >
            <Download className="w-4 h-4" />
            {exportingCsv ? 'Gerando...' : 'Exportar Relatório CSV'}
          </button>
        </div>

        {loading ? (
          <p className="text-slate-500 text-center py-8">Carregando relatório...</p>
        ) : pagedData && pagedData.content.length > 0 ? (
          <>
            <table className="w-full text-left text-sm border-collapse">
              <thead>
                <tr className="bg-slate-50 text-xs uppercase text-slate-500 font-semibold border-b border-slate-200">
                  <th className="px-6 py-4">Usuário</th>
                  <th className="px-6 py-4">Livro</th>
                  <th className="px-6 py-4">Retirada / Prazo</th>
                  <th className="px-6 py-4 text-center">Dias Atraso</th>
                  <th className="px-6 py-4">Severidade</th>
                </tr>
              </thead>
              <tbody>
                {pagedData.content.map((item) => (
                  <tr
                    key={item.loanId}
                    className={`border-b border-slate-100 hover:bg-slate-50 ${
                      item.severity === 'CRITICAL' ? 'bg-red-50/20' : ''
                    }`}
                  >
                    <td className="px-6 py-4">
                      <p className="font-semibold text-slate-800 m-0">{item.userName}</p>
                      <a
                        href={`mailto:${item.userEmail}`}
                        className="text-xs text-[#003399] hover:underline"
                      >
                        {item.userEmail}
                      </a>
                    </td>
                    <td className="px-6 py-4">
                      <p className="font-medium text-slate-800 m-0">{item.bookTitle}</p>
                      <p className="text-xs text-slate-500 m-0">ISBN: {item.bookIsbn}</p>
                    </td>
                    <td className="px-6 py-4">
                      <p className="text-slate-600 text-xs m-0">
                        Ret: {new Date(item.loanDate).toLocaleDateString()}
                      </p>
                      <p className="text-red-600 text-xs font-medium m-0">
                        Venc: {new Date(item.dueDate).toLocaleDateString()}
                      </p>
                    </td>
                    <td className="px-6 py-4 text-center font-bold text-red-600">
                      {item.daysOverdue}
                    </td>
                    <td className="px-6 py-4">{severityBadge(item.severity)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {pagedData.totalPages > 1 && (
              <div className="px-6 py-4 bg-slate-50 text-xs text-slate-500 flex justify-between items-center">
                <button
                  disabled={currentPage === 0}
                  onClick={() => setCurrentPage((p) => p - 1)}
                  className="hover:text-slate-800 cursor-pointer disabled:opacity-40"
                >
                  ← Anterior
                </button>
                <span>
                  Página {currentPage + 1} de {pagedData.totalPages} ({pagedData.totalElements}{' '}
                  registros)
                </span>
                <button
                  disabled={!pagedData.hasNext}
                  onClick={() => setCurrentPage((p) => p + 1)}
                  className="hover:text-slate-800 text-[#003399] font-medium cursor-pointer disabled:opacity-40"
                >
                  Próximo →
                </button>
              </div>
            )}
          </>
        ) : (
          <div className="text-center py-12 text-slate-400">
            {debouncedSearch
              ? '🔍 Nenhum atraso encontrado com essa busca.'
              : 'Nenhum empréstimo em atraso no momento! Tudo em dia.'}
          </div>
        )}
      </div>
    </div>
  );
}
