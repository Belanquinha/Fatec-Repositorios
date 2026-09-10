import { Injectable, Inject } from '@angular/core';
import { MSAL_INSTANCE } from '@azure/msal-angular';
import { AccountInfo, AuthenticationResult, IPublicClientApplication, RedirectRequest } from '@azure/msal-browser';
import { environment } from '../../../environments/environment';
import { UsuarioLogado } from './models/usuario-logado';

const GRAPH_SCOPES = ['User.Read', 'openid', 'profile', 'email'];
const GRAPH_PHOTO_URL = 'https://graph.microsoft.com/v1.0/me/photo/$value';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  constructor(@Inject(MSAL_INSTANCE) private instance: IPublicClientApplication) {}

  async inicializar(): Promise<void> {
    await this.instance.initialize();
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

  get conta(): AccountInfo | undefined {
    const contas = this.instance.getAllAccounts();
    return contas.length > 0 ? contas[0] : undefined;
  }

  async obterUsuarioLogado(): Promise<UsuarioLogado | null> {
    const nomeBackend = localStorage.getItem('usuarioNome');
    const emailBackend = localStorage.getItem('usuarioEmail');
    const fotoBackend = localStorage.getItem('usuarioFoto');

    if (nomeBackend && emailBackend) {
      return {
        nome: nomeBackend,
        email: emailBackend,
        foto: fotoBackend || undefined,
      };
    }

    const conta = this.conta;
    if (!conta) {
      return null;
    }

    const usuario: UsuarioLogado = {
      nome: conta.name || conta.username || '',
      email: conta.username || '',
    };

    const foto = await this.buscarFotoPerfil(conta);
    if (foto) {
      usuario.foto = foto;
    }

    return usuario;
  }

  async loginMicrosoft(): Promise<void> {
    const request: RedirectRequest = {
      scopes: GRAPH_SCOPES,
      authority: environment.msalAuthority,
      redirectUri: environment.msalRedirectUri,
    };

    const resultado = await this.instance.loginRedirect(request);
  }

  async loginMicrosoftViaApi(): Promise<{ accessToken: string; tokenType: string; expiresInSeconds: number }> {
    const conta = this.conta;
    if (!conta) {
      throw new Error('Nenhuma conta Microsoft encontrada. Faça login novamente.');
    }

    const resultado = await this.instance.acquireTokenSilent({
      scopes: GRAPH_SCOPES,
      account: conta,
    });

    const resposta = await fetch(`${environment.apiUrl}/auth/login-microsoft`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        accessToken: resultado.accessToken,
      }),
    });

    const dados = await resposta.json().catch(() => null);

    if (!resposta.ok) {
      const mensagem = dados?.mensagem ?? dados?.message ?? 'Não foi possível realizar o login via Microsoft.';
      throw new Error(mensagem);
    }

    if (typeof window !== 'undefined' && dados?.accessToken) {
      window.localStorage.setItem('accessToken', dados.accessToken);
      window.localStorage.setItem('tokenType', dados.tokenType ?? 'Bearer');
      window.localStorage.setItem('expiresInSeconds', String(dados.expiresInSeconds ?? 0));
      if (dados.nome) window.localStorage.setItem('usuarioNome', dados.nome);
      if (dados.email) window.localStorage.setItem('usuarioEmail', dados.email);
      if (dados.fotoUrl) window.localStorage.setItem('usuarioFoto', dados.fotoUrl);
    }

    return dados as { accessToken: string; tokenType: string; expiresInSeconds: number };
  }

  async loginInstituicao(email: string, senha: string): Promise<{ accessToken: string; tokenType: string; expiresInSeconds: number }> {
    const resposta = await fetch(`${environment.apiUrl}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, senha }),
    });

    console.log(resposta)

    const dados = await resposta.json().catch(() => null);

    if (!resposta.ok) {
      const mensagem = dados?.mensagem ?? dados?.message ?? 'Não foi possível realizar o login.';
      throw new Error(mensagem);
    }

    if (typeof window !== 'undefined' && dados?.accessToken) {
      window.localStorage.setItem('accessToken', dados.accessToken);
      window.localStorage.setItem('tokenType', dados.tokenType ?? 'Bearer');
      window.localStorage.setItem('expiresInSeconds', String(dados.expiresInSeconds ?? 0));
    }

    return dados as { accessToken: string; tokenType: string; expiresInSeconds: number };
  }

  logout(): void {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('tokenType');
    localStorage.removeItem('expiresInSeconds');
    localStorage.removeItem('usuarioNome');
    localStorage.removeItem('usuarioEmail');
    localStorage.removeItem('usuarioFoto');
    this.instance.logoutRedirect({ postLogoutRedirectUri: environment.msalRedirectUri });
  }

  private async buscarFotoPerfil(conta: AccountInfo): Promise<string | undefined> {
    try {
      const resultado: AuthenticationResult = await this.instance.acquireTokenSilent({
        scopes: ['User.Read'],
        account: conta,
      });

      const resposta = await fetch(GRAPH_PHOTO_URL, {
        headers: { Authorization: `Bearer ${resultado.accessToken}` },
      });

      if (!resposta.ok) {
        return undefined;
      }

      const blob = await resposta.blob();
      return await this.blobParaDataUrl(blob);
    } catch (erro) {
      console.error('Erro ao buscar a foto de perfil: ', erro);
      return undefined;
    }
  }

  private blobParaDataUrl(blob: Blob): Promise<string> {
    return new Promise((resolve, reject) => {
      const leitor = new FileReader();
      leitor.onload = () => resolve(leitor.result as string);
      leitor.onerror = () => reject(leitor.error);
      leitor.readAsDataURL(blob);
    });
  }
}
