import { type ReactNode, useEffect, useState, useCallback } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Search, Book, ArrowLeft, AlertCircle, Bookmark, LogOut, User, Bell } from 'lucide-react';
import { useAuth } from '../hooks/useAuth';
import { api } from '../services/api';

interface DashboardLayoutProps {
  children: ReactNode;
}

export default function DashboardLayout({ children }: DashboardLayoutProps) {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const canManage = user?.role === 'ADMIN' || user?.role === 'LIBRARIAN';

  const [notificationCount, setNotificationCount] = useState(0);

  const fetchNotifications = useCallback(async () => {
    if (!user) return;
    try {
      const res = await api('/reservations/my/notifications');
      if (res.ok) {
        const data = await res.json();
        setNotificationCount(data.availableForPickup || 0);
      }
    } catch {
      // silently fail
    }
  }, [user]);

  useEffect(() => {
    fetchNotifications();
    const interval = setInterval(fetchNotifications, 30000); // poll every 30s
    return () => clearInterval(interval);
  }, [fetchNotifications]);

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
            <Search className="w-4 h-4 text-slate-400 absolute left-3" />
            <input
              type="text"
              placeholder="Pesquisar no sistema..."
              className="w-full pl-10 pr-4 py-2 bg-slate-100 border-transparent rounded-md text-sm focus:bg-white focus:border-[#003399] focus:ring-1 focus:ring-[#003399] outline-none transition-all"
            />
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
