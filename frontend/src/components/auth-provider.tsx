"use client";

import type { ReactNode } from "react";
import { AuthContext, useAuthProvider } from "@/lib/use-auth";

export function AuthProvider({ children }: { children: ReactNode }) {
  const auth = useAuthProvider();
  return <AuthContext value={auth}>{children}</AuthContext>;
}
