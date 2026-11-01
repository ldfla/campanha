import { defineConfig, devices } from "@playwright/test";

/**
 * E2E contra o ambiente local DEMO já em execução (docs/operations/local-mvp.md): backend com seed e CORS
 * para :3000, frontend com NEXT_PUBLIC_CAMPAIGN_ID da campanha com ofertas. Nada aqui toca serviço externo.
 */
export default defineConfig({
  testDir: "./tests/e2e",
  fullyParallel: false,
  workers: 1,
  retries: 0,
  reporter: [["list"]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://localhost:3000",
    locale: "pt-BR",
    timezoneId: "America/Sao_Paulo",
    trace: "retain-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
