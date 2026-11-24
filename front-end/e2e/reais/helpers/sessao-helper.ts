/**
 * Garante navegador "limpo" no início de cada teste real.
 *
 * Cuidado com a forma: `page.addInitScript` não roda uma vez por teste — ele é
 * reexecutado em **toda** full page navigation. Um `localStorage.clear()` dentro
 * dele apagava a sessão recém-criada pelo login assim que o teste fazia o
 * segundo `page.goto('/perfil')` ou `/projeto-forms`, e o `authGuard` devolvia
 * `/entrar?returnUrl=...`. Por isso a limpeza é feita aqui, depois da primeira
 * navegação, e nunca via init script.
 */
export async function limparSessao(page: import('@playwright/test').Page): Promise<void> {
  await page.goto('/');
  await page.evaluate(() => window.localStorage.clear());
}
