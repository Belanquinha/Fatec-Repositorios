import { Injectable, Inject } from '@angular/core';
import { MSAL_INSTANCE } from '@azure/msal-angular';
import { AccountInfo, AuthenticationResult, IPublicClientApplication, PopupRequest } from '@azure/msal-browser';
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
    await this.instance.handleRedirectPromise();
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

  loginPopUp(): void {
    const request: PopupRequest = {
      scopes: GRAPH_SCOPES,
      authority: environment.msalAuthority,
      redirectUri: environment.msalRedirectUri,
    };

    this.instance.loginPopup(request).then(() => {
      window.location.reload();
    });
  }

  logout(): void {
    this.instance.logoutPopup();
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
