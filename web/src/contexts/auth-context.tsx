"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { services } from "@/services";
import type { Services } from "@/services/contracts";
import type { Usuario } from "@/types/domain";
import { ApiError, errorMessage } from "@/lib/errors";

type SessionState = {
  status: "loading" | "ready" | "error";
  user: Usuario | null;
  error: string;
};
interface AuthContextValue extends SessionState {
  api: Services;
  refresh(): Promise<void>;
  setUser(user: Usuario | null): void;
  handleError(error: unknown): void;
}
const Context = createContext<AuthContextValue | null>(null);

export function AuthProvider({
  children,
  service = services,
}: {
  children: ReactNode;
  service?: Services;
}) {
  const [session, setSession] = useState<SessionState>({
    status: "loading",
    user: null,
    error: "",
  });
  const refresh = useCallback(async () => {
    try {
      setSession({ status: "ready", user: await service.auth.me(), error: "" });
    } catch (error) {
      setSession({ status: "error", user: null, error: errorMessage(error) });
    }
  }, [service]);
  useEffect(() => {
    let active = true;
    service.auth
      .me()
      .then((user) => {
        if (active) setSession({ status: "ready", user, error: "" });
      })
      .catch((error) => {
        if (active)
          setSession({
            status: "error",
            user: null,
            error: errorMessage(error),
          });
      });
    return () => {
      active = false;
    };
  }, [service]);
  const setUser = useCallback(
    (user: Usuario | null) => setSession({ status: "ready", user, error: "" }),
    [],
  );
  const handleError = useCallback(
    (error: unknown) => {
      if (error instanceof ApiError && error.status === 401) setUser(null);
    },
    [setUser],
  );
  return (
    <Context.Provider
      value={{ ...session, api: service, refresh, setUser, handleError }}
    >
      {children}
    </Context.Provider>
  );
}
export function useAuth() {
  const context = useContext(Context);
  if (!context) throw new Error("Use AuthProvider para acessar a sessão.");
  return context;
}
