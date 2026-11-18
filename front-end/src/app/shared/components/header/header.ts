import {
  Component,
  HostListener,
  ElementRef,
  ViewChild,
  OnInit,
  OnDestroy,
  computed,
  effect,
  signal,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';
import { ThemeService } from '../../../core/theme/theme.service';
import { UsuarioLogado } from '../../../core/auth/models/usuario-logado';
import { filter } from 'rxjs/operators';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-header',
  imports: [CommonModule, RouterModule],
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
  private sessaoInicializada = false;
  private readonly themeService = inject(ThemeService);
  modoEscuro = this.themeService.modoEscuro;
  private routerSubscription!: Subscription;

  primeiroNome = computed(() => {
    const nome = this.usuario()?.nome;
    if (!nome) return '';
    const primeiro = nome.split(' ')[0];
    return primeiro.charAt(0).toUpperCase() + primeiro.slice(1).toLowerCase();
  });

  podeSubmeter = computed(
    () => this.usuario()?.role === 'ALUNO' || this.usuario()?.role === 'PROFESSOR',
  );

  @ViewChild('menuNav') menuNav!: ElementRef<HTMLElement>;
  private ultimoFocoAntesDoMenu: HTMLElement | null = null;
  private overflowAnteriorDoBody: string | null = null;

  constructor(
    private elementRef: ElementRef,
    private authService: AuthService,
    private router: Router,
  ) {
    if (this.authService.usuarioAtual) {
      effect(() => {
        const atual = this.authService.usuarioAtual();
        if (!this.sessaoInicializada && atual === null) {
          return;
        }
        this.usuario.set(atual);
        this.logado.set(atual !== null);
        this.isAdmin.set(this.authService.isAdmin());
      });
    }
  }

  ngOnInit(): void {
    this.iniciarSessao();
    this.routerSubscription = this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => {
        this.fecharMenu();
        if (this.sessaoInicializada) {
          void this.carregarUsuario();
        }
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

  private async iniciarSessao(): Promise<void> {
    try {
      await this.authService.quandoPronto();
    } catch (erro) {
      console.error('Erro ao inicializar o login da Microsoft: ', erro);
      return;
    }
    this.sessaoInicializada = true;
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
      queueMicrotask(() => this.menuNav?.nativeElement.focus());
    } else {
      this.aplicarTravaDeRolagem();
      this.restaurarFoco();
    }
  }

  alternarTema(): void {
    this.themeService.alternar();
  }

  logout(): void {
    this.fecharMenu();
    Promise.resolve(this.authService.logout()).catch((erro) => {
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
