"use client";

import type { ReactNode } from "react";
import { CampaignStateContext, useCampaignStateLoader } from "@/lib/use-campaign-state";

export function CampaignStateProvider({ children }: { children: ReactNode }) {
  const value = useCampaignStateLoader();
  return <CampaignStateContext value={value}>{children}</CampaignStateContext>;
}
