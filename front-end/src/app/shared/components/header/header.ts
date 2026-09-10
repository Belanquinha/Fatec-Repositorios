import { Component, HostListener, ElementRef, ViewChild, OnInit, OnDestroy } from '@angular/core';
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
  logado = false;
  usuario: UsuarioLogado | null = null;
  private routerSubscription!: Subscription;

  @ViewChild('menuNav') menuNav!: ElementRef<HTMLElement>;

  constructor(private elementRef: ElementRef, private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    this.carregarUsuario();
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

  private async carregarUsuario(): Promise<void> {
    this.usuario = await this.authService.obterUsuarioLogado();
    this.logado = this.usuario !== null;
  }

  mudarMenu(): void {
    this.estadoDoMenuAberto = !this.estadoDoMenuAberto;
  }

  loginMicrosoft(): void {
    this.authService.loginMicrosoft();
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
