import { Injectable, Inject, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MSAL_INSTANCE } from '@azure/msal-angular';
import {
  AccountInfo,
  AuthenticationResult,
  IPublicClientApplication,
  RedirectRequest,
} from '@azure/msal-browser';
import { environment } from '../../../environments/environment';
import { UsuarioLogado } from './models/usuario-logado';
import {
  SessaoBackend,
  gravarSessao,
  lerChave,
  lerTokenValido,
  limparSessao,
} from './sessao-navegador';

const GRAPH_SCOPES = ['https://graph.microsoft.com/User.Read'];
const RETURN_URL_STORAGE_KEY = 'fatec_auth_return_url';

export interface CodigoEnviadoResponse {
  mensagem: string;
  email: string;
  expiraEmSegundos: number;
  reenvioEmSegundos: number;
}

export class ErroAutenticacao extends Error {
  constructor(
    message: string,
    public readonly status?: number,
    public readonly retryAfterSeconds?: number,
  ) {
    super(message);
    this.name = 'ErroAutenticacao';
  }
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private pronto?: Promise<void>;
  private inicializadoComSucesso = false;
  private readonly router = inject(Router, { optional: true });

  /**
   * Estado reativo da sessão para que o Header e demais componentes reflitam
   * logins e logouts feitos via SPA imediatamente, sem exigir reload completo.
   */
  readonly usuarioAtual = signal<UsuarioLogado | null>(null);

  constructor(@Inject(MSAL_INSTANCE) private instance: IPublicClientApplication) {}

