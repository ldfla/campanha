import { expect, test, type Page } from "@playwright/test";

/** P06-08 / P17-05: o CTA segue o estado decidido pelo servidor e falha fechado. */
async function stubState(page: Page, body: Record<string, unknown> | null, status = 200): Promise<void> {
  await page.route("**/api/campaigns/public/**", (route) =>
    body === null
      ? route.fulfill({ status, contentType: "application/problem+json", body: "{}" })
      : route.fulfill({ status, contentType: "application/json", body: JSON.stringify(body) }),
  );
}

const base = { id: "c", title: "Campanha", mode: "DEMO", simulation: true, availableNumbers: 10, endsAt: null };

test("campanha aberta libera o CTA para /participar", async ({ page }) => {
  await page.goto("/");
  const cta = page.getByRole("link", { name: /Construir junto/ }).first();
  await expect(cta).toBeVisible();
  await expect(cta).toHaveAttribute("href", "/participar");
});

for (const [ctaState, label] of [
  ["SOLD_OUT", "Esgotado"],
  ["SUSPENDED", "Campanha suspensa"],
  ["BLOCKED", "Indisponível no momento"],
  ["NOT_STARTED", "Em breve"],
] as const) {
  test(`estado ${ctaState} bloqueia todos os CTAs`, async ({ page }) => {
    await stubState(page, { ...base, ctaState, purchaseEnabled: false });
    await page.goto("/");
    await expect(page.getByRole("status").filter({ hasText: label }).first()).toBeVisible();
    await expect(page.locator('a[href^="/participar"]')).toHaveCount(0);
  });
}

test("falha ao ler o estado bloqueia a compra", async ({ page }) => {
  await stubState(page, null, 500);
  await page.goto("/");
  await expect(page.getByRole("status").filter({ hasText: "Indisponível no momento" }).first()).toBeVisible();
  await expect(page.locator('a[href^="/participar"]')).toHaveCount(0);
});
