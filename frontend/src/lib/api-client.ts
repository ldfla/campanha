const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080/api";

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly detail: string,
    public readonly type?: string,
  ) {
    super(detail);
    this.name = "ApiError";
  }
}

async function request<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const url = `${API_BASE}${path}`;
  const res = await fetch(url, {
    ...options,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      Accept: "application/json",
      ...options.headers,
    },
  });

  if (!res.ok) {
    let detail = res.statusText;
    let type: string | undefined;
    try {
      const body = (await res.json()) as Record<string, unknown>;
      if (typeof body.detail === "string") detail = body.detail;
      if (typeof body.type === "string") type = body.type;
    } catch {
      /* body is not JSON */
    }
    throw new ApiError(res.status, detail, type);
  }

  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

// ── Auth ──

export interface UserResponse {
  id: string;
  firstName: string;
  surname: string;
  phoneNumber: string;
  roles: string[];
  createdAt: string;
}

export interface RegisterRequest {
  firstName: string;
  surname: string;
  phoneNumber: string;
}

export interface LoginRequest {
  phoneNumber: string;
}

export const auth = {
  register: (data: RegisterRequest) =>
    request<UserResponse>("/auth/register", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  login: (data: LoginRequest) =>
    request<UserResponse>("/auth/login", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  logout: () =>
    request<void>("/auth/logout", { method: "POST" }),

  me: () => request<UserResponse>("/auth/me"),

  verifyContact: (phoneNumber: string, code: string) =>
    request<void>("/auth/verify-contact", {
      method: "POST",
      body: JSON.stringify({ phoneNumber, code }),
    }),
};

// ── Campaign ──

export interface CampaignResponse {
  id: string;
  title: string;
  slug: string;
  description: string;
  mode: string;
  status: string;
  allowedTransitions: string[];
  capabilities: string[];
  startsAt: string;
  endsAt: string;
  timezone: string;
  numberMin: number;
  numberMax: number;
  totalNumbers: number;
  createdAt: string;
  updatedAt: string;
}

export interface PrizeResponse {
  id: number;
  type: string;
  description: string;
  valueCents: number;
  currency: string;
  quantity: number;
  status: string;
  sortOrder: number;
  createdAt: string;
}

export const campaigns = {
  getBySlug: (slug: string) =>
    request<CampaignResponse>(`/campaigns/slug/${slug}`),

  getById: (id: string) =>
    request<CampaignResponse>(`/campaigns/${id}`),

  getPrizes: (id: string) =>
    request<PrizeResponse[]>(`/campaigns/${id}/prizes`),
};

// ── Participation ──

export interface ParticipationAllocation {
  entryId: string;
  numbersAssigned: number[];
  paymentOrderId: string;
}

export const participations = {
  request: (campaignId: string, idempotencyKey: string, count: number) =>
    request<ParticipationAllocation>(
      `/participations/campaigns/${campaignId}`,
      {
        method: "POST",
        body: JSON.stringify({ idempotencyKey, count }),
      },
    ),

  availability: (campaignId: string) =>
    request<number>(`/participations/campaigns/${campaignId}/availability`),

  numbers: (campaignId: string, page = 0, size = 100) =>
    request<PagedResponse<NumberAvailability>>(
      `/participations/campaigns/${campaignId}/numbers?page=${page}&size=${size}`,
    ),
};

// ── Rankings ──

export interface RankingPosition {
  rank: number;
  pseudonym: string;
  metricValue: number;
}

export interface RankingSnapshot {
  id: string;
  scope: string;
  positions: RankingPosition[];
  createdAt: string;
}

export const rankings = {
  getByScope: (campaignId: string, scope: string) =>
    request<RankingSnapshot>(
      `/rankings/campaigns/${campaignId}/${scope}`,
    ),

  listSnapshots: (campaignId: string) =>
    request<PagedResponse<RankingSnapshot>>(
      `/rankings/campaigns/${campaignId}/snapshots`,
    ),
};

// ── Payment ──

export interface PaymentOrderResponse {
  orderId: string;
  amountCents: number;
  currency: string;
  pixCode: string;
  pixQrCodeBase64: string;
  expiresAt: string;
  status: string;
}

export const payments = {
  createOrder: (amountCents: number, participationEntryId: string) =>
    request<PaymentOrderResponse>("/payments/orders", {
      method: "POST",
      body: JSON.stringify({ amountCents, participationEntryId }),
    }),
};

// ── Referrals ──

export interface ReferralLink {
  code: string;
  url: string;
}

export interface ReferralMetrics {
  totalClicks: number;
  totalAttributions: number;
}

export const referrals = {
  createLink: (campaignId: string) =>
    request<ReferralLink>(`/referrals/campaigns/${campaignId}/link`, {
      method: "POST",
    }),

  click: (campaignId: string, code: string) =>
    request<void>(`/referrals/campaigns/${campaignId}/click/${code}`, {
      method: "POST",
    }),

  metrics: (campaignId: string) =>
    request<ReferralMetrics>(`/referrals/campaigns/${campaignId}/metrics`),
};

// ── Shared types ──

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface NumberAvailability {
  numberValue: number;
  status: string;
  assignedToUserId: string | null;
}
