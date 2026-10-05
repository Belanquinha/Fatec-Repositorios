import { Component, HostListener, ElementRef, ViewChild, OnInit, OnDestroy, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { MicrosoftLoginButton } from '../microsoft-login-button/microsoft-login-button';
import { AuthService } from '../../../core/auth/auth.service';
import { UsuarioLogado } from '../../../core/auth/models/usuario-logado';
import { filter } from 'rxjs/operators';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-header',
  imports: [CommonModule, RouterModule, MicrosoftLoginButton],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header implements OnInit, OnDestroy {
  estadoDoMenuAberto = false;
  isHovered = false;
  logado = signal(false);
  isAdmin = signal(false);
  usuario = signal<UsuarioLogado | null>(null);
  private routerSubscription!: Subscription;

  primeiroNome = computed(() => {
    const nome = this.usuario()?.nome;
    if (!nome) return '';
    const primeiro = nome.split(' ')[0];
    return primeiro.charAt(0).toUpperCase() + primeiro.slice(1).toLowerCase();
  });

  @ViewChild('menuNav') menuNav!: ElementRef<HTMLElement>;

  constructor(private elementRef: ElementRef, private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.iniciarSessao();
    this.routerSubscription = this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      this.fecharMenu();
    });
  }

  ngOnDestroy(): void {
    if (this.routerSubscription) {
      this.routerSubscription.unsubscribe();
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
  }

  loginMicrosoft(): void {
    this.authService.loginMicrosoft().catch((erro) => {
      console.error('Erro ao iniciar o login com a Microsoft: ', erro);
    });
  }

  logout(): void {
    this.authService.logout().catch((erro) => {
      console.error('Erro ao encerrar a sessão: ', erro);
    });
  }

  fecharMenu(): void {
    this.estadoDoMenuAberto = false;
  }

  @HostListener('document:click', ['$event'])
  onClickOutside(event: MouseEvent): void {
    if (!this.estadoDoMenuAberto) return;

    const target = event.target as HTMLElement;
    if (target.closest('.icon-menu')) return;
    if (target.closest('.profile-btn')) return;
    if (this.menuNav?.nativeElement.contains(target)) return;

    this.estadoDoMenuAberto = false;
  }
}
