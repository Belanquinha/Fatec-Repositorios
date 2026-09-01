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

  loginMicrosoft(): void {
    const request: RedirectRequest = {
      scopes: GRAPH_SCOPES,
      authority: environment.msalAuthority,
      redirectUri: environment.msalRedirectUri,
    };

    this.instance.loginRedirect(request);
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
