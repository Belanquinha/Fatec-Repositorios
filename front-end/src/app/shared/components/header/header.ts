import {
  Component,
  HostListener,
  ElementRef,
  ViewChild,
  OnInit,
  OnDestroy,
  computed,
  signal,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { MicrosoftLoginButton } from '../microsoft-login-button/microsoft-login-button';
import { AuthService } from '../../../core/auth/auth.service';
import { ThemeService } from '../../../core/theme/theme.service';
import { UsuarioLogado } from '../../../core/auth/models/usuario-logado';
import { filter } from 'rxjs/operators';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-header',
  imports: [CommonModule, RouterModule, MicrosoftLoginButton],
  templateUrl: './header.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './header.css',
})
export class Header implements OnInit, OnDestroy {
  estadoDoMenuAberto = false;
  isHovered = false;
  logado = signal(false);
  isAdmin = signal(false);
  usuario = signal<UsuarioLogado | null>(null);
  private readonly themeService = inject(ThemeService);
  /** Espelha o modo escuro no template (ícone sol/lua + estado do switch). */
  modoEscuro = this.themeService.modoEscuro;
  private routerSubscription!: Subscription;

  primeiroNome = computed(() => {
    const nome = this.usuario()?.nome;
    if (!nome) return '';
    const primeiro = nome.split(' ')[0];
    return primeiro.charAt(0).toUpperCase() + primeiro.slice(1).toLowerCase();
  });

  @ViewChild('menuNav') menuNav!: ElementRef<HTMLElement>;
  /** Foco a restaurar ao fechar o drawer (a11y: o Tab volta para o toggle). */
  private ultimoFocoAntesDoMenu: HTMLElement | null = null;
  /** `overflow` do body antes da trava — restaurado ao fechar (não zerado). */
  private overflowAnteriorDoBody: string | null = null;

  constructor(
    private elementRef: ElementRef,
    private authService: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.iniciarSessao();
    this.routerSubscription = this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => {
        this.fecharMenu();
      });
  }

  ngOnDestroy(): void {
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
    }
    if (
      typeof document !== 'undefined' &&
      this.overflowAnteriorDoBody !== null
    ) {
      document.body.style.overflow = this.overflowAnteriorDoBody;
      this.overflowAnteriorDoBody = null;
    }
  }

  /**
   * O header é montado antes do `MicrosoftLoginButton` disparar `ngOnInit`, então consultar a
   * conta aqui exigiria acesso ao cache do MSAL antes de `initialize()`. Aguardar `quandoPronto()`
   * garante que a UI reflita o estado real da sessão.
   */
  private async iniciarSessao(): Promise<void> {
    try {
      await this.authService.quandoPronto();
    } catch (erro) {
      console.error('Erro ao inicializar o login da Microsoft: ', erro);
      return;
    }
    await this.carregarUsuario();
  }

  private async carregarUsuario(): Promise<void> {
    this.usuario.set(await this.authService.obterUsuarioLogado());
    this.logado.set(this.usuario() !== null);
    this.isAdmin.set(this.authService.isAdmin());
  }

  mudarMenu(): void {
    this.estadoDoMenuAberto = !this.estadoDoMenuAberto;
    if (this.estadoDoMenuAberto) {
      if (
        typeof document !== 'undefined' &&
        document.activeElement instanceof HTMLElement
      ) {
        this.ultimoFocoAntesDoMenu = document.activeElement;
      }
      this.aplicarTravaDeRolagem();
      // Leva o foco para dentro do drawer (sem mudar o visual).
      queueMicrotask(() => this.menuNav?.nativeElement.focus());
    } else {
      this.aplicarTravaDeRolagem();
      this.restaurarFoco();
    }
  }

  loginMicrosoft(): void {
    this.authService.loginMicrosoft().catch((erro) => {
      console.error('Erro ao iniciar o login com a Microsoft: ', erro);
    });
  }

  /** Alterna o modo escuro sem fechar o drawer (o usuário vê o efeito na hora). */
  alternarTema(): void {
    this.themeService.alternar();
  }

  logout(): void {
    this.authService.logout().catch((erro) => {
      console.error('Erro ao encerrar a sessão: ', erro);
    });
  }

  fecharMenu(): void {
    if (!this.estadoDoMenuAberto) return;
    this.estadoDoMenuAberto = false;
    this.aplicarTravaDeRolagem();
    this.restaurarFoco();
  }

  private restaurarFoco(): void {
    const alvo = this.ultimoFocoAntesDoMenu;
    this.ultimoFocoAntesDoMenu = null;
    if (alvo && typeof document !== 'undefined' && document.contains(alvo)) {
      alvo.focus();
    }
  }

  private aplicarTravaDeRolagem(): void {
    if (typeof document === 'undefined') return;
    if (this.estadoDoMenuAberto) {
      if (this.overflowAnteriorDoBody === null) {
        this.overflowAnteriorDoBody = document.body.style.overflow;
      }
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = this.overflowAnteriorDoBody ?? '';
      this.overflowAnteriorDoBody = null;
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.fecharMenu();
  }

  @HostListener('document:click', ['$event'])
  onClickOutside(event: MouseEvent): void {
    if (!this.estadoDoMenuAberto) return;

    const target = event.target as HTMLElement;
    if (target.closest('[data-menu-toggle]')) return;
    if (this.menuNav?.nativeElement.contains(target)) return;

    this.fecharMenu();
  }
}
