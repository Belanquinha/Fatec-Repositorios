import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { vi } from 'vitest';
import { authGuard } from './auth.guard';
import { submissaoGuard } from './submissao.guard';
import { AuthService } from './auth.service';

describe('authGuard', () => {
  async function executar(usuario: unknown) {
    await TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { obterUsuarioLogado: vi.fn().mockResolvedValue(usuario) } },
      ],
    }).compileComponents();

    return TestBed.runInInjectionContext(() => authGuard(null as never, null as never));
  }

  it('libera a rota quando há sessão válida', async () => {
    await expect(
      executar({ nome: 'Aluno Teste', email: 'aluno.teste@aluno.cps.sp.gov.br', role: 'ALUNO' })
    ).resolves.toBe(true);
  });

  it('redireciona para a home quando não há sessão', async () => {
    const resultado = await executar(null);

    expect(resultado).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(resultado as UrlTree)).toBe('/');
  });
});

/**
 * Submissão de projeto é de aluno e professor. ADMIN é conta de gestão do
 * catálogo (CRUD de instituições), sem fluxo de submissão — barra na rota.
 */
describe('submissaoGuard', () => {
  async function executar(usuario: unknown) {
    await TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { obterUsuarioLogado: vi.fn().mockResolvedValue(usuario) } },
      ],
    }).compileComponents();

    return TestBed.runInInjectionContext(() => submissaoGuard(null as never, null as never));
  }

  it('libera ALUNO', async () => {
    await expect(
      executar({ nome: 'Aluno', email: 'a@aluno.cps.sp.gov.br', role: 'ALUNO' })
    ).resolves.toBe(true);
  });

  it('libera PROFESSOR', async () => {
    await expect(
      executar({ nome: 'Prof', email: 'p@cps.sp.gov.br', role: 'PROFESSOR' })
    ).resolves.toBe(true);
  });

  it('redireciona ADMIN para a home', async () => {
    const resultado = await executar({ nome: 'Admin', email: 'admin@cps.sp.gov.br', role: 'ADMIN' });

    expect(resultado).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(resultado as UrlTree)).toBe('/');
  });

  it('redireciona para a home quando não há sessão', async () => {
    const resultado = await executar(null);

    expect(resultado).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(resultado as UrlTree)).toBe('/');
  });
});
