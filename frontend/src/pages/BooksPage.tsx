import { useDebounce } from '../hooks/useDebounce';
import { BookResponse } from '../types/book';
import api from '../services/api';
import './BooksPage.css';

export function BooksPage() {
  const [books, setBooks] = useState<BookResponse[]>([]);
  const [searchInput, setSearchInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Aplica debounce à query de busca
  const debouncedQuery = useDebounce(searchInput, 300);

  // Buscar livros quando a query debounce mudar
  useEffect(() => {
    const fetchBooks = async () => {
      setIsLoading(true);
      setError(null);

      try {
        const params = debouncedQuery ? { q: debouncedQuery } : {};
        const response = await api.get<BookResponse[]>('/books', { params });
        setBooks(response.data);
      } catch (err) {
        setError('Erro ao buscar livros. Tente novamente.');
        console.error(err);
      } finally {
        setIsLoading(false);
      }
    };

    fetchBooks();
  }, [debouncedQuery]);

  const handleClearSearch = () => {
    setSearchInput('');
  };

  return (
    <div className="books-page">
      <h1>Livros Cadastrados</h1>

      {/* Campo de busca com debounce */}
      <div className="search-container">
        <div className="search-input-wrapper">
          <span className="search-icon">🔍</span>
          <input
            type="text"
            placeholder="Buscar por título ou autor..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            className="search-input"
            aria-label="Buscar livros"
          />
          {searchInput && (
            <button
              className="clear-button"
              onClick={handleClearSearch}
              title="Limpar busca"
              aria-label="Limpar campo de busca"
            >
              ✕
            </button>
          )}
        </div>

        {/* Indicador de carregamento */}
        {isLoading && <p className="loading-text">Buscando...</p>}
      </div>

      {/* Mensagem de erro */}
      {error && <p className="error-message">{error}</p>}

      {/* Lista de livros ou mensagem vazia */}
      {!isLoading && books.length === 0 && searchInput && (
        <p className="empty-message">
          Nenhum livro encontrado para "<strong>{searchInput}</strong>"
        </p>
      )}

      {!isLoading && books.length === 0 && !searchInput && (
        <p className="empty-message">Nenhum livro cadastrado</p>
      )}

      {/* Grid de livros */}
      {books.length > 0 && (
        <div className="books-grid">
          {books.map((book) => (
            <div key={book.id} className="book-card">
              <h3>{book.title}</h3>
              <p className="author">por {book.author}</p>
              <p className="isbn">ISBN: {book.isbn}</p>
              <p className="publisher">{book.publisher}</p>
              <p className="year">Ano: {book.year}</p>
              <p className="quantity">Quantidade: {book.quantity}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

import { useEffect, useState } from 'react';