  async inicializar(): Promise<void> {
    this.pronto ??= this.executarInicializacao();
    const tentativa = this.pronto;

    try {
      await tentativa;
    } catch (erro) {
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
    const resultadoRedirect = await this.inicializado();
    this.inicializadoComSucesso = true;

    if (resultadoRedirect) {
      try {
        await this.trocarTokensMicrosoftNoBackend(
          resultadoRedirect.accessToken,
          resultadoRedirect.idToken,
        );
        const destino = this.consumirReturnUrl();
        if (destino && destino !== '/') {
          await this.router?.navigateByUrl(destino);
        }
      } catch (erro) {
        console.error('Erro ao concluir login Microsoft com o back-end:', erro);
        this.limparSessaoLocal();
      }
    } else {
      this.sincronizarSessao();
    }
  }

  private async inicializado(): Promise<AuthenticationResult | null> {
    try {
      return (await this.instance.handleRedirectPromise()) ?? null;
    } catch (e: any) {
      if (e?.errorCode === 'no_token_request_cache_error') {
        Object.keys(sessionStorage)
          .filter((k) => k.startsWith('msal.'))
          .forEach((k) => sessionStorage.removeItem(k));
        return null;
      }
      throw e;
    }
  }

  sincronizarSessao(): UsuarioLogado | null {
    if (!lerTokenValido()) {
      this.usuarioAtual.set(null);
      return null;
    }

    const nome = lerChave('usuarioNome');
    const email = lerChave('usuarioEmail');
    if (!nome || !email) {
      this.usuarioAtual.set(null);
      return null;
    }

    const foto = lerChave('usuarioFoto');
    const role = lerChave('usuarioRole');
    // O id é opcional de propósito: uma sessão gravada antes desta chave existir
    // (ou um payload que não a trouxe) continua apresentando nome/e-mail. Quem
    // precisa do id trata a ausência — hoje o `podeExcluir()` do /perfil, que
    // simplesmente não mostra a ação em vez de inventar autoria.
    const id = lerChave('usuarioId');

    const usuario: UsuarioLogado = {
      id: id || undefined,
      nome,
      email,
      foto: foto || undefined,
      role: role || undefined,
    };
    this.usuarioAtual.set(usuario);
    return usuario;
  }

  async obterUsuarioLogado(): Promise<UsuarioLogado | null> {
    return this.sincronizarSessao();
  }

  async loginMicrosoft(returnUrl?: string): Promise<void> {
    await this.quandoPronto();

    if (returnUrl && typeof sessionStorage !== 'undefined') {
      sessionStorage.setItem(RETURN_URL_STORAGE_KEY, returnUrl);
    }

    const request: RedirectRequest = {
      scopes: GRAPH_SCOPES,
      authority: environment.msalAuthority,
      redirectUri: environment.msalRedirectUri,
    };

    await this.instance.loginRedirect(request);
  }

  async loginMicrosoftViaApi(): Promise<{
    accessToken: string;
    tokenType: string;
    expiresInSeconds: number;
  }> {
    const conta = this.conta;
    if (!conta) {
      throw new Error('Nenhuma conta Microsoft encontrada. Faça login novamente.');
    }

    let resultado = await this.instance.acquireTokenSilent({
      scopes: GRAPH_SCOPES,
      account: conta,
      authority: environment.msalAuthority,
    });

    if (decodificarClaims(resultado.accessToken)['ver'] === '1.0') {
      try {
        resultado = await this.instance.acquireTokenSilent({
          scopes: GRAPH_SCOPES,
          account: conta,
          authority: environment.msalAuthority,
          forceRefresh: true,
        });
      } catch {
        // Segue com o token em cache caso a renovação falhe.
      }
    }

    return this.trocarTokensMicrosoftNoBackend(resultado.accessToken, resultado.idToken);
  }

  private async trocarTokensMicrosoftNoBackend(
    accessToken: string,
    idToken: string,
  ): Promise<{ accessToken: string; tokenType: string; expiresInSeconds: number }> {
    const dados = await this.postJson<SessaoBackend>(
      '/auth/login-microsoft',
      { accessToken, idToken },
      'Não foi possível realizar o login via Microsoft.',
    );

    gravarSessao(dados);
    this.sincronizarSessao();

    return dados as { accessToken: string; tokenType: string; expiresInSeconds: number };
  }

  async loginComEmailSenha(email: string, senha: string): Promise<SessaoBackend> {
    const dados = await this.postJson<SessaoBackend>(
      '/auth/login',
      { email, senha },
      'E-mail ou senha inválidos.',
    );

    gravarSessao(dados);
    this.sincronizarSessao();
    return dados;
  }

  async iniciarCadastro(
    nome: string,
    email: string,
    senha: string,
  ): Promise<CodigoEnviadoResponse> {
    return this.postJson<CodigoEnviadoResponse>(
      '/auth/cadastro/iniciar',
      { nome, email, senha },
      'Não foi possível iniciar o cadastro.',
    );
  }

  async reenviarCodigoCadastro(email: string): Promise<CodigoEnviadoResponse> {
    return this.postJson<CodigoEnviadoResponse>(
      '/auth/cadastro/reenviar',
      { email },
      'Não foi possível reenviar o código.',
    );
  }

  async confirmarCadastro(email: string, codigo: string): Promise<SessaoBackend> {
    const dados = await this.postJson<SessaoBackend>(
      '/auth/cadastro/confirmar',
      { email, codigo },
      'Código inválido ou expirado.',
    );

    gravarSessao(dados);
    this.sincronizarSessao();
    return dados;
  }

  async solicitarRecuperacaoSenha(email: string): Promise<CodigoEnviadoResponse> {
    return this.postJson<CodigoEnviadoResponse>(
      '/auth/senha/esquecer',
      { email },
      'Não foi possível solicitar a recuperação de senha.',
    );
  }

  async redefinirSenha(
    email: string,
    codigo: string,
    novaSenha: string,
  ): Promise<{ mensagem: string }> {
    return this.postJson<{ mensagem: string }>(
      '/auth/senha/redefinir',
      { email, codigo, novaSenha },
      'Não foi possível redefinir a senha.',
    );
  }

  async logout(): Promise<void> {
    this.limparSessaoLocal();

    await this.quandoPronto();
    if (this.conta) {
      await this.instance.logoutRedirect({ postLogoutRedirectUri: environment.msalRedirectUri });
      return;
    }

    await this.router?.navigateByUrl('/');
  }

  isAdmin(): boolean {
    if (!lerTokenValido()) {
      return false;
    }

    const role = lerChave('usuarioRole');
    return role === 'ADMIN';
  }

  limparSessaoLocal(): void {
    limparSessao();
    this.usuarioAtual.set(null);
  }

  private consumirReturnUrl(): string | null {
    if (typeof sessionStorage === 'undefined') {
      return null;
    }
    const valor = sessionStorage.getItem(RETURN_URL_STORAGE_KEY);
    sessionStorage.removeItem(RETURN_URL_STORAGE_KEY);
    if (!valor || !valor.startsWith('/') || valor.startsWith('//')) {
      return null;
    }
    return valor;
  }

  private async postJson<T>(
    caminho: string,
    corpo: unknown,
    mensagemPadraoErro: string,
  ): Promise<T> {
    let resposta: Response;
    try {
      resposta = await fetch(`${environment.apiUrl}${caminho}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(corpo),
      });
    } catch {
      throw new ErroAutenticacao(
        'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.',
      );
    }

    const dados = await resposta.json().catch(() => null);

    if (!resposta.ok) {
      const retryHeader = resposta.headers?.get?.('Retry-After');
      const retryAfter = retryHeader ? Number.parseInt(retryHeader, 10) : undefined;
      throw new ErroAutenticacao(
        extrairMensagemErro(dados, mensagemPadraoErro),
        resposta.status,
        Number.isFinite(retryAfter) ? retryAfter : undefined,
      );
    }

    return dados as T;
  }
}

function extrairMensagemErro(dados: any, padrao: string): string {
  if (dados?.erros && typeof dados.erros === 'object') {
    const primeiraChave = Object.keys(dados.erros)[0];
    if (primeiraChave && typeof dados.erros[primeiraChave] === 'string') {
      return dados.erros[primeiraChave];
    }
  }
  return dados?.mensagem ?? dados?.message ?? padrao;
}

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
        .join(''),
    );
    return JSON.parse(json);
  } catch {
    return {};
  }
}
