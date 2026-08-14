import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../core/auth/auth.service';
import { UsuarioLogado } from '../../../core/auth/models/usuario-logado';

@Component({
  selector: 'app-microsoft-login-button',
  imports: [],
  templateUrl: './microsoft-login-button.html',
  styleUrl: './microsoft-login-button.css',
})
export class MicrosoftLoginButton implements OnInit {
  usuarioLogado = false;
  usuario: UsuarioLogado | null = null;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.authService
      .inicializar()
      .then(() => this.carregarUsuario())
      .catch((error) => {
        console.error('Erro ao inicializar o login da Microsoft: ', error);
      });
  }

  private async carregarUsuario(): Promise<void> {
    this.usuario = await this.authService.obterUsuarioLogado();
    this.usuarioLogado = this.usuario !== null;
  }

  login(): void {
    this.authService.loginPopUp();
  }

  logout(): void {
    this.authService.logout();
    this.usuarioLogado = false;
    this.usuario = null;
  }
}
