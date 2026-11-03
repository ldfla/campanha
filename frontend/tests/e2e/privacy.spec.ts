import { expect, test } from "@playwright/test";

/** "Meus números" sem login não pode mostrar nem buscar nada; ranking público só com pseudônimo. */
test("Meus números sem sessão não chama a API de números", async ({ page }) => {
  const calls: string[] = [];
  page.on("request", (req) => {
    if (req.url().includes("/numbers")) calls.push(req.url());
  });
  await page.goto("/meus-numeros");
  await page.waitForLoadState("networkidle");
  expect(calls).toEqual([]);
  await expect(page.getByText(/\b\d{5}\b/)).toHaveCount(0);
});

test("ranking público não expõe telefone completo", async ({ page }) => {
  await page.goto("/rankings");
  await page.waitForLoadState("networkidle");
  const text = await page.locator("body").innerText();
  expect(text).not.toMatch(/\+55\s?\d{2}\s?\d{4,5}-?\d{4}/);
});
