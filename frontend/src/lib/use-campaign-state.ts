"use client";

import { createContext, useContext, useEffect, useState } from "react";
import { campaigns, type CtaState, type PublicCampaignState } from "./api-client";
import { DEMO_CAMPAIGN_ID } from "./campaign-config";

export interface CampaignStateValue {
  state: PublicCampaignState | null;
  loading: boolean;
  /** Só o servidor habilita a compra; carregando ou com erro, o CTA fica bloqueado. */
  purchaseEnabled: boolean;
  ctaState: CtaState | "LOADING" | "UNAVAILABLE";
}

const FAIL_CLOSED: CampaignStateValue = {
  state: null,
  loading: true,
  purchaseEnabled: false,
  ctaState: "LOADING",
};

export const CampaignStateContext = createContext<CampaignStateValue>(FAIL_CLOSED);

export function useCampaignStateLoader(): CampaignStateValue {
  const [value, setValue] = useState<CampaignStateValue>(FAIL_CLOSED);

  useEffect(() => {
    let active = true;
    campaigns
      .publicState(DEMO_CAMPAIGN_ID)
      .then((state) => {
        if (!active) return;
        setValue({ state, loading: false, purchaseEnabled: state.purchaseEnabled, ctaState: state.ctaState });
      })
      .catch(() => {
        if (active) setValue({ state: null, loading: false, purchaseEnabled: false, ctaState: "UNAVAILABLE" });
      });
    return () => {
      active = false;
    };
  }, []);

  return value;
}

export function useCampaignState(): CampaignStateValue {
  return useContext(CampaignStateContext);
}

const LABELS: Record<CampaignStateValue["ctaState"], string> = {
  OPEN: "Construir junto",
  LOADING: "Carregando…",
  NOT_STARTED: "Em breve",
  SOLD_OUT: "Esgotado",
  CLOSED: "Participações encerradas",
  SUSPENDED: "Campanha suspensa",
  BLOCKED: "Indisponível no momento",
  UNAVAILABLE: "Indisponível no momento",
};

export function ctaLabel(ctaState: CampaignStateValue["ctaState"]): string {
  return LABELS[ctaState];
}
