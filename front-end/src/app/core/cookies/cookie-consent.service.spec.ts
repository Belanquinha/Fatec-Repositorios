import { beforeEach, describe, expect, it } from 'vitest';

import { CHAVE_TEMA_ARMAZENADO } from '../theme/theme.service';
import {
  CHAVE_CONSENTIMENTO_COOKIES,
  CookieConsentService,
} from './cookie-consent.service';

describe('CookieConsentService', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('exibe o banner inicial quando ainda não há consentimento salvo', () => {
    const service = new CookieConsentService();

    expect(service.consentimento()).toBeNull();
    expect(service.bannerVisivel()).toBe(true);
    expect(service.preferenciasAbertas()).toBe(false);
  });

  it('aceitarTodos() salva todas as categorias ativas e oculta o banner', () => {
    const service = new CookieConsentService();

    service.aceitarTodos();

    expect(service.bannerVisivel()).toBe(false);
    expect(service.consentimento()).toMatchObject({
      versao: 1,
      essenciais: true,
      preferencias: true,
      analiticos: true,
    });

    const salvo = JSON.parse(localStorage.getItem(CHAVE_CONSENTIMENTO_COOKIES)!);
    expect(salvo.preferencias).toBe(true);
    expect(salvo.analiticos).toBe(true);
  });

  it('rejeitarOpcionais() grava apenas essenciais e limpa preferência de tema persistida', () => {
    localStorage.setItem(CHAVE_TEMA_ARMAZENADO, 'escuro');
    const service = new CookieConsentService();

    service.rejeitarOpcionais();

    expect(service.bannerVisivel()).toBe(false);
    expect(service.consentimento()).toMatchObject({
      versao: 1,
      essenciais: true,
      preferencias: false,
      analiticos: false,
    });
    expect(localStorage.getItem(CHAVE_TEMA_ARMAZENADO)).toBeNull();
  });

  it('salvarPreferencias() respeita escolhas individuais do titular', () => {
    const service = new CookieConsentService();

    service.salvarPreferencias({ preferencias: true, analiticos: false });

    expect(service.consentimento()).toMatchObject({
      essenciais: true,
      preferencias: true,
      analiticos: false,
    });
  });

  it('restaura consentimento válido salvo anteriormente no localStorage', () => {
    localStorage.setItem(
      CHAVE_CONSENTIMENTO_COOKIES,
      JSON.stringify({
        versao: 1,
        essenciais: true,
        preferencias: false,
        analiticos: true,
        atualizadoEm: '2026-11-17T00:00:00.000Z',
      }),
    );

    const service = new CookieConsentService();

    expect(service.bannerVisivel()).toBe(false);
    expect(service.consentimento()?.analiticos).toBe(true);
    expect(service.consentimento()?.preferencias).toBe(false);
  });

  it('ignora JSON corrompido ou inválido e volta a exibir o banner', () => {
    localStorage.setItem(CHAVE_CONSENTIMENTO_COOKIES, '{invalido');

    const service = new CookieConsentService();

    expect(service.consentimento()).toBeNull();
    expect(service.bannerVisivel()).toBe(true);
  });
});
