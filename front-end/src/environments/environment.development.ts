/**
 * Configuração do build de DESENVOLVimento (usado pelo `ng serve`).
 *
 * Idêntica à de produção, com uma única diferença: `apiUrl`. Em dev não há
 * nginx na frente, então o Angular fala direto com o Spring Boot na porta 4040
 * via CORS. Em produção o mesmo caminho relativo `/api` é traduzido pelo nginx.
 * Essa é a razão de `apiUrl` ser a única diferença entre os dois arquivos — e
 * também a razão de não ser configurável por variável de ambiente: o valor
 * correto é determinado pela topologia, não por quem constrói.
 */

/** Tenant único da CPS (AD-3). O app é single-tenant, então o GUID é fixo. */
const MSAL_TENANT_ID = 'eabe64c5-68f5-4a76-8301-9577a679e449';

/** Application (client) ID do app registration no Microsoft Entra ID. */
const MSAL_CLIENT_ID = '146c36f9-abf3-48b0-a533-5f462e5e4eed';

/** `server.port` do back-end (back-end/src/main/resources/application.yml). */
const API_LOCAL = 'http://localhost:4040';

export const environment = {
  production: false,

  apiUrl: API_LOCAL,

  msalClientId: MSAL_CLIENT_ID,

  msalAuthority: `https://login.microsoftonline.com/${MSAL_TENANT_ID}/v2.0`,

  /**
   * Ver `environment.ts`. Em dev isto resolve para `http://localhost:4200/`,
   * que é a URI registrada no Entra ID para desenvolvimento.
   */
  msalRedirectUri: document.baseURI,
};
