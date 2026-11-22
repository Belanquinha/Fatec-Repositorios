import {
  Component,
  OnInit,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule, DOCUMENT } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { ThemeService, ModoTema } from '../../core/theme/theme.service';
import { AuthService } from '../../core/auth/auth.service';
import { environment } from '../../../environments/environment';

export type TamanhoFonte = 'sm' | 'md' | 'lg' | 'xl';

export const CHAVE_FONTE_ARMAZENADA = 'fatec-tamanho-fonte';
export const CHAVE_EMAILS_ATIVIDADE = 'fatec-emails-atividade';

const MAPA_TAMANHO_FONTE: Record<TamanhoFonte, string> = {
  sm: '14px',
  md: '16px',
  lg: '18px',
  xl: '20px',
};

const MAPA_PX_PARA_TAMANHO: Record<string, TamanhoFonte> = {
  '14px': 'sm',
  '16px': 'md',
  '18px': 'lg',
  '20px': 'xl',
};

@Component({
  selector: 'app-configuracoes',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './configuracoes.html',
  styleUrl: './configuracoes.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Configuracoes implements OnInit {
  readonly themeService = inject(ThemeService);
  readonly authService = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly document = inject(DOCUMENT);

  /** Usuário conectado no momento. */
  readonly usuario = this.authService.usuarioAtual;
  readonly logado = computed(() => !!this.usuario());

  /** 1. Tema atual: 'claro' | 'escuro' | 'sistema'. */
  readonly modoTema = this.themeService.modo;

  /** 2. Tamanho de fonte do site. */
  readonly tamanhoFonte = signal<TamanhoFonte>('md');

  /** 3. Notificações por e-mail de atividades na conta. */
  readonly notificacoesEmail = signal<boolean>(true);

  /** Feedback temporário de ação na tela. */
  readonly sucessoMensagem = signal<string | null>(null);

  /** 4 & 5. Modal e exclusão de conta. */
  readonly modalExclusaoAberto = signal<boolean>(false);
  readonly textoConfirmacao = signal<string>('');
  readonly excluindoConta = signal<boolean>(false);
  readonly erroExclusao = signal<string | null>(null);

  /** Apenas libera exclusão se o usuário digitar exatamente a frase. */
  readonly podeConfirmarExclusao = computed(() => {
    return this.textoConfirmacao().trim().toLowerCase() === 'excluir minha conta';
  });

  ngOnInit(): void {
    this.carregarPreferenciasSalvas();
  }

  /* --------------------------------------------------------------------------
     1. Tema
     -------------------------------------------------------------------------- */
  definirTema(modo: ModoTema): void {
    this.themeService.definirModo(modo);
    this.exibirSucessoTemporario(`Tema atualizado para ${modo}.`);
  }

  /* --------------------------------------------------------------------------
     2. Tamanho da Fonte
     -------------------------------------------------------------------------- */
  definirTamanhoFonte(tamanho: TamanhoFonte): void {
    this.tamanhoFonte.set(tamanho);
    const px = MAPA_TAMANHO_FONTE[tamanho];

    if (this.document?.documentElement) {
      if (tamanho === 'md') {
        this.document.documentElement.style.fontSize = '';
      } else {
        this.document.documentElement.style.fontSize = px;
      }
    }

    try {
      if (typeof localStorage !== 'undefined') {
        if (tamanho === 'md') {
          localStorage.removeItem(CHAVE_FONTE_ARMAZENADA);
        } else {
          localStorage.setItem(CHAVE_FONTE_ARMAZENADA, px);
        }
      }
    } catch {
      // Ignora erro de storage privado
    }

    this.exibirSucessoTemporario('Tamanho do texto ajustado.');
  }

  obterEstiloAmostraFonte(): string {
    return MAPA_TAMANHO_FONTE[this.tamanhoFonte()];
  }

  /* --------------------------------------------------------------------------
     3. Notificações de E-mail
     -------------------------------------------------------------------------- */
  alternarNotificacoesEmail(): void {
    const novoValor = !this.notificacoesEmail();
    this.notificacoesEmail.set(novoValor);

    try {
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem(CHAVE_EMAILS_ATIVIDADE, String(novoValor));
      }
    } catch {
      // Ignora erro de storage privado
    }

    const texto = novoValor
      ? 'E-mails de atividade da conta ativados.'
      : 'E-mails de atividade da conta desativados.';
    this.exibirSucessoTemporario(texto);
  }

  /* --------------------------------------------------------------------------
     4. Sair da Conta
     -------------------------------------------------------------------------- */
  async sair(): Promise<void> {
    await this.authService.logout();
  }

  /* --------------------------------------------------------------------------
     5. Exclusão de Conta
     -------------------------------------------------------------------------- */
  abrirModalExclusao(): void {
    this.textoConfirmacao.set('');
    this.erroExclusao.set(null);
    this.modalExclusaoAberto.set(true);
  }

  fecharModalExclusao(): void {
    if (this.excluindoConta()) return;
    this.modalExclusaoAberto.set(false);
    this.textoConfirmacao.set('');
    this.erroExclusao.set(null);
  }

  confirmarExclusao(): void {
    if (!this.podeConfirmarExclusao() || this.excluindoConta()) {
      return;
    }

    if (!this.logado()) {
      this.erroExclusao.set('Nenhuma conta conectada para ser excluída.');
      return;
    }

    this.excluindoConta.set(true);
    this.erroExclusao.set(null);

    this.http.delete(`${environment.apiUrl}/usuarios/me`).subscribe({
      next: () => {
        this.excluindoConta.set(false);
        this.modalExclusaoAberto.set(false);
        this.authService.limparSessaoLocal();
        this.router.navigate(['/']);
      },
      error: (erro: any) => {
        this.excluindoConta.set(false);
        const mensagem =
          erro?.error?.mensagem ||
          erro?.error?.message ||
          'Não foi possível concluir a exclusão da conta. Tente novamente mais tarde.';
        this.erroExclusao.set(mensagem);
      },
    });
  }

  /* --------------------------------------------------------------------------
     Helpers
     -------------------------------------------------------------------------- */
  private carregarPreferenciasSalvas(): void {
    try {
      if (typeof localStorage !== 'undefined') {
        // Carrega tamanho de fonte
        const fonteSalva = localStorage.getItem(CHAVE_FONTE_ARMAZENADA);
        if (fonteSalva && MAPA_PX_PARA_TAMANHO[fonteSalva]) {
          this.tamanhoFonte.set(MAPA_PX_PARA_TAMANHO[fonteSalva]);
        }

        // Carrega preferências de email
        const emailSalvo = localStorage.getItem(CHAVE_EMAILS_ATIVIDADE);
        if (emailSalvo !== null) {
          this.notificacoesEmail.set(emailSalvo === 'true');
        }
      }
    } catch {
      // Ignora erro de storage privado
    }
  }

  private temporizadorSucesso: ReturnType<typeof setTimeout> | null = null;
  private exibirSucessoTemporario(msg: string): void {
    this.sucessoMensagem.set(msg);
    if (this.temporizadorSucesso) {
      clearTimeout(this.temporizadorSucesso);
    }
    this.temporizadorSucesso = setTimeout(() => {
      this.sucessoMensagem.set(null);
      this.temporizadorSucesso = null;
    }, 4000);
  }
}
