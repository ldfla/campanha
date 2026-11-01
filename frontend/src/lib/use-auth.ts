"use client";

import { useState, useEffect, useCallback, createContext, useContext } from "react";
import { auth, type UserResponse } from "./api-client";

interface AuthState {
  user: UserResponse | null;
  loading: boolean;
  error: string | null;
}

interface AuthContextValue extends AuthState {
  register: (firstName: string, surname: string, phoneNumber: string) => Promise<UserResponse>;
  login: (phoneNumber: string) => Promise<UserResponse>;
  logout: () => Promise<void>;
  refresh: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return ctx;
}

export { AuthContext };

export function useAuthProvider(): AuthContextValue {
  const [state, setState] = useState<AuthState>({
    user: null,
    loading: true,
    error: null,
  });

  const refresh = useCallback(async () => {
    try {
      const user = await auth.me();
      setState({ user, loading: false, error: null });
    } catch {
      setState({ user: null, loading: false, error: null });
    }
  }, []);

  useEffect(() => {
    let active = true;
    auth
      .me()
      .then((user) => {
        if (active) setState({ user, loading: false, error: null });
      })
      .catch(() => {
        if (active) setState({ user: null, loading: false, error: null });
      });
    return () => {
      active = false;
    };
  }, []);

  const register = useCallback(
    async (firstName: string, surname: string, phoneNumber: string) => {
      setState((s) => ({ ...s, loading: true, error: null }));
      try {
        const user = await auth.register({ firstName, surname, phoneNumber });
        setState({ user, loading: false, error: null });
        return user;
      } catch (err) {
        const message =
          err instanceof Error ? err.message : "Erro ao registrar";
        setState((s) => ({ ...s, loading: false, error: message }));
        throw err;
      }
    },
    [],
  );

  const login = useCallback(async (phoneNumber: string) => {
    setState((s) => ({ ...s, loading: true, error: null }));
    try {
      const user = await auth.login({ phoneNumber });
      setState({ user, loading: false, error: null });
      return user;
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "Erro ao entrar";
      setState((s) => ({ ...s, loading: false, error: message }));
      throw err;
    }
  }, []);

  const doLogout = useCallback(async () => {
    try {
      await auth.logout();
    } catch {
      /* best-effort */
    }
    setState({ user: null, loading: false, error: null });
  }, []);

  return {
    ...state,
    register,
    login,
    logout: doLogout,
    refresh,
  };
}
