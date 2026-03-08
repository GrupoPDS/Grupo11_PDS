import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { usePageTitle } from '../hooks/usePageTitle';

export default function RegisterPage() {
  usePageTitle('Cadastro');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { register } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (password !== confirmPassword) {
      setError('As senhas não coincidem');
      return;
    }

    if (password.length < 8) {
      setError('A senha deve ter no mínimo 8 caracteres');
      return;
    }

    setLoading(true);

    try {
      await register(name, email, password);
      navigate('/');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao criar conta');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-center items-center p-4">
      <div className="w-full max-w-md bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
        <div className="p-8">
          <div className="text-center mb-8">
            <div className="w-12 h-12 bg-[#003399] rounded-lg flex items-center justify-center text-white font-bold text-xl mx-auto mb-4 shadow-md">
              U
            </div>
            <h1 className="text-2xl font-bold text-slate-800">Biblioteca UFU</h1>
            <p className="text-slate-500 mt-2">Criar Nova Conta</p>
          </div>

          {error && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 rounded-md text-sm">
              {error}
            </div>
          )}

          <form className="space-y-5" onSubmit={handleSubmit}>
            <div>
              <label htmlFor="name" className="block text-sm font-medium text-slate-700 mb-1">
                Nome Completo
              </label>
              <input
                id="name"
                type="text"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="João da Silva"
                required
                disabled={loading}
                className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-md focus:bg-white focus:border-[#003399] focus:ring-1 focus:ring-[#003399] outline-none transition-all disabled:opacity-50"
              />
            </div>

            <div>
              <label htmlFor="email" className="block text-sm font-medium text-slate-700 mb-1">
                Email
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="aluno@ufu.br"
                required
                disabled={loading}
                className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-md focus:bg-white focus:border-[#003399] focus:ring-1 focus:ring-[#003399] outline-none transition-all disabled:opacity-50"
              />
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-slate-700 mb-1">
                Senha
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="Mínimo 8 caracteres"
                required
                disabled={loading}
                className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-md focus:bg-white focus:border-[#003399] focus:ring-1 focus:ring-[#003399] outline-none transition-all disabled:opacity-50"
              />
            </div>

            <div>
              <label
                htmlFor="confirmPassword"
                className="block text-sm font-medium text-slate-700 mb-1"
              >
                Confirmar Senha
              </label>
              <input
                id="confirmPassword"
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="Repita a senha"
                required
                disabled={loading}
                className="w-full px-4 py-2 bg-slate-50 border border-slate-200 rounded-md focus:bg-white focus:border-[#003399] focus:ring-1 focus:ring-[#003399] outline-none transition-all disabled:opacity-50"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-[#003399] hover:bg-blue-800 text-white font-medium py-2.5 rounded-md transition-colors mt-4 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {loading ? 'Criando...' : 'Criar Conta'}
            </button>
          </form>
        </div>

        <div className="bg-slate-50 border-t border-slate-100 p-4 text-center">
          <p className="text-sm text-slate-600 m-0">
            Já tem conta?{' '}
            <Link to="/login" className="font-semibold text-[#003399] hover:underline">
              Entrar
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
