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

  verifyMfa: (phoneNumber: string, code: string) =>
    request<UserResponse>("/auth/mfa/verify", {
      method: "POST",
      body: JSON.stringify({ phoneNumber, code }),
    }),
};

// ── Campaign ──

export type CampaignMode = "DEMO" | "COMMERCIAL_PROMOTION" | "PHILANTHROPIC_AUTHORIZED";
export type CampaignStatus =
  | "DRAFT" | "LEGAL_REVIEW" | "AUTHORIZED" | "SCHEDULED"
  | "OPEN" | "LOCKED" | "DRAW_PENDING" | "DRAWN"
  | "AWARDS_PENDING" | "CLOSED" | "SUSPENDED" | "CANCELLED";

export interface CampaignResponse {
  id: string;
  title: string;
  slug: string;
  description: string;
  mode: CampaignMode;
  status: CampaignStatus;
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

export interface AssignedNumber {
  entryPublicId: string;
  seriesCode: string;
  numberValue: number;
  formattedNumber: string;
}

export interface ParticipationAllocation {
  requestPublicId: string;
  numbers: AssignedNumber[];
}

export interface CreateParticipationRequest {
  idempotencyKey: string;
  count: number;
}

export interface NumberAvailability {
  numberValue: number;
  status: string;
  assignedToUserId: string | null;
}

export const participations = {
  request: (campaignId: string, idempotencyKey: string, count: number) =>
    request<ParticipationAllocation>(
      `/participations/campaigns/${campaignId}`,
      {
        method: "POST",
        body: JSON.stringify({ idempotencyKey, count } satisfies CreateParticipationRequest),
      },
    ),

  availability: (campaignId: string) =>
    request<number>(`/participations/campaigns/${campaignId}/availability`),

  numbers: (campaignId: string, page = 0, size = 100) =>
    request<PagedResponse<NumberAvailability>>(
      `/participations/campaigns/${campaignId}/numbers?page=${page}&size=${size}`,
    ),

  // ── Future endpoints (not yet implemented in backend) ──

  offers: (campaignId: string) =>
    request<OfferResponse[]>(
      `/participations/campaigns/${campaignId}/offers`,
    ),

  beneficiaries: (campaignId: string) =>
    request<BeneficiaryResponse[]>(
      `/participations/campaigns/${campaignId}/beneficiaries`,
    ),

  checkout: (campaignId: string, data: CheckoutRequest) =>
    request<CheckoutResponse>(
      `/participations/campaigns/${campaignId}/checkout`,
      {
        method: "POST",
        body: JSON.stringify(data),
      },
    ),

  confirmCheckout: (campaignId: string, requestPublicId: string) =>
    request<CheckoutResponse>(
      `/participations/campaigns/${campaignId}/checkout/${requestPublicId}/confirm`,
      { method: "POST" },
    ),
};

export interface OfferResponse {
  id: string;
  quantity: number;
  totalCents: number;
  currency: string;
}

export interface BeneficiaryResponse {
  id: string;
  name: string;
}

export interface CheckoutRequest {
  idempotencyKey: string;
  offerId: string;
  entityId: string;
  referralCode?: string;
}

export interface CheckoutResponse {
  requestPublicId: string;
  expiresAt: string;
  numbers: string[];
  orderPublicId: string;
  amountCents: number;
  currency: string;
  simulation: boolean;
}

// ── Mock Payment (local profile only) ──

export const mockPayments = {
  settle: (orderPublicId: string) =>
    request<void>(`/payments/mock/orders/${orderPublicId}/settle`, {
      method: "POST",
    }),
};

// ── Payment ──

export interface CreatePaymentOrderRequest {
  campaignId: number;
  amountCents: number;
  idempotencyKey: string;
}

export interface OrderReceipt {
  orderPublicId: string;
  paymentId: number;
  chargeId: string;
  txid: string;
  status: string;
}

export type OrderStatus =
  | "CREATED" | "AWAITING_PAYMENT" | "CONFIRMED" | "RECONCILED"
  | "CANCELLED" | "EXPIRED" | "REFUND_PENDING" | "REFUNDED";

export const payments = {
  createOrder: (campaignId: number, amountCents: number, idempotencyKey: string) =>
    request<OrderReceipt>("/payments/orders", {
      method: "POST",
      body: JSON.stringify({ campaignId, amountCents, idempotencyKey } satisfies CreatePaymentOrderRequest),
    }),
};

// ── Ranking ──

export type RankingScope = "PROMOTER" | "PARTICIPANT";
export type RankingMetric = "REFERRAL_CONFIRMED_PARTICIPATIONS" | "CONFIRMED_PARTICIPATIONS";

export interface RankingRow {
  position: number;
  pseudonym: string;
  score: number;
  firstEventAt: string;
}

export interface RankingView {
  campaignPublicId: string;
  campaignTitle: string;
  scope: RankingScope;
  metric: RankingMetric;
  ruleVersion: number;
  tieBreaker: string;
  cutoffAt: string;
  generatedAt: string;
  consistency: string;
  simulation: boolean;
  awardsEligible: boolean;
  notice: string | null;
  entries: RankingRow[];
}

export interface SnapshotView {
  snapshotId: string;
  campaignPublicId: string;
  scope: RankingScope;
  metric: RankingMetric;
  ruleVersion: number;
  cutoffAt: string;
  generatedAt: string;
  snapshotHash: string;
  entryCount: number;
}

export const rankings = {
  current: (campaignId: string, scope: RankingScope, limit = 50) =>
    request<RankingView>(
      `/rankings/campaigns/${campaignId}/${scope}?limit=${limit}`,
    ),

  listSnapshots: (campaignId: string, scope: RankingScope) =>
    request<SnapshotView[]>(
      `/rankings/campaigns/${campaignId}/snapshots?scope=${scope}`,
    ),

  getSnapshot: (campaignId: string, snapshotId: string) =>
    request<SnapshotView>(
      `/rankings/campaigns/${campaignId}/snapshots/${snapshotId}`,
    ),
};

// ── Referral ──

export interface ReferralMetrics {
  code: string;
  clicks: number;
  participations: number;
}

export const referrals = {
  createLink: (campaignId: string) =>
    request<ReferralMetrics>(`/referrals/campaigns/${campaignId}/link`, {
      method: "POST",
    }),

  click: (campaignId: string, code: string, anonymousId: string) =>
    request<void>(`/referrals/campaigns/${campaignId}/click/${code}?anonymousId=${encodeURIComponent(anonymousId)}`, {
      method: "POST",
    }),

  attribute: (campaignId: string, code: string, entryId: string) =>
    request<void>(`/referrals/campaigns/${campaignId}/attributions/${code}/${entryId}`, {
      method: "POST",
    }),

  metrics: (campaignId: string) =>
    request<ReferralMetrics>(`/referrals/campaigns/${campaignId}/metrics`),
};

// ── Drawing (Apuração) — backend not yet implemented ──

export interface DrawOutcome {
  prizeTag: string;
  winningNumber: number;
  entryId: string | null;
  decisionCode: string;
}

export interface DrawTransparencyResponse {
  status: string;
  ruleVersion: string;
  formulaJson: string;
  entriesHash: string;
  contestIdentifier: string | null;
  sourceDocumentHash: string | null;
  sourceUrl: string | null;
  resultPayload: string | null;
  outcomes: DrawOutcome[];
}

export const drawings = {
  transparency: (campaignId: string, drawPublicId: string) =>
    request<DrawTransparencyResponse>(
      `/drawings/campaigns/${campaignId}/draws/${drawPublicId}/transparency`,
    ),
};

// ── Prize Load (Transparency) — backend not yet implemented ──

export interface PrizeLoadResponse {
  numberMin: number;
  numberMax: number;
  tier1Count: number;
  tier1Cents: number;
  tier2Count: number;
  tier2Cents: number;
  definedAt: string;
  fileSha256: string;
  generatedAt: string;
}

export interface PrizeLoadVerifyResponse {
  storedSha256: string;
  computedSha256: string;
  match: boolean;
  definedAt: string;
}

export const prizeLoad = {
  get: (campaignId: string) =>
    request<PrizeLoadResponse>(
      `/participations/campaigns/${campaignId}/numbers/prize-load`,
    ),

  exportCsvUrl: (campaignId: string) =>
    `${API_BASE}/participations/campaigns/${campaignId}/numbers/prize-load/export`,

  verify: (campaignId: string) =>
    request<PrizeLoadVerifyResponse>(
      `/participations/campaigns/${campaignId}/numbers/prize-load/verify`,
    ),
};

// ── Surprise Commitment — backend not yet implemented ──

export interface SurpriseCommitmentResponse {
  commitment: string;
  definedAt: string;
  campaignId: string;
}

export const surprises = {
  commitment: (campaignId: string) =>
    request<SurpriseCommitmentResponse>(
      `/surprises/campaigns/${campaignId}/commitment`,
    ),
};

// ── Shared types ──

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
