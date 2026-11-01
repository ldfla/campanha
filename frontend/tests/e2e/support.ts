import { request, type APIRequestContext } from "@playwright/test";

export const API_URL = process.env.E2E_API_URL ?? "http://localhost:8080/api";
export const DEMO_CODE = "000000";
const ADMIN_PHONE = "+5511999990000";

/** Telefone E.164 único por execução para não colidir com cadastros anteriores do banco local. */
export function uniquePhone(): string {
  const tail = `${Date.now()}`.slice(-8);
  return `+55119${tail}`;
}

/** Sessão do admin do seed local (código e MFA DEMO), usada só para o que o participante não pode fazer. */
export async function adminApi(): Promise<APIRequestContext> {
  const api = await request.newContext({ baseURL: `${API_URL}/` });
  await api.post("auth/login", { data: { phoneNumber: ADMIN_PHONE } });
  await api.post("auth/login/verify", { data: { phoneNumber: ADMIN_PHONE, code: DEMO_CODE } });
  const mfa = await api.post("auth/mfa/verify", { data: { phoneNumber: ADMIN_PHONE, code: DEMO_CODE } });
  if (!mfa.ok()) throw new Error(`MFA do admin local falhou: ${mfa.status()}`);
  await api.get("auth/me");
  return api;
}

export async function settleAsOperator(orderPublicId: string): Promise<void> {
  const api = await adminApi();
  const state = await api.storageState();
  const xsrf = state.cookies.find((cookie) => cookie.name === "XSRF-TOKEN")?.value;
  const response = await api.post(`payments/mock/orders/${orderPublicId}/settle`, {
    headers: xsrf ? { "X-XSRF-TOKEN": decodeURIComponent(xsrf) } : {},
  });
  await api.dispose();
  if (!response.ok()) throw new Error(`Liquidação mock falhou: ${response.status()}`);
}
