import { useEffect, useState, type ReactNode } from 'react';
import {
  loginApi,
  registerApi,
  logoutApi,
  getMeApi,
  setTokens,
  getSavedRefreshToken,
  type UserInfo,
} from '../services/api';
import { AuthContext } from './authContext';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserInfo | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const restore = async () => {
      const savedRefreshToken = getSavedRefreshToken();
      if (savedRefreshToken) {
        try {
          const me = await getMeApi();
          setUser(me);
        } catch {
          setTokens(null);
        }
      }
      setIsLoading(false);
    };
    restore();
  }, []);

  const login = async (email: string, password: string) => {
    const response = await loginApi(email, password);
    setTokens({
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
    });
    setUser(response.user);
  };

  const register = async (name: string, email: string, password: string) => {
    const response = await registerApi(name, email, password);
    setTokens({
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
    });
    setUser(response.user);
  };

  const logout = async () => {
    const refreshToken = getSavedRefreshToken();
    if (refreshToken) {
      await logoutApi(refreshToken).catch(() => {});
    }
    setTokens(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
