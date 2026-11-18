import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  effect,
  inject,
  signal,
} from '@angular/core';

import { CookieConsentService } from '../../../core/cookies/cookie-consent.service';

@Component({
  selector: 'app-cookie-banner',
  imports: [],
  templateUrl: './cookie-banner.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './cookie-banner.css',
})
export class CookieBanner {
  private readonly cookieService = inject(CookieConsentService);

  readonly bannerVisivel = this.cookieService.bannerVisivel;
  readonly preferenciasAbertas = this.cookieService.preferenciasAbertas;
  readonly consentimento = this.cookieService.consentimento;

  readonly preferenciasMarcado = signal(false);
  readonly analiticosMarcado = signal(false);

  constructor() {
    effect(() => {
      if (this.preferenciasAbertas()) {
        const atual = this.consentimento();
        // Privacidade por padrão (LGPD/ANPD): sem escolha prévia, opcionais iniciam desmarcados.
        this.preferenciasMarcado.set(atual ? atual.preferencias : false);
        this.analiticosMarcado.set(atual ? atual.analiticos : false);
      }
    });
  }

  aceitarTodos(): void {
    this.cookieService.aceitarTodos();
  }

  rejeitarOpcionais(): void {
    this.cookieService.rejeitarOpcionais();
  }

  abrirPreferencias(): void {
    this.cookieService.abrirPreferencias();
  }

  fecharPreferencias(): void {
    this.cookieService.fecharPreferencias();
  }

  alternarPreferencias(): void {
    this.preferenciasMarcado.update((v) => !v);
  }

  alternarAnaliticos(): void {
    this.analiticosMarcado.update((v) => !v);
  }

  salvarEscolhas(): void {
    this.cookieService.salvarPreferencias({
      preferencias: this.preferenciasMarcado(),
      analiticos: this.analiticosMarcado(),
    });
  }

  @HostListener('document:keydown.escape')
  aoPressionarEscape(): void {
    if (this.preferenciasAbertas()) {
      this.fecharPreferencias();
    }
  }
}
