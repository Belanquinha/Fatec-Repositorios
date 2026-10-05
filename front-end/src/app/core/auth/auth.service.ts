import { Injectable, Inject } from '@angular/core';
import { MSAL_INSTANCE } from '@azure/msal-angular';
import { AccountInfo, IPublicClientApplication, RedirectRequest } from '@azure/msal-browser';
import { environment } from '../../../environments/environment';
import { UsuarioLogado } from './models/usuario-logado';
import { gravarSessao, lerChave, lerTokenValido, limparSessao } from './sessao-navegador';

/**
 * Escopo do Microsoft Graph, escrito por extenso de propósito.
 *
 * <p>A forma curta `User.Read` é ambígua entre as duas gerações da API: o MSAL pode resolvê-la
 * para o Azure AD Graph v1 (`00000003-0000-0000-c000-000000000000`), e esse recurso emite token
 * `ver=1.0` com `iss=https://sts.windows.net/{tid}/`. O back-end valida contra o JWKS de discovery
 * **v2**, cuja assinatura não é a desse token — daí "Token Microsoft inválido".
 *
 * <p>Nomear o recurso Graph v2 na URL elimina a ambiguidade: o token só pode sair com
 * `aud=00000003-0000-0cc0-000000000000`. Vale notar que `openid`, `profile` e `email` não são
 * pedidos aqui — o MSAL acrescenta os escopos OIDC sozinho, em qualquer fluxo.
 */
