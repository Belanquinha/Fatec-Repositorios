import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ThemeService } from '../../core/theme/theme.service';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-configuracoes',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './configuracoes.html',
  styleUrl: './configuracoes.css'
})
export class Configuracoes implements OnInit {
  private router = inject(Router);
  private themeService = inject(ThemeService);
  private authService = inject(AuthService);

  modoEscuro = this.themeService.modoEscuro;
  fonteTamanho = signal('medium');
  usuario = signal<{ nome: string; email: string; role?: string } | null>(null);
  carregando = signal(true);

  ngOnInit(): void {
    this.authService.obterUsuarioLogado().then((u) => {
      this.usuario.set(u);
      this.carregando.set(false);
    }).catch(() => this.carregando.set(false));
  }

  alternarTema(): void {
    this.themeService.alternar();
  }

  alterarFonte(event: Event): void {
    const valor = (event.target as HTMLSelectElement).value;
    this.fonteTamanho.set(valor);
    this.aplicarFonte(valor);
  }

  private aplicarFonte(tamanho: string): void {
    const mapa = { small: '12px', medium: '14px', large: '16px' };
    const novaTamanho = mapa[tamanho] || '14px';
    document.documentElement.style.setProperty('--fx-fonte', novaTamanho);
  }

  sairConta(): void {
    this.authService.logout().catch((erro) => {
      console.error('Erro ao encerrar a sessão: ', erro);
    });
    this.router.navigate(['/']);
  }
}