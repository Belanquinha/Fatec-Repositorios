/**
 * Uma variável `NG_APP_*` definida como string vazia (ou só espaços) contorna o fallback do
 * `??` e entrega ao MSAL um authority/redirect vazio, que só falha no navegador. Aqui string
 * em branco é tratada como ausente, para que o padrão abaixo sempre valha.
 *
 * As chaves precisam continuar literais: o `@ngx-env/builder` só substitui `import.meta.env['CHAVE']`
 * escrito estaticamente.
 */
function definido(valor: string | undefined): string | undefined {
  return typeof valor === 'string' && valor.trim() !== '' ? valor : undefined;
}

export const environment = {
  production: false,

  // Azure AD/Entra ID - sobrescritos via .env (NG_APP_*)
  msalClientId: definido(import.meta.env['NG_APP_MSAL_CLIENT_ID']) ?? '',
  msalAuthority: definido(import.meta.env['NG_APP_MSAL_AUTHORITY'])
    ?? 'https://login.microsoftonline.com/eabe64c5-68f5-4a76-8301-9577a679e449',
  msalRedirectUri: definido(import.meta.env['NG_APP_MSAL_REDIRECT_URI'])
    ?? 'http://localhost:4200/',

  // URL base da API Spring Boot
  apiUrl: definido(import.meta.env['NG_APP_API_URL']) ?? 'http://localhost:4040'
};
