import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { PlusCircle, Bookmark, BookOpen } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { api } from '../services/api';
import { Toast, type ToastType } from '../components/Toast';

interface BookCopy {
  id: number;
  copyCode: string;
}

interface Book {
  id: number;
  title: string;
  author: string;
  isbn: string;
  category: string;
  quantity: number;
  availableCopies: number;
  copies: BookCopy[];
}

type ToastNotification = {
  message: string;
  type: ToastType;
  id: string;
} | null;

export default function CatalogPage() {
  const { user } = useAuth();
  const [title, setTitle] = useState('');
  const [author, setAuthor] = useState('');
  const [isbn, setIsbn] = useState('');
  const [category, setCategory] = useState('Tecnologia');
  const [quantity, setQuantity] = useState(1);
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState<ToastNotification>(null);
  const [books, setBooks] = useState<Book[]>([]);
  const [loadingBooks, setLoadingBooks] = useState(true);
  const [borrowingBookId, setBorrowingBookId] = useState<number | null>(null);
  const [reservingBookId, setReservingBookId] = useState<number | null>(null);

  const categories = ['Tecnologia', 'Ficção', 'Educação', 'História'];

  const isValidISBN = isbn.trim().length > 0;
  const isValidTitle = title.trim().length > 0;
  const isValidAuthor = author.trim().length > 0;
  const canSubmit = isValidTitle && isValidAuthor && isValidISBN && !loading;

  const canCreateBook = user?.role === 'ADMIN' || user?.role === 'LIBRARIAN';

  useEffect(() => {
    loadBooks();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const showToast = (message: string, type: ToastType) => {
    const id = Math.random().toString(36).substr(2, 9);
    setToast({ message, type, id });
    setTimeout(() => setToast(null), 4000);
  };

  const handleBorrowBook = async (bookId: number) => {
    if (borrowingBookId) return; // Previne cliques duplos
    setBorrowingBookId(bookId);
    try {
      const res = await api('/loans/borrow', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ bookId }),
      });

      if (res.status === 201) {
        showToast('Empréstimo registrado! Prazo de devolução: 14 dias.', 'success');
        await loadBooks();
        return;
      }
      if (res.status === 400 || res.status === 409) {
        const body = await res.json().catch(() => ({}));
        throw new Error(body.error || 'Este livro não está disponível no momento.');
      }
      if (!res.ok) throw new Error('Falha ao registrar empréstimo.');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Erro inesperado.';
      console.error(err);
      showToast(message, 'error');
    } finally {
      setBorrowingBookId(null);
    }
  };

  const handleReserveBook = async (bookId: number) => {
    if (reservingBookId !== null) return;
    setReservingBookId(bookId);
    try {
      const res = await api('/reservations', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ bookId }),
      });

      if (res.status === 409) {
        showToast(
          'Este livro tem cópias disponíveis na estante! Dirija-se à biblioteca para retirá-lo.',
          'success',
        );
        return;
      }
      if (res.status === 400 || res.status === 403) {
        const body = await res.json().catch(() => ({}));
        throw new Error(
          body.error || body.message || 'Você já possui uma reserva ativa para este livro.',
        );
      }
      if (!res.ok) throw new Error('Falha ao reservar o livro.');

      showToast('Reserva efetuada com sucesso! Você entrou na fila de espera.', 'success');
      await loadBooks();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Erro inesperado ao tentar reservar.';
      console.error(err);
      showToast(message, 'error');
    } finally {
      setReservingBookId(null);
    }
  };

  const loadBooks = async () => {
    try {
      setLoadingBooks(true);
      const res = await api('/books');
      if (!res.ok) throw new Error('Falha ao carregar livros');
      const data = await res.json();
      setBooks(data || []);
    } catch (err) {
      console.error('Erro ao carregar livros:', err);
      showToast('Falha ao carregar lista de livros', 'error');
    } finally {
      setLoadingBooks(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canSubmit) return;

    setLoading(true);

    try {
      const body = {
        title: title.trim(),
        author: author.trim(),
        isbn: isbn.trim(),
        category: category,
        publisher: '',
        year: null,
        quantity: quantity,
      };

      const res = await api('/books', {
        method: 'POST',
        body: JSON.stringify(body),
      });

      if (res.status === 201) {
        showToast('Livro cadastrado com sucesso!', 'success');
        setTitle('');
        setAuthor('');
        setIsbn('');
        setCategory('Tecnologia');
        setQuantity(1);
        await loadBooks();
      } else if (res.status === 403) {
        showToast('Você não tem permissão para cadastrar livros', 'error');
      } else if (res.status === 409) {
        const json = await res.json().catch(() => ({}));
        showToast(`ISBN duplicado: ${json.error || 'Este ISBN já existe'}`, 'error');
      } else if (res.status >= 400 && res.status < 500) {
        const json = await res.json().catch(() => ({}));
        const detalhes = json.detalhes
          ? json.detalhes
              .map((d: { campo: string; mensagem: string }) => `${d.campo}: ${d.mensagem}`)
              .join('; ')
          : json.error || 'Dados inválidos';
        showToast(`Erro: ${detalhes}`, 'error');
      } else if (res.status >= 500) {
        showToast('Erro do servidor. Tente novamente mais tarde.', 'error');
      }
    } catch (err) {
      showToast('Falha ao conectar com o servidor', 'error');
      console.error('Erro na requisição:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-8">
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-slate-800">Catálogo de Livros</h1>
          <p className="text-sm text-slate-500">Gerenciamento do Acervo Mestre</p>
        </div>
      </div>

      {/* Formulário de cadastro (ADMIN/LIBRARIAN) */}
      {canCreateBook && (
        <div className="bg-white p-6 rounded-lg border border-slate-200 shadow-sm">
          <h2 className="text-base font-semibold text-slate-800 mb-4 flex items-center gap-2">
            <PlusCircle className="w-5 h-5 text-[#003399]" /> Cadastrar Novo Livro
          </h2>
          <form onSubmit={handleSubmit} className="grid grid-cols-1 md:grid-cols-5 gap-4 items-end">
            <div>
              <label htmlFor="title" className="block text-xs font-medium text-slate-500 mb-1">
                Título do Livro <span className="text-red-500">*</span>
              </label>
              <input
                id="title"
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Ex: Clean Code"
                disabled={loading}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-md text-sm outline-none focus:border-[#003399] transition-colors disabled:opacity-50"
              />
            </div>
            <div>
              <label htmlFor="author" className="block text-xs font-medium text-slate-500 mb-1">
                Autor <span className="text-red-500">*</span>
              </label>
              <input
                id="author"
                type="text"
                value={author}
                onChange={(e) => setAuthor(e.target.value)}
                placeholder="Ex: Robert C. Martin"
                disabled={loading}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-md text-sm outline-none focus:border-[#003399] transition-colors disabled:opacity-50"
              />
            </div>
            <div>
              <label htmlFor="isbn" className="block text-xs font-medium text-slate-500 mb-1">
                ISBN <span className="text-red-500">*</span>
              </label>
              <input
                id="isbn"
                type="text"
                value={isbn}
                onChange={(e) => setIsbn(e.target.value)}
                placeholder="000-000-000"
                disabled={loading}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-md text-sm outline-none focus:border-[#003399] transition-colors disabled:opacity-50"
              />
            </div>
            <div>
              <label htmlFor="category" className="block text-xs font-medium text-slate-500 mb-1">
                Categoria
              </label>
              <select
                id="category"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                disabled={loading}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-md text-sm outline-none focus:border-[#003399] transition-colors disabled:opacity-50"
              >
                {categories.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex gap-2">
              <div className="w-20">
                <label htmlFor="quantity" className="block text-xs font-medium text-slate-500 mb-1">
                  Qtd <span className="text-red-500">*</span>
                </label>
                <input
                  id="quantity"
                  type="number"
                  min={1}
                  max={99}
                  value={quantity}
                  onChange={(e) => setQuantity(Math.max(1, Number(e.target.value)))}
                  disabled={loading}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-md text-sm outline-none focus:border-[#003399] transition-colors disabled:opacity-50"
                />
              </div>
              <button
                type="submit"
                disabled={!canSubmit}
                className="bg-[#003399] text-white px-4 py-2 rounded-md text-sm font-medium hover:bg-blue-800 transition-colors self-end cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {loading ? 'Salvando...' : 'Cadastrar'}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Grid de Livros */}
      {loadingBooks ? (
        <p className="text-slate-500 text-center py-8">Carregando livros...</p>
      ) : books.length === 0 ? (
        <p className="text-slate-400 text-center py-8">Nenhum livro cadastrado ainda</p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
          {books.map((book) => (
            <div
              key={book.id}
              className="bg-white rounded-lg border border-slate-200 shadow-sm flex flex-col overflow-hidden hover:shadow-md transition-shadow"
            >
              <div className="h-2 bg-[#003399]"></div>
              <div className="p-5 flex-1 flex flex-col">
                <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400 mb-2">
                  {book.category}
                </span>
                <h3 className="text-lg font-bold text-slate-800 leading-tight mb-1">
                  {canCreateBook ? (
                    <Link
                      to={`/books/${book.id}`}
                      className="hover:text-[#003399] transition-colors no-underline text-slate-800"
                      title="Ver exemplares"
                    >
                      {book.title}
                    </Link>
                  ) : (
                    book.title
                  )}
                </h3>
                <p className="text-sm text-slate-600 mb-4">
                  por <span className="font-semibold">{book.author}</span>
                </p>

                <div className="mt-auto space-y-2">
                  <p className="text-xs text-slate-500 flex justify-between">
                    <span>ISBN:</span> <span>{book.isbn}</span>
                  </p>
                  <p className="text-xs text-slate-500 flex justify-between">
                    <span>Disponibilidade:</span>
                    <span
                      className={
                        book.availableCopies > 0
                          ? 'font-bold text-emerald-600'
                          : 'font-bold text-red-500'
                      }
                    >
                      {book.availableCopies > 0
                        ? `${book.availableCopies} de ${book.quantity} disponíveis`
                        : 'Todos emprestados'}
                    </span>
                  </p>

                  {/* Códigos dos exemplares */}
                  {book.copies && book.copies.length > 0 && (
                    <div className="mt-2">
                      <p className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider mb-1">
                        Exemplares
                      </p>
                      <div className="flex flex-wrap gap-1">
                        {book.copies.map((c) => (
                          <span
                            key={c.id}
                            className="px-1.5 py-0.5 bg-slate-100 text-slate-600 text-[10px] rounded font-mono"
                          >
                            {c.copyCode}
                          </span>
                        ))}
                      </div>
                    </div>
                  )}
                </div>
              </div>

              {user?.role === 'STUDENT' && (
                <div className="p-4 border-t border-slate-100 bg-slate-50">
                  {book.availableCopies > 0 ? (
                    <button
                      onClick={() => handleBorrowBook(book.id)}
                      disabled={borrowingBookId !== null || reservingBookId !== null}
                      className="w-full py-2 rounded-md text-sm font-semibold flex justify-center items-center gap-2 transition-colors cursor-pointer bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      <BookOpen className="w-4 h-4" />
                      {borrowingBookId === book.id ? 'Processando...' : 'Emprestar'}
                    </button>
                  ) : (
                    <button
                      onClick={() => handleReserveBook(book.id)}
                      disabled={reservingBookId !== null || borrowingBookId !== null}
                      className="w-full py-2 rounded-md text-sm font-semibold flex justify-center items-center gap-2 transition-colors cursor-pointer bg-amber-500 text-white hover:bg-amber-600 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      <Bookmark className="w-4 h-4" />
                      {reservingBookId === book.id ? 'Processando...' : 'Entrar na Fila de Espera'}
                    </button>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {toast && <Toast message={toast.message} type={toast.type} />}
    </div>
  );
}
