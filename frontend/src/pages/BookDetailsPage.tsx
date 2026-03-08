import { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { ArrowLeft, Book, Copy, User, Calendar, AlertTriangle, CheckCircle } from 'lucide-react';
import { api } from '../services/api';

interface BookInfo {
  id: number;
  title: string;
  author: string;
  isbn: string;
  category: string;
  quantity: number;
  availableCopies: number;
}

interface CopyDetail {
  id: number;
  copyCode: string;
  status: 'DISPONIVEL' | 'EMPRESTADO' | 'ATRASADO';
  borrowerName: string | null;
  borrowerEmail: string | null;
  loanDate: string | null;
  dueDate: string | null;
}

export default function BookDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const [book, setBook] = useState<BookInfo | null>(null);
  const [copies, setCopies] = useState<CopyDetail[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    loadData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id]);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [bookRes, copiesRes] = await Promise.all([
        api(`/books/${id}`),
        api(`/books/${id}/copies`),
      ]);

      if (!bookRes.ok) throw new Error('Livro nao encontrado');
      if (!copiesRes.ok) throw new Error('Falha ao carregar exemplares');

      const bookData = await bookRes.json();
      const copiesData = await copiesRes.json();

      setBook(bookData);
      setCopies(copiesData);
    } catch (err) {
      console.error(err);
      setError('Erro ao carregar detalhes do livro.');
    } finally {
      setLoading(false);
    }
  };

  const statusConfig: Record<
    string,
    { label: string; color: string; bg: string; icon: React.ReactNode }
  > = {
    DISPONIVEL: {
      label: 'Disponivel',
      color: 'text-emerald-700',
      bg: 'bg-emerald-50 border-emerald-200',
      icon: <CheckCircle className="w-4 h-4 text-emerald-500" />,
    },
    EMPRESTADO: {
      label: 'Emprestado',
      color: 'text-blue-700',
      bg: 'bg-blue-50 border-blue-200',
      icon: <User className="w-4 h-4 text-blue-500" />,
    },
    ATRASADO: {
      label: 'Atrasado',
      color: 'text-red-700',
      bg: 'bg-red-50 border-red-200',
      icon: <AlertTriangle className="w-4 h-4 text-red-500" />,
    },
  };

  const formatDate = (dateStr: string | null) => {
    if (!dateStr) return '-';
    const d = new Date(dateStr + 'T00:00:00');
    return d.toLocaleDateString('pt-BR');
  };

  const countByStatus = (status: string) => copies.filter((c) => c.status === status).length;

  if (loading) {
    return (
      <div className="p-8 max-w-5xl mx-auto">
        <p className="text-slate-500 text-center py-12">Carregando detalhes do livro...</p>
      </div>
    );
  }

  if (error || !book) {
    return (
      <div className="p-8 max-w-5xl mx-auto">
        <div className="bg-red-50 border border-red-200 rounded-lg p-6 text-center">
          <p className="text-red-600 font-medium">{error || 'Livro nao encontrado.'}</p>
          <Link to="/" className="text-sm text-[#003399] mt-2 inline-block hover:underline">
            Voltar ao catalogo
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="p-8 max-w-5xl mx-auto space-y-6">
      {/* Header com voltar */}
      <div className="flex items-center gap-4">
        <Link
          to="/"
          className="p-2 text-slate-400 hover:text-slate-700 bg-white border border-slate-200 rounded-lg hover:shadow-sm transition-all no-underline"
        >
          <ArrowLeft className="w-5 h-5" />
        </Link>
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Detalhes do Livro</h1>
          <p className="text-sm text-slate-500">Gerenciamento de Exemplares</p>
        </div>
      </div>

      {/* Info do livro */}
      <div className="bg-white rounded-lg border border-slate-200 shadow-sm p-6">
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 bg-[#003399]/10 rounded-lg flex items-center justify-center shrink-0">
            <Book className="w-6 h-6 text-[#003399]" />
          </div>
          <div className="flex-1 min-w-0">
            <h2 className="text-xl font-bold text-slate-800 leading-tight">{book.title}</h2>
            <p className="text-sm text-slate-600 mt-1">
              por <span className="font-semibold">{book.author}</span>
            </p>
            <div className="flex flex-wrap gap-4 mt-3 text-xs text-slate-500">
              <span>
                ISBN: <span className="font-medium text-slate-700">{book.isbn}</span>
              </span>
              <span>
                Categoria: <span className="font-medium text-slate-700">{book.category}</span>
              </span>
              <span>
                Total:{' '}
                <span className="font-medium text-slate-700">{book.quantity} exemplar(es)</span>
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Contadores de status */}
      <div className="grid grid-cols-3 gap-4">
        <div className="bg-emerald-50 border border-emerald-200 rounded-lg p-4 text-center">
          <p className="text-2xl font-bold text-emerald-700">{countByStatus('DISPONIVEL')}</p>
          <p className="text-xs text-emerald-600 font-medium mt-1">Disponiveis</p>
        </div>
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 text-center">
          <p className="text-2xl font-bold text-blue-700">{countByStatus('EMPRESTADO')}</p>
          <p className="text-xs text-blue-600 font-medium mt-1">Emprestados</p>
        </div>
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-center">
          <p className="text-2xl font-bold text-red-700">{countByStatus('ATRASADO')}</p>
          <p className="text-xs text-red-600 font-medium mt-1">Atrasados</p>
        </div>
      </div>

      {/* Tabela de exemplares */}
      <div className="bg-white rounded-lg border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-200 flex items-center gap-2">
          <Copy className="w-5 h-5 text-[#003399]" />
          <h3 className="text-base font-semibold text-slate-800">Exemplares ({copies.length})</h3>
        </div>

        {copies.length === 0 ? (
          <div className="p-8 text-center text-slate-400">
            Nenhum exemplar cadastrado para este livro.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-slate-50 text-left">
                  <th className="px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    Codigo Hash
                  </th>
                  <th className="px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    Emprestado para
                  </th>
                  <th className="px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    <div className="flex items-center gap-1">
                      <Calendar className="w-3.5 h-3.5" /> Data Emprestimo
                    </div>
                  </th>
                  <th className="px-6 py-3 text-xs font-semibold text-slate-500 uppercase tracking-wider">
                    <div className="flex items-center gap-1">
                      <Calendar className="w-3.5 h-3.5" /> Data Devolucao
                    </div>
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {copies.map((copy) => {
                  const cfg = statusConfig[copy.status] || statusConfig.DISPONIVEL;
                  return (
                    <tr key={copy.id} className="hover:bg-slate-50 transition-colors">
                      <td className="px-6 py-4">
                        <span className="font-mono text-sm font-bold text-slate-800 bg-slate-100 px-2 py-1 rounded">
                          {copy.copyCode}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        <span
                          className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold border ${cfg.bg} ${cfg.color}`}
                        >
                          {cfg.icon}
                          {cfg.label}
                        </span>
                      </td>
                      <td className="px-6 py-4">
                        {copy.borrowerName ? (
                          <div>
                            <p className="font-medium text-slate-800">{copy.borrowerName}</p>
                            <p className="text-xs text-slate-400">{copy.borrowerEmail}</p>
                          </div>
                        ) : (
                          <span className="text-slate-300">-</span>
                        )}
                      </td>
                      <td className="px-6 py-4 text-slate-600">{formatDate(copy.loanDate)}</td>
                      <td className="px-6 py-4">
                        {copy.dueDate ? (
                          <span
                            className={
                              copy.status === 'ATRASADO'
                                ? 'text-red-600 font-semibold'
                                : 'text-slate-600'
                            }
                          >
                            {formatDate(copy.dueDate)}
                          </span>
                        ) : (
                          <span className="text-slate-300">-</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
