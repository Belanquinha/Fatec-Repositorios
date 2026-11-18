import { Component, ChangeDetectionStrategy, inject } from '@angular/core';

import { CookieConsentService } from '../../../core/cookies/cookie-consent.service';

@Component({
  selector: 'app-footer',
  imports: [],
  templateUrl: './footer.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './footer.css',
})
export class Footer {
  private readonly cookieConsentService = inject(CookieConsentService);

  abrirPreferenciasCookies(): void {
    this.cookieConsentService.abrirPreferencias();
  }
}
