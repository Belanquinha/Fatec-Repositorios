import { ComponentFixture, TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';

import { CookieConsentService } from '../../../core/cookies/cookie-consent.service';
import { Footer } from './footer';

describe('Footer', () => {
  let component: Footer;
  let fixture: ComponentFixture<Footer>;
  let cookieConsentService: CookieConsentService;

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [Footer],
    }).compileComponents();

    cookieConsentService = TestBed.inject(CookieConsentService);
    fixture = TestBed.createComponent(Footer);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('abre o modal de preferências de cookies (LGPD) ao clicar em "Gerenciar Cookies"', () => {
    const btn = fixture.nativeElement.querySelector(
      '[data-manage-cookies]',
    ) as HTMLButtonElement;
    expect(btn).not.toBeNull();
    expect(cookieConsentService.preferenciasAbertas()).toBe(false);

    btn.click();

    expect(cookieConsentService.preferenciasAbertas()).toBe(true);
  });
});
