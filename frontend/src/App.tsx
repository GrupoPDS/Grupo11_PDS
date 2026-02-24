import { useEffect, useState } from 'react';
import { Toast, type ToastType } from './components/Toast';
import './App.css';

interface Book {
  id: number;
  title: string;
  author: string;
  isbn: string;
  category: string;
  quantity: number;
}

type ToastNotification = {
  message: string;
  type: ToastType;
  id: string;
} | null;

const API_URL = 'http://localhost:8080/api/books';

function App() {
  const [title, setTitle] = useState('');
  const [author, setAuthor] = useState('');
  const [isbn, setIsbn] = useState('');
  const [category, setCategory] = useState('Tecnologia');
  const [loading, setLoading] = useState(false);
  const [toast, setToast] = useState<ToastNotification>(null);
  const [books, setBooks] = useState<Book[]>([]);
  const [loadingBooks, setLoadingBooks] = useState(true);

  const categories = ['Tecnologia', 'Ficção', 'Educação', 'História'];

  // Validação em tempo real
  const isValidISBN = isbn.trim().length > 0;
  const isValidTitle = title.trim().length > 0;
  const isValidAuthor = author.trim().length > 0;
  const canSubmit = isValidTitle && isValidAuthor && isValidISBN && !loading;

  // Carregar livros ao montar o componente
  useEffect(() => {
    loadBooks();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Mostrar Toast com auto-dismiss
  const showToast = (message: string, type: ToastType) => {
    const id = Math.random().toString(36).substr(2, 9);
    setToast({ message, type, id });
    setTimeout(() => setToast(null), 4000);
  };

  // Carregar lista de livros
  const loadBooks = async () => {
    try {
      setLoadingBooks(true);
      const res = await fetch(API_URL);
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

  // Submeter formulário
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
        quantity: 1,
      };

      const res = await fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      });

      if (res.status === 201) {
        showToast('✓ Livro cadastrado com sucesso!', 'success');
        setTitle('');
        setAuthor('');
        setIsbn('');
        setCategory('Tecnologia');
        // Recarregar lista de livros
        await loadBooks();
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
    <div className="app">
      <div className="header">
        <h1>📚 Sistema de Cadastro de Livros</h1>
        <p>Adicione novos livros ao acervo da biblioteca</p>
      </div>

      <div className="container">
        <section className="form-section">
          <h2>Novo Livro</h2>
          <form className="book-form" onSubmit={handleSubmit}>
            <div className="form-group">
              <label htmlFor="title">
                Título <span className="required">*</span>
              </label>
              <input
                id="title"
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Ex: Engenharia de Software Moderna"
                disabled={loading}
                className={isValidTitle && title.trim() ? 'valid' : ''}
              />
              {title.trim() && <span className="check">✓</span>}
            </div>

            <div className="form-group">
              <label htmlFor="author">
                Autor <span className="required">*</span>
              </label>
              <input
                id="author"
                type="text"
                value={author}
                onChange={(e) => setAuthor(e.target.value)}
                placeholder="Ex: Marco Tulio Valente"
                disabled={loading}
                className={isValidAuthor && author.trim() ? 'valid' : ''}
              />
              {author.trim() && <span className="check">✓</span>}
            </div>

            <div className="form-group">
              <label htmlFor="isbn">
                ISBN <span className="required">*</span>
              </label>
              <input
                id="isbn"
                type="text"
                value={isbn}
                onChange={(e) => setIsbn(e.target.value)}
                placeholder="Ex: 978-6500000000"
                disabled={loading}
                className={isValidISBN && isbn.trim() ? 'valid' : ''}
              />
              {isbn.trim() && <span className="check">✓</span>}
            </div>

            <div className="form-group">
              <label htmlFor="category">Categoria</label>
              <select
                id="category"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                disabled={loading}
              >
                {categories.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
            </div>

            <button type="submit" disabled={!canSubmit} className="submit-btn">
              {loading ? '⏳ Enviando...' : '➕ Cadastrar'}
            </button>
            <p className="form-hint">Todos os campos marcados com * são obrigatórios</p>
          </form>
        </section>

        <section className="books-section">
          <h2>Livros Cadastrados ({books.length})</h2>
          {loadingBooks ? (
            <p className="loading">Carregando livros...</p>
          ) : books.length === 0 ? (
            <p className="empty">Nenhum livro cadastrado ainda</p>
          ) : (
            <div className="books-list">
              {books.map((book) => (
                <div key={book.id} className="book-card">
                  <div className="book-header">
                    <h3>{book.title}</h3>
                    <span className="category-badge">{book.category}</span>
                  </div>
                  <p className="book-author">
                    por <strong>{book.author}</strong>
                  </p>
                  <p className="book-isbn">ISBN: {book.isbn}</p>
                  <p className="book-quantity">Quantidade: {book.quantity}</p>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>

      {toast && <Toast message={toast.message} type={toast.type} />}
    </div>
  );
}

export default App;
