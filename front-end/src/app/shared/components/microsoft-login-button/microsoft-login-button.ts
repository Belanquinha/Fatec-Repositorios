import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { AuthService } from '../../../core/auth/auth.service';
import { UsuarioLogado } from '../../../core/auth/models/usuario-logado';
import { ContaDev } from '../../../core/auth/models/conta-dev';

@Component({
  selector: 'app-microsoft-login-button',
  imports: [],
  templateUrl: './microsoft-login-button.html',
  styleUrl: './microsoft-login-button.css',
})
export class MicrosoftLoginButton implements OnInit {
  private readonly cdr = inject(ChangeDetectorRef);

  usuarioLogado = false;
  usuario: UsuarioLogado | null = null;
  inicializando = true;

  /**
   * Atalhos de login por papel. Vazio quando o recurso está desligado — e é assim que deve ficar em
   * produção: lá o botão do Microsoft é o único caminho de login.
   */
  contasDev: ContaDev[] = [];

  /** Um atalho em andamento: desabilita os demais para não disparar logins em paralelo. */
  entrandoComoDev = false;

  erroDev: string | null = null;

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
        this.inicializando = false;
        // A aplicação é zoneless: sem isto o botão ficaria permanentemente desabilitado,
        // já que `inicializando` só muda depois de um await.
        this.cdr.markForCheck();
      });

    // Fora da cadeia do MSAL de propósito: uma falha de rede aqui não pode impedir o botão do
    // Microsoft de funcionar. `contasDev()` já devolve lista vazia nesses casos.
    this.carregarContasDev();
  }

  private async carregarContasDev(): Promise<void> {
    this.contasDev = await this.authService.contasDev();
    if (this.contasDev.length > 0) {
      this.cdr.markForCheck();
    }
  }

  private async carregarUsuario(): Promise<void> {
    this.usuario = await this.authService.obterUsuarioLogado();
    this.usuarioLogado = this.usuario !== null;
  }

  login(): void {
    this.authService.loginMicrosoft();
  }

  /**
   * Entra como o papel escolhido, sem passar pela Microsoft, e recarrega a página.
   *
   * O recarregamento é necessário, e não cosmético: o header e os guardas de rota leem a sessão uma
   * única vez, na inicialização. Entrar como outro papel sem recarregar deixaria a tela mostrando as
   * permissões da conta anterior — o oposto do que o atalho promete.
   */
  async entrarComo(conta: ContaDev): Promise<void> {
    this.entrandoComoDev = true;
    this.erroDev = null;

    try {
      await this.authService.loginDev(conta.email);
      this.recarregar();
    } catch (erro) {
      console.error('Erro no login de desenvolvimento:', erro);
      this.erroDev = erro instanceof Error ? erro.message : 'Não foi possível entrar.';
      this.entrandoComoDev = false;
      this.cdr.markForCheck();
    }
  }

  private recarregar(): void {
    window.location.reload();
  }

  logout(): void {
    this.authService.logout().catch((erro) => {
      console.error('Erro ao encerrar a sessão: ', erro);
    });
    this.usuarioLogado = false;
    this.usuario = null;
  }
}