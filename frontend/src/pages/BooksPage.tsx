import { useEffect, useState } from 'react';

}
  );
    </div>
      )}
        </div>
          ))}
            </div>
              <p className="quantity">Quantidade: {book.quantity}</p>
              <p className="year">Ano: {book.year}</p>
              <p className="publisher">{book.publisher}</p>
              <p className="isbn">ISBN: {book.isbn}</p>
              <p className="author">por {book.author}</p>
              <h3>{book.title}</h3>
            <div key={book.id} className="book-card">
          {books.map((book) => (
        <div className="books-grid">
      {books.length > 0 && (
      {/* Grid de livros */}

      )}
        <p className="empty-message">Nenhum livro cadastrado</p>
      {!isLoading && books.length === 0 && !searchInput && (

      )}
        </p>
          Nenhum livro encontrado para "<strong>{searchInput}</strong>"
        <p className="empty-message">
      {!isLoading && books.length === 0 && searchInput && (
      {/* Lista de livros ou mensagem vazia */}

      {error && <p className="error-message">{error}</p>}
      {/* Mensagem de erro */}

      </div>
        {isLoading && <p className="loading-text">Buscando...</p>}
        {/* Indicador de carregamento */}

        </div>
          )}
            </button>
              ✕
            >
              aria-label="Limpar campo de busca"
              title="Limpar busca"
              onClick={handleClearSearch}
              className="clear-button"
            <button
          {searchInput && (
          />
            aria-label="Buscar livros"
            className="search-input"
            onChange={(e) => setSearchInput(e.target.value)}
            value={searchInput}
            placeholder="Buscar por título ou autor..."
            type="text"
          <input
          <span className="search-icon">🔍</span>
        <div className="search-input-wrapper">
      <div className="search-container">
      {/* Campo de busca com debounce */}

      <h1>Livros Cadastrados</h1>
    <div className="books-page">
  return (

  };
    setSearchInput('');
  const handleClearSearch = () => {

  }, [debouncedQuery]);
    fetchBooks();

    };
      }
        setIsLoading(false);
      } finally {
        console.error(err);
        setError('Erro ao buscar livros. Tente novamente.');
      } catch (err) {
        setBooks(response.data);
        const response = await api.get<BookResponse[]>('/books', { params });
        const params = debouncedQuery ? { q: debouncedQuery } : {};
      try {

      setError(null);
      setIsLoading(true);
    const fetchBooks = async () => {
  useEffect(() => {
  // Buscar livros quando a query debounce mudar

  const debouncedQuery = useDebounce(searchInput, 300);
  // Aplica debounce à query de busca

  const [error, setError] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [searchInput, setSearchInput] = useState('');
  const [books, setBooks] = useState<BookResponse[]>([]);
export function BooksPage() {

import './BooksPage.css';
import api from '../services/api';
import { BookResponse } from '../types/book';
import { useDebounce } from '../hooks/useDebounce';
