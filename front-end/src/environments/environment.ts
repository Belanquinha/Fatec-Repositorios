/**
 * Configuração do build de PRODUÇÃO (usado pelo `ng build` e pela imagem Docker).
 *
 * Nada aqui é lido de variável de ambiente. A distinção entre dev e produção é
 * apenas `apiUrl`, e ela não é configuração: é consequência de existir ou não um
 * reverse proxy na frente. Ver `environment.development.ts` para o `ng serve`.
 */

/** Tenant único da CPS (AD-3). O app é single-tenant, então o GUID é fixo. */
const MSAL_TENANT_ID = 'eabe64c5-68f5-4a76-8301-9577a679e449';

/** Application (client) ID do app registration no Microsoft Entra ID. */
const MSAL_CLIENT_ID = '146c36f9-abf3-48b0-a533-5f462e5e4eed';

export const environment = {
  production: true,

  /**
   * URL base da API.
   *
   * Relativa de propósito: o bundle só precisa saber que a API vive sob `/api`.
   * Quem decide para onde isso aponta é o `nginx.conf`, que faz
   * `location /api/` -> `proxy_pass http://backend:4040/` removendo o prefixo.
   * Com um caminho relativo, o mesmo artefato funciona atrás de qualquer proxy,
   * host ou prefixo, e o front-end não carrega uma segunda cópia da topologia
   * de deploy que possa divergir da do nginx.
   */
  apiUrl: '/api',

  /**
   * Identificador público, não segredo: todo SPA web o expõe no bundle. Fica
   * versionado para que o mesmo app registration seja usado em todos ambientes.
   */
  msalClientId: MSAL_CLIENT_ID,

  /**
   * Endpoint de discovery do tenant. Nunca `common`: o app é single-tenant.
   *
   * O sufixo `/v2.0` é obrigatório aqui, e a ausência dele era a causa do login nunca ter
   * funcionado. Sem ele o MSAL usa o endpoint v1 e entrega um token do Azure AD Graph v1
   * (`iss=https://sts.windows.net/{tid}/`, `aud=00000003-...-c000-000000000000`) — um recurso que
   * a Microsoft já depreciou. O back-end valida o token contra o JWKS de discovery **v2**
   * (`{tid}/discovery/v2.0/keys`), e o par v1/v2 é exatamente onde a assinatura deixava de bater.
   *
   * Com `/v2.0`, o token sai com `aud` do Microsoft Graph (`00000003-0000-0cc0-000000000000`), que
   * o `MicrosoftTokenVerifier` já aceita. As chamadas ao Graph seguem na API v1.0, que aceita token
   * v2 — só o formato do token muda, não o código de quem consome.
   */
  msalAuthority: `https://login.microsoftonline.com/${MSAL_TENANT_ID}/v2.0`,

  /**
   * Redirect URI do login e do logout.
   *
   * O Entra ID exige que esta URI esteja registrada, e divergir dela causa
   * `AADSTS50011`. Em vez de fixar um host (que quebraria em qualquer ambiente
   * que não seja exatamente aquele), derivamos de `document.baseURI` — que é o
   * `<base href>` do index.html, o mesmo valor que o roteador do Angular usa
   * para resolver rotas. Assim a URI enviada é sempre a página real em que a
   * pessoa está.
   */
  msalRedirectUri: document.baseURI,
};
