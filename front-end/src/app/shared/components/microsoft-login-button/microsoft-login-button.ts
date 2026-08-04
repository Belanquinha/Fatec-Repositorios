import { Component , OnInit} from '@angular/core';
import { MsalService } from '@azure/msal-angular';
import { AuthenticationResult } from '@azure/msal-browser';

@Component({
  selector: 'app-microsoft-login-button',
  imports: [],
  templateUrl: './microsoft-login-button.html',
  styleUrl: './microsoft-login-button.css',
})
export class MicrosoftLoginButton implements OnInit {
  usuarioLogado = false;
  nomeUsuario = '';

  // Injeta o serviço da Microsoft no construtor
  constructor(private authService: MsalService) {}


  // Verifica se o usuário já tem uma sessão ativa ao carregar a página
  ngOnInit(): void {
    this.authService.instance.initialize().then(() => {

      this.authService.instance.handleRedirectPromise().then(() => {
        const contas = this.authService.instance.getAllAccounts();
        if (contas.length > 0) {
          this.usuarioLogado = true;
          this.nomeUsuario = contas[0].name || '';
        }

     }).catch(error => {
      console.error('Erro ao lidar com a promessa de redirecionamento: ', error);
     })
    })
  }

  
  // redireciona para a pagina Microsoft para o usuário logar
  login() {
    this.authService.loginRedirect()
  }

  // limpa as variaveis locais e manda o popup de logout da Microsoft
  logout() {
    this.authService.logoutPopup().subscribe(() => {
      this.usuarioLogado = false;
      this.nomeUsuario = '';
    });
  }
}