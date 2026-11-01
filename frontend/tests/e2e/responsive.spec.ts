import { expect, test } from "@playwright/test";

/** P06-07 / P17-06: sem rolagem horizontal em 360/390/768/1280, teclado e zoom de 200%. */
const WIDTHS = [360, 390, 768, 1280];
const PAGES = ["/", "/participar", "/regulamento", "/rankings", "/meus-numeros"];

for (const width of WIDTHS) {
  test(`sem rolagem horizontal em ${width}px`, async ({ page }) => {
    await page.setViewportSize({ width, height: 800 });
    for (const path of PAGES) {
      await page.goto(path);
      await page.waitForLoadState("networkidle");
      const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
      expect(overflow, `${path} em ${width}px`).toBeLessThanOrEqual(0);
    }
  });
}

test("zoom de 200% em 1280px não cria rolagem horizontal na home", async ({ page }) => {
  await page.setViewportSize({ width: 640, height: 800 });
  await page.goto("/");
  await page.waitForLoadState("networkidle");
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBeLessThanOrEqual(0);
});

test("CTA alcançável pelo teclado com foco visível", async ({ page }) => {
  await page.goto("/");
  await page.waitForLoadState("networkidle");
  let focusedCta = false;
  for (let i = 0; i < 40 && !focusedCta; i++) {
    await page.keyboard.press("Tab");
    focusedCta = await page.evaluate(() => {
      const el = document.activeElement;
      return el instanceof HTMLAnchorElement && el.getAttribute("href") === "/participar";
    });
  }
  expect(focusedCta).toBe(true);
  const outline = await page.evaluate(() => {
    const el = document.activeElement;
    return el ? getComputedStyle(el).outlineStyle : "none";
  });
  expect(outline).not.toBe("none");
});
