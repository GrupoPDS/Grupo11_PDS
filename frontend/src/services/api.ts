const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

let tokens: AuthTokens | null = null;
let refreshPromise: Promise<string> | null = null;

export function setTokens(newTokens: AuthTokens | null) {
  tokens = newTokens;
  if (newTokens) {
    localStorage.setItem('refreshToken', newTokens.refreshToken);
  } else {
    localStorage.removeItem('refreshToken');
  }
}

export function getAccessToken(): string | null {
  return tokens?.accessToken ?? null;
}

export function getSavedRefreshToken(): string | null {
  return localStorage.getItem('refreshToken');
}

async function refreshAccessToken(): Promise<string> {
  const refreshToken = tokens?.refreshToken ?? getSavedRefreshToken();
  if (!refreshToken) throw new Error('No refresh token');

  const res = await fetch(`${API_BASE}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  });

  if (!res.ok) {
    setTokens(null);
    throw new Error('Refresh failed');
  }

  const data = await res.json();
  setTokens({ accessToken: data.accessToken, refreshToken: data.refreshToken });
  return data.accessToken;
}

export async function api(endpoint: string, options: RequestInit = {}): Promise<Response> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };

  if (tokens?.accessToken) {
    headers['Authorization'] = `Bearer ${tokens.accessToken}`;
  }

  let res = await fetch(`${API_BASE}${endpoint}`, { ...options, headers });

  // Se 401 e temos refresh token, tenta renovar
  if (res.status === 401 && (tokens?.refreshToken || getSavedRefreshToken())) {
    try {
      // Evita múltiplos refreshes simultâneos
      if (!refreshPromise) {
        refreshPromise = refreshAccessToken();
      }
      const newToken = await refreshPromise;
      refreshPromise = null;

      headers['Authorization'] = `Bearer ${newToken}`;
      res = await fetch(`${API_BASE}${endpoint}`, { ...options, headers });
    } catch {
      refreshPromise = null;
      // Refresh falhou — vai retornar 401
    }
  }

  return res;
}

// Auth API calls
export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  user: UserInfo;
}

export interface UserInfo {
  id: number;
  name: string;
  email: string;
  role: string;
  createdAt: string;
  updatedAt: string;
}

export async function loginApi(email: string, password: string): Promise<AuthResponse> {
  const res = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.error || 'Credenciais inválidas');
  }

  return res.json();
}

export async function registerApi(
  name: string,
  email: string,
  password: string,
): Promise<AuthResponse> {
  const res = await fetch(`${API_BASE}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, email, password }),
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.error || 'Erro ao registrar');
  }

  return res.json();
}

export async function logoutApi(refreshToken: string): Promise<void> {
  await fetch(`${API_BASE}/auth/logout`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${tokens?.accessToken}`,
    },
    body: JSON.stringify({ refreshToken }),
  });
}

export async function getMeApi(): Promise<UserInfo> {
  const res = await api('/auth/me');
  if (!res.ok) throw new Error('Não autenticado');
  return res.json();
}
