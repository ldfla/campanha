"use client";

import { useState, useEffect, useCallback, createContext, useContext } from "react";
import { auth, type CodeChallengeResponse, type UserResponse } from "./api-client";

interface AuthState {
  user: UserResponse | null;
  loading: boolean;
  error: string | null;
}

interface AuthContextValue extends AuthState {
  /** Cadastra e solicita código; não autentica. */
  register: (firstName: string, surname: string, phoneNumber: string) => Promise<CodeChallengeResponse>;
  /** Solicita código de acesso; não autentica nem revela se o telefone existe. */
  requestCode: (phoneNumber: string) => Promise<CodeChallengeResponse>;
  /** Verifica o código recebido e só então cria a sessão. */
  verifyCode: (phoneNumber: string, code: string) => Promise<UserResponse>;
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
    (firstName: string, surname: string, phoneNumber: string) =>
      auth.register({ firstName, surname, phoneNumber }),
    [],
  );

  const requestCode = useCallback(
    (phoneNumber: string) => auth.login({ phoneNumber }),
    [],
  );

  const verifyCode = useCallback(async (phoneNumber: string, code: string) => {
    setState((s) => ({ ...s, loading: true, error: null }));
    try {
      const user = await auth.verifyLogin(phoneNumber, code);
      setState({ user, loading: false, error: null });
      return user;
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "Erro ao verificar o código";
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
    requestCode,
    verifyCode,
    logout: doLogout,
    refresh,
  };
}
