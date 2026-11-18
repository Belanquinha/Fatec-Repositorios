import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CHAVE_TEMA_ARMAZENADO, ThemeService } from './theme.service';

function simularSistema(escuro: boolean): void {
  vi.stubGlobal('matchMedia', vi.fn().mockReturnValue({
    matches: escuro,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  }));
}

describe('ThemeService', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.unstubAllGlobals();
    document.documentElement.classList.remove('dark');
  });

  it('começa claro quando não há escolha salva nem preferência do sistema', () => {
    simularSistema(false);
    const service = new ThemeService();

    expect(service.tema()).toBe('claro');
    expect(service.modoEscuro()).toBe(false);
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });

  it('respeita o prefers-color-scheme na primeira visita', () => {
    simularSistema(true);
    const service = new ThemeService();

    expect(service.tema()).toBe('escuro');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });

  it('prioriza a escolha salva sobre o sistema', () => {
    simularSistema(true);
    localStorage.setItem(CHAVE_TEMA_ARMAZENADO, 'claro');
    const service = new ThemeService();

    expect(service.tema()).toBe('claro');
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });

  it('alternar() troca o tema, aplica a classe e persiste', () => {
    simularSistema(false);
    const service = new ThemeService();

    service.alternar();

    expect(service.tema()).toBe('escuro');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
    expect(localStorage.getItem(CHAVE_TEMA_ARMAZENADO)).toBe('escuro');

    service.alternar();

    expect(service.tema()).toBe('claro');
    expect(document.documentElement.classList.contains('dark')).toBe(false);
    expect(localStorage.getItem(CHAVE_TEMA_ARMAZENADO)).toBe('claro');
  });

  it('ignora valor salvo inválido e usa o sistema', () => {
    simularSistema(true);
    localStorage.setItem(CHAVE_TEMA_ARMAZENADO, 'neon');
    const service = new ThemeService();

    expect(service.tema()).toBe('escuro');
  });

  it('aplica o tema em memória mas não grava no localStorage quando cookies de preferência foram recusados', () => {
    simularSistema(false);
    localStorage.setItem(
      'fatec-lgpd-cookies',
      JSON.stringify({
        versao: 1,
        essenciais: true,
        preferencias: false,
        analiticos: false,
        atualizadoEm: '2026-11-17T00:00:00.000Z',
      }),
    );
    const service = new ThemeService();

    service.alternar();

    expect(service.tema()).toBe('escuro');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
    expect(localStorage.getItem(CHAVE_TEMA_ARMAZENADO)).toBeNull();
  });
});
