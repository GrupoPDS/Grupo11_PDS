import { type ReactNode, useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Search, Book, ArrowLeft, AlertCircle, Bookmark, LogOut, User, Bell } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { useDebounce } from '../hooks/useDebounce';
import { api } from '../services/api';

interface SearchResult {
  id: number;
  title: string;
  author: string;
  isbn: string;
  category: string;
  availableCopies: number;
  quantity: number;
}

interface DashboardLayoutProps {
  children: ReactNode;
}

export default function DashboardLayout({ children }: DashboardLayoutProps) {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const canManage = user?.role === 'ADMIN' || user?.role === 'LIBRARIAN';

  const [notificationCount, setNotificationCount] = useState(0);

  // Search state
  const [searchInput, setSearchInput] = useState('');
  const [searchResults, setSearchResults] = useState<SearchResult[]>([]);
  const [showDropdown, setShowDropdown] = useState(false);
  const [searching, setSearching] = useState(false);
  const debouncedSearch = useDebounce(searchInput, 300);

  useEffect(() => {
    if (!user) return;

    let cancelled = false;

    const poll = async () => {
      try {
        const res = await api('/reservations/my/notifications');
        if (res.ok && !cancelled) {
          const data = await res.json();
          setNotificationCount(data.availableForPickup || 0);
        }
      } catch {
        // silently fail
      }
    };

    poll();
    const interval = setInterval(poll, 30000);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [user]);

  // Live search effect
  useEffect(() => {
    if (!debouncedSearch.trim()) {
      setSearchResults([]);
      setShowDropdown(false);
      return;
    }

    let cancelled = false;
    const doSearch = async () => {
      setSearching(true);
      try {
        const res = await api(`/books?q=${encodeURIComponent(debouncedSearch.trim())}`);
        if (res.ok && !cancelled) {
          const data = await res.json();
          setSearchResults(data.slice(0, 6));
          setShowDropdown(true);
        }
      } catch {
        // silently fail
      } finally {
        if (!cancelled) setSearching(false);
      }
    };
    doSearch();
    return () => {
      cancelled = true;
    };
  }, [debouncedSearch]);

  // Close dropdown when navigating
  useEffect(() => {
    setShowDropdown(false);
  }, [location.pathname, location.search]);

  const handleSearchKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && searchInput.trim()) {
      setShowDropdown(false);
      navigate(`/?q=${encodeURIComponent(searchInput.trim())}`);
    }
    if (e.key === 'Escape') {
      setShowDropdown(false);
    }
  };

  const handleResultClick = (book: SearchResult) => {
    setShowDropdown(false);
    setSearchInput('');
    if (canManage) {
      navigate(`/books/${book.id}`);
    } else {
      navigate(`/?q=${encodeURIComponent(book.title)}`);
    }
  };

  const navItems = [
    { path: '/', icon: <Book />, label: 'Catálogo', visible: true },
    { path: '/returns', icon: <ArrowLeft />, label: 'Devoluções', visible: canManage },
    { path: '/overdue-report', icon: <AlertCircle />, label: 'Atrasos', visible: canManage },
    { path: '/my-loans', icon: <Bookmark />, label: 'Meus Empréstimos', visible: true },
  ];

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <div className="flex h-screen bg-slate-50 font-sans text-slate-900 overflow-hidden">
      {/* Sidebar */}
      <aside className="w-64 bg-slate-900 text-slate-300 flex flex-col border-r border-slate-800 shrink-0">
        <div className="h-16 flex items-center px-6 border-b border-slate-800 mb-4 bg-slate-950/50">
          <div className="w-8 h-8 bg-[#003399] rounded flex items-center justify-center text-white font-bold mr-3 shadow-sm">
            U
          </div>
          <span className="text-lg font-semibold text-white tracking-wide">
            Biblio<span className="text-[#003399]">UFU</span>
          </span>
        </div>

        <nav className="flex-1 px-4 space-y-1 overflow-y-auto">
          <div className="text-xs font-semibold text-slate-500 uppercase tracking-wider mb-2 ml-2 mt-4">
            Menu Principal
          </div>
          {navItems
            .filter((item) => item.visible)
            .map((item) => {
              const isActive =
                item.path === '/'
                  ? location.pathname === '/'
                  : location.pathname.startsWith(item.path);

              return (
                <Link
                  key={item.path}
                  to={item.path}
                  className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-md font-medium transition-all no-underline ${
                    isActive
                      ? 'bg-[#003399] text-white shadow-md'
                      : 'hover:bg-slate-800 hover:text-white text-slate-400'
                  }`}
                >
                  <span className="w-5 h-5 shrink-0 [&>svg]:w-5 [&>svg]:h-5">{item.icon}</span>
                  {item.label}
                </Link>
              );
            })}
        </nav>

        <div className="p-4 border-t border-slate-800 bg-slate-950/30">
          <div className="flex items-center gap-3 px-3 py-2 mb-2">
            <div className="w-8 h-8 rounded-full bg-slate-700 flex items-center justify-center text-slate-300">
              <User className="w-4 h-4" />
            </div>
            <div className="text-left flex-1 overflow-hidden">
              <p className="text-sm font-medium text-white truncate m-0">
                {user?.name || 'Usuário'}
              </p>
              <p className="text-[10px] text-slate-500 uppercase tracking-wider m-0">
                {user?.role || ''}
              </p>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="w-full flex items-center justify-center gap-2 px-3 py-2 bg-red-500/10 text-red-400 hover:bg-red-500/20 hover:text-red-300 rounded-md font-medium transition-colors border border-red-500/20 cursor-pointer"
          >
            <LogOut className="w-4 h-4" /> Sair
          </button>
        </div>
      </aside>

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0">
        <header className="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-8 shrink-0 z-10">
          <div className="flex items-center w-full max-w-md relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 z-10" />
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              onFocus={() => {
                if (searchResults.length > 0) setShowDropdown(true);
              }}
              onKeyDown={handleSearchKeyDown}
              placeholder="Buscar livros por titulo, autor, ISBN..."
              className="w-full pl-10 pr-4 py-2 bg-slate-100 border-transparent rounded-md text-sm focus:bg-white focus:border-[#003399] focus:ring-1 focus:ring-[#003399] outline-none transition-all"
            />

            {/* Dropdown de resultados */}
            {showDropdown && (
              <>
                <div className="fixed inset-0 z-10" onClick={() => setShowDropdown(false)} />
                <div className="absolute top-full left-0 right-0 mt-1 bg-white border border-slate-200 rounded-lg shadow-lg z-20 max-h-80 overflow-y-auto">
                  {searching ? (
                    <div className="px-4 py-3 text-sm text-slate-400">Buscando...</div>
                  ) : searchResults.length === 0 ? (
                    <div className="px-4 py-3 text-sm text-slate-400">
                      Nenhum resultado para &ldquo;{debouncedSearch}&rdquo;
                    </div>
                  ) : (
                    <>
                      {searchResults.map((book) => (
                        <button
                          key={book.id}
                          onClick={() => handleResultClick(book)}
                          className="w-full px-4 py-3 text-left hover:bg-slate-50 transition-colors flex items-start gap-3 border-b border-slate-100 last:border-b-0 cursor-pointer bg-transparent"
                        >
                          <Book className="w-4 h-4 text-[#003399] mt-0.5 shrink-0" />
                          <div className="flex-1 min-w-0">
                            <p className="text-sm font-medium text-slate-800 truncate m-0">
                              {book.title}
                            </p>
                            <p className="text-xs text-slate-500 m-0">{book.author}</p>
                          </div>
                          <span
                            className={`text-[10px] font-bold px-1.5 py-0.5 rounded-full shrink-0 ${
                              book.availableCopies > 0
                                ? 'bg-emerald-100 text-emerald-700'
                                : 'bg-red-100 text-red-600'
                            }`}
                          >
                            {book.availableCopies > 0
                              ? `${book.availableCopies} disp.`
                              : 'Indisponivel'}
                          </span>
                        </button>
                      ))}
                      <button
                        onClick={() => {
                          setShowDropdown(false);
                          navigate(`/?q=${encodeURIComponent(searchInput.trim())}`);
                        }}
                        className="w-full px-4 py-2.5 text-center text-xs font-medium text-[#003399] hover:bg-slate-50 transition-colors cursor-pointer bg-transparent border-t border-slate-200"
                      >
                        Ver todos os resultados
                      </button>
                    </>
                  )}
                </div>
              </>
            )}
          </div>

          <div className="flex items-center gap-4">
            <Link
              to="/my-loans"
              className="relative p-2 text-slate-500 hover:text-slate-700 bg-slate-50 rounded-full border border-slate-200 cursor-pointer no-underline"
              title={
                notificationCount > 0
                  ? `${notificationCount} reserva(s) pronta(s) para retirada`
                  : 'Sem notificações'
              }
            >
              <Bell className="w-5 h-5" />
              {notificationCount > 0 && (
                <span className="absolute -top-1 -right-1 min-w-[18px] h-[18px] flex items-center justify-center bg-red-500 text-white text-[10px] font-bold rounded-full px-1">
                  {notificationCount}
                </span>
              )}
            </Link>
          </div>
        </header>

        <main className="flex-1 overflow-y-auto">{children}</main>
      </div>
    </div>
  );
}
