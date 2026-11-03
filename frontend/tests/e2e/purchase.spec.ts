import { expect, test } from "@playwright/test";
import { DEMO_CODE, settleAsOperator, uniquePhone } from "./support";

/**
 * P17-01 / P17-07: visitante escolhe a oferta do servidor, cadastra com código, reserva, o operador liquida o
 * mock e o participante confirma; valores em BRL e números com cinco dígitos.
 */
test("compra simulada ponta a ponta até Meus números", async ({ page }) => {
  const phone = uniquePhone();
  await page.goto("/participar");

  const offer = page.getByRole("button", { name: /R\$\s?1,00/ }).first();
  await expect(offer).toBeVisible();
  await offer.click();
  await page.getByRole("button", { name: "Continuar" }).click();

  await expect(page.getByRole("heading", { name: "Escolha a instituição" })).toBeVisible();
  await page.getByRole("button", { name: /Organização/ }).first().click();
  await page.getByRole("button", { name: "Continuar" }).click();

  await page.getByLabel("Nome", { exact: true }).fill("Teste");
  await page.getByLabel("Sobrenome").fill("Ponta");
  await page.getByLabel("WhatsApp").fill(phone.replace("+55", ""));
  await page.getByRole("button", { name: /Continuar|Receber código|Enviar código/ }).click();
  await page.getByLabel("Código de verificação").fill(DEMO_CODE);

  const checkout = page.waitForResponse((res) => res.url().includes("/checkout") && res.request().method() === "POST");
  await page.getByRole("button", { name: "Ir para pagamento" }).click();
  const response = await checkout;
  expect(response.status(), await response.text()).toBe(201);
  const body = (await response.json()) as { orderPublicId: string; numbers: string[]; amountCents: number };

  await expect(page.getByRole("heading", { name: "Pagamento via Pix" })).toBeVisible();
  await expect(page.getByText("R$ 1,00").first()).toBeVisible();
  expect(body.amountCents).toBe(100);
  for (const number of body.numbers) {
    expect(number).toMatch(/^\d{5}$/);
    await expect(page.getByText(number, { exact: true })).toBeVisible();
  }

  await page.getByRole("button", { name: "Verificar pagamento" }).click();
  await expect(page.getByText(/ainda não foi confirmado pelo provedor/)).toBeVisible();

  await settleAsOperator(body.orderPublicId);
  await page.getByRole("button", { name: "Verificar pagamento" }).click();
  await expect(page.getByText("Pagamento confirmado. Seus números estão garantidos.")).toBeVisible();

  await page.goto("/meus-numeros");
  for (const number of body.numbers) {
    await expect(page.getByText(number, { exact: true }).first()).toBeVisible();
  }
});
