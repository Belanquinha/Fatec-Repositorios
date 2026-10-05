import { Component, OnInit, signal } from '@angular/core';
import { AuthService } from '../../../core/auth/auth.service';
import { UsuarioLogado } from '../../../core/auth/models/usuario-logado';

@Component({
  selector: 'app-microsoft-login-button',
  imports: [],
  templateUrl: './microsoft-login-button.html',
  styleUrl: './microsoft-login-button.css',
})
export class MicrosoftLoginButton implements OnInit {
  usuarioLogado = signal(false);
  usuario = signal<UsuarioLogado | null>(null);
  inicializando = signal(true);

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.authService
      .inicializar()
      .then(async () => {
        const conta = this.authService.conta;
        if (conta) {
          try {
            await this.authService.loginMicrosoftViaApi();
          } catch (e) {
            console.error('Erro ao autenticar com o back-end:', e);
            // Sem sessão no backend não há perfil a exibir: descarta o que sobrou
            // de tentativas anteriores para a UI não mostrar um usuário "logado"
            // que o backend nunca reconheceu.
            this.authService.limparSessaoLocal();
          }
        }
        await this.carregarUsuario();
      })
      .catch((error) => {
        console.error('Erro ao inicializar o login da Microsoft: ', error);
      })
      .finally(() => {
        this.inicializando.set(false);
      });
  }

  private async carregarUsuario(): Promise<void> {
    this.usuario.set(await this.authService.obterUsuarioLogado());
    this.usuarioLogado.set(this.usuario() !== null);
  }

  login(): void {
    this.authService.loginMicrosoft();
  }

  logout(): void {
    this.authService.logout().catch((erro) => {
      console.error('Erro ao encerrar a sessão: ', erro);
    });
    this.usuarioLogado.set(false);
    this.usuario.set(null);
  }
}