import { ComponentFixture, TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';

import {
  CHAVE_CONSENTIMENTO_COOKIES,
  CookieConsentService,
} from '../../../core/cookies/cookie-consent.service';
import { CookieBanner } from './cookie-banner';

describe('CookieBanner', () => {
  let fixture: ComponentFixture<CookieBanner>;
  let service: CookieConsentService;

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      imports: [CookieBanner],
    }).compileComponents();

    service = TestBed.inject(CookieConsentService);
    fixture = TestBed.createComponent(CookieBanner);
    fixture.detectChanges();
  });

  it('renderiza o aviso LGPD na primeira visita com botões de aceitar, recusar e personalizar', () => {
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('[data-cookie-banner]')).not.toBeNull();
    expect(el.querySelector('[data-cookie-accept]')).not.toBeNull();
    expect(el.querySelector('[data-cookie-reject]')).not.toBeNull();
    expect(el.querySelector('[data-cookie-customize]')).not.toBeNull();
  });

  it('oculta o banner e persiste consentimento completo ao clicar em "Aceitar todos"', () => {
    const btnAceitar = fixture.nativeElement.querySelector(
      '[data-cookie-accept]',
    ) as HTMLButtonElement;
    btnAceitar.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-cookie-banner]')).toBeNull();
    expect(service.consentimento()).toMatchObject({
      essenciais: true,
      preferencias: true,
      analiticos: true,
    });
  });

  it('grava apenas essenciais ao clicar em "Apenas essenciais"', () => {
    const btnRejeitar = fixture.nativeElement.querySelector(
      '[data-cookie-reject]',
    ) as HTMLButtonElement;
    btnRejeitar.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-cookie-banner]')).toBeNull();
    expect(service.consentimento()).toMatchObject({
      essenciais: true,
      preferencias: false,
      analiticos: false,
    });
  });

  it('abre o modal com categorias opcionais desmarcadas por padrão (opt-in LGPD) e salva seleção', () => {
    const btnPersonalizar = fixture.nativeElement.querySelector(
      '[data-cookie-customize]',
    ) as HTMLButtonElement;
    btnPersonalizar.click();
    fixture.detectChanges();

    const modal = fixture.nativeElement.querySelector('[data-cookie-modal]') as HTMLElement;
    expect(modal).not.toBeNull();

    const switchPref = modal.querySelector('[data-switch-preferencias]') as HTMLButtonElement;
    const switchAnaliticos = modal.querySelector('[data-switch-analiticos]') as HTMLButtonElement;
    expect(switchPref.getAttribute('aria-checked')).toBe('false');
    expect(switchAnaliticos.getAttribute('aria-checked')).toBe('false');

    switchPref.click();
    fixture.detectChanges();
    expect(switchPref.getAttribute('aria-checked')).toBe('true');

    const btnSalvar = modal.querySelector('[data-modal-save]') as HTMLButtonElement;
    btnSalvar.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-cookie-modal]')).toBeNull();
    const salvo = JSON.parse(localStorage.getItem(CHAVE_CONSENTIMENTO_COOKIES)!);
    expect(salvo.preferencias).toBe(true);
    expect(salvo.analiticos).toBe(false);
  });

  it('fecha o modal ao pressionar Escape', () => {
    service.abrirPreferencias();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[data-cookie-modal]')).not.toBeNull();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[data-cookie-modal]')).toBeNull();
  });
});