const GRAPH_SCOPES = ['https://graph.microsoft.com/User.Read'];

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  /**
   * MSAL Browser v3+ exige `await initialize()` antes de qualquer acesso ao cache. A promessa é
   * memorizada para que `inicializar()` seja idempotente e possa ser aguardado por qualquer
   * componente, independentemente de quem chegar primeiro.
   */
  private pronto?: Promise<void>;

  /**
   * Separado de `pronto` de propósito: `pronto` registra que a inicialização *foi tentada*,
   * enquanto isto registra que ela *concluiu*. Ler `getAllAccounts()` sobre uma instância cujo
   * `initialize()` falhou lança `uninitialized_public_client_application`.
   */
  private inicializadoComSucesso = false;

  constructor(@Inject(MSAL_INSTANCE) private instance: IPublicClientApplication) {}

  async inicializar(): Promise<void> {
    this.pronto ??= this.executarInicializacao();
    const tentativa = this.pronto;

    try {
      await tentativa;
    } catch (erro) {
      // Libera a memorização para que uma chamada posterior possa tentar de novo, em vez de
      // repetir a mesma rejeição pelo resto da vida da página.
      if (this.pronto === tentativa) {
        this.pronto = undefined;
        this.inicializadoComSucesso = false;
      }
      throw erro;
    }
  }

  async quandoPronto(): Promise<void> {
    await (this.pronto ?? this.inicializar());
  }

  get conta(): AccountInfo | undefined {
    if (!this.inicializadoComSucesso) {
      return undefined;
    }
    return this.instance.getAllAccounts()[0] ?? undefined;
  }

  private async executarInicializacao(): Promise<void> {
    await this.instance.initialize();
    await this.inicializado();
    this.inicializadoComSucesso = true;
  }

  private async inicializado(): Promise<void> {
    try {
      await this.instance.handleRedirectPromise();
    } catch (e: any) {
      if (e?.errorCode === 'no_token_request_cache_error') {
        Object.keys(sessionStorage)
          .filter((k) => k.startsWith('msal.'))
          .forEach((k) => sessionStorage.removeItem(k));
      } else {
        throw e;
      }
    }
  }

  /**
   * A identidade exibida na UI vem exclusivamente do backend: só existe usuário
   * logado quando há token de acesso guardado. O cache do MSAL indica apenas que
   * a Microsoft autenticou a pessoa no navegador — se o `/auth/login-microsoft`
   * falhou (backend fora do ar, token expirado, conta não provisionada), mostrar
   * um perfil ali seria mentir sobre o estado da sessão.
   *
   * A chave existir no `localStorage` não é prova de nada: o JWT tem validade e o
   * `localStorage` não expira nada. Ler o `exp` é o que separa "sessão viva" de
   * "chave de um token que venceu ontem" — sem isso a UI anunciaria login e toda
   * chamada autenticada responderia 401.
   */
  async obterUsuarioLogado(): Promise<UsuarioLogado | null> {
    if (!lerTokenValido()) {
      return null;
    }

    const nome = lerChave('usuarioNome');
    const email = lerChave('usuarioEmail');
    if (!nome || !email) {
      return null;
    }

    const foto = lerChave('usuarioFoto');
    const role = lerChave('usuarioRole');

    return {
      nome,
      email,
      foto: foto || undefined,
      role: role || undefined,
    };
  }

  async loginMicrosoft(): Promise<void> {
    // O link de login aparece antes da inicialização resolver; sem esta espera o
    // `loginRedirect` roda sobre uma instância não inicializada.
    await this.quandoPronto();

    const request: RedirectRequest = {
      scopes: GRAPH_SCOPES,
      authority: environment.msalAuthority,
      redirectUri: environment.msalRedirectUri,
    };

    await this.instance.loginRedirect(request);
  }

  async loginMicrosoftViaApi(): Promise<{ accessToken: string; tokenType: string; expiresInSeconds: number }> {
    const conta = this.conta;
    if (!conta) {
      throw new Error('Nenhuma conta Microsoft encontrada. Faça login novamente.');
    }

    // A authority vai explícita de propósito: sem ela o MSAL reutiliza a authority da conta
    // em cache — e quem logou antes da correção `/v2.0` carrega conta v1, que emite access
    // token v1 (iss sts.windows.net) para sempre. Com a authority v2 o MSAL renova pela via
    // v2 quando o cache expirar.
    let resultado = await this.instance.acquireTokenSilent({
      scopes: GRAPH_SCOPES,
      account: conta,
      authority: environment.msalAuthority,
    });

    // Cache obsoleto: o silent acima pode devolver o v1 guardado em localStorage. O back-end
    // autentica pelo idToken (v2) e usa o access token só como bearer para o Graph — então um
    // v1 aqui não quebra o login —, mas forçar uma renovação aproxima o navegador do formato
    // v2 e evita carregar o token legado para sempre.
    if (decodificarClaims(resultado.accessToken)['ver'] === '1.0') {
      try {
        resultado = await this.instance.acquireTokenSilent({
          scopes: GRAPH_SCOPES,
          account: conta,
          authority: environment.msalAuthority,
          forceRefresh: true,
        });
      } catch {
        // Renovação falhou (rede, sessão expirada): segue com o token em cache. O back-end
        // decide pelo idToken + Graph, então abortar aqui seria recusar um login válido.
      }
    }

    const resposta = await fetch(`${environment.apiUrl}/auth/login-microsoft`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        accessToken: resultado.accessToken,
        idToken: resultado.idToken,
      }),
    });

    const dados = await resposta.json().catch(() => null);

    if (!resposta.ok) {
      const mensagem = dados?.mensagem ?? dados?.message ?? 'Não foi possível realizar o login via Microsoft.';
      throw new Error(mensagem);
    }

    gravarSessao(dados);

    return dados as { accessToken: string; tokenType: string; expiresInSeconds: number };
  }

  async logout(): Promise<void> {
    this.limparSessaoLocal();

    // `logoutRedirect` também exige a instância inicializada.
    await this.quandoPronto();
    await this.instance.logoutRedirect({ postLogoutRedirectUri: environment.msalRedirectUri });
  }

  isAdmin(): boolean {
    // Passa pela mesma checagem de validade de `obterUsuarioLogado`: um papel guardado no
    // `localStorage` ao lado de um token vencido descreveria uma sessão que a API não reconhece.
    if (!lerTokenValido()) {
      return false;
    }

    const role = lerChave('usuarioRole');
    return role === 'ADMIN';
  }

  /**
   * Descarta a identidade guardada no navegador. Usado quando o login no backend
   * falha, para que uma sessão de uma tentativa anterior não sobreviva e continue
   * aparecendo como se estivesse válida.
   */
  limparSessaoLocal(): void {
    limparSessao();
  }
}

/**
 * Lê o payload de um JWT sem verificar nada.
 *
 * <p>Serve só para o teste de formato abaixo (token v1 em cache pede renovação forçada).
 * Quem valida token é o back-end, com a chave pública do Microsoft.
 */
function decodificarClaims(jwt: string | undefined): Record<string, unknown> {
  if (!jwt) return {};
  try {
    const payload = jwt.split('.')[1];
    if (!payload) return {};
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + c.charCodeAt(0).toString(16).padStart(2, '0'))
        .join('')
    );
    return JSON.parse(json);
  } catch {
    return {};
  }
}
