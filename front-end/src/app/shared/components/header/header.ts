import { Component, HostListener, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MicrosoftLoginButton } from '../microsoft-login-button/microsoft-login-button';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-header',
  imports: [CommonModule, RouterModule, MicrosoftLoginButton],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header {
  estadoDoMenuAberto = false;
  isHovered = false;

  @ViewChild('menuNav') menuNav!: ElementRef<HTMLElement>;

  constructor(private elementRef: ElementRef, private authService: AuthService) {}

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
    if (this.menuNav?.nativeElement.contains(target)) return;

    this.estadoDoMenuAberto = false;
  }
}
