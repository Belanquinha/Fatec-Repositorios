import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { By } from '@angular/platform-browser';
import { describe, beforeEach, afterEach, it, expect, vi } from 'vitest';

import { Configuracoes, CHAVE_FONTE_ARMAZENADA, CHAVE_EMAILS_ATIVIDADE } from './configuracoes';
import { AuthService } from '../../core/auth/auth.service';
import { ThemeService, CHAVE_TEMA_ARMAZENADO } from '../../core/theme/theme.service';
import { environment } from '../../../environments/environment';

describe('Configuracoes', () => {
  let component: Configuracoes;
  let fixture: ComponentFixture<Configuracoes>;
  let httpTesting: HttpTestingController;

  const mockUsuarioAtual = signal<{ nome: string; email: string; role: string } | null>({
    nome: 'Carlos Aluno',
    email: 'carlos.aluno@fatec.sp.gov.br',
    role: 'ALUNO',
  });

  const mockAuthService = {
    usuarioAtual: mockUsuarioAtual,
    logout: vi.fn().mockResolvedValue(undefined),
    limparSessaoLocal: vi.fn(),
  };

  beforeEach(async () => {
    localStorage.clear();
    mockUsuarioAtual.set({
      nome: 'Carlos Aluno',
      email: 'carlos.aluno@fatec.sp.gov.br',
      role: 'ALUNO',
    });
    vi.clearAllMocks();

    await TestBed.configureTestingModule({
      imports: [Configuracoes],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: mockAuthService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Configuracoes);
    component = fixture.componentInstance;
    httpTesting = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
    document.documentElement.style.fontSize = '';
  });

  it('deve criar o componente', () => {
    expect(component).toBeTruthy();
  });

  /* --------------------------------------------------------------------------
     1. Tema
     -------------------------------------------------------------------------- */
  it('permite alternar entre tema claro, escuro e sistema', () => {
    const btnClaro = fixture.nativeElement.querySelector('[data-tema-claro]') as HTMLButtonElement;
    const btnEscuro = fixture.nativeElement.querySelector('[data-tema-escuro]') as HTMLButtonElement;
    const btnSistema = fixture.nativeElement.querySelector('[data-tema-sistema]') as HTMLButtonElement;

    expect(btnClaro).not.toBeNull();
    expect(btnEscuro).not.toBeNull();
    expect(btnSistema).not.toBeNull();

    // Ativa tema claro
    btnClaro.click();
    fixture.detectChanges();
    expect(component.modoTema()).toBe('claro');
    expect(localStorage.getItem(CHAVE_TEMA_ARMAZENADO)).toBe('claro');

    // Ativa tema escuro
    btnEscuro.click();
    fixture.detectChanges();
    expect(component.modoTema()).toBe('escuro');
    expect(localStorage.getItem(CHAVE_TEMA_ARMAZENADO)).toBe('escuro');

    // Ativa modo sistema
    btnSistema.click();
    fixture.detectChanges();
    expect(component.modoTema()).toBe('sistema');
    expect(localStorage.getItem(CHAVE_TEMA_ARMAZENADO)).toBeNull();
  });

  /* --------------------------------------------------------------------------
     2. Tamanho da Fonte
     -------------------------------------------------------------------------- */
  it('permite ajustar tamanho de fonte e persiste no localStorage', () => {
    const btnSm = fixture.nativeElement.querySelector('[data-fonte-sm]') as HTMLButtonElement;
    const btnLg = fixture.nativeElement.querySelector('[data-fonte-lg]') as HTMLButtonElement;
    const btnXl = fixture.nativeElement.querySelector('[data-fonte-xl]') as HTMLButtonElement;
    const btnMd = fixture.nativeElement.querySelector('[data-fonte-md]') as HTMLButtonElement;

    // Grande
    btnLg.click();
    fixture.detectChanges();
    expect(component.tamanhoFonte()).toBe('lg');
    expect(document.documentElement.style.fontSize).toBe('18px');
    expect(localStorage.getItem(CHAVE_FONTE_ARMAZENADA)).toBe('18px');

    // Extra Grande
    btnXl.click();
    fixture.detectChanges();
    expect(component.tamanhoFonte()).toBe('xl');
    expect(document.documentElement.style.fontSize).toBe('20px');
    expect(localStorage.getItem(CHAVE_FONTE_ARMAZENADA)).toBe('20px');

    // Pequeno
    btnSm.click();
    fixture.detectChanges();
    expect(component.tamanhoFonte()).toBe('sm');
    expect(document.documentElement.style.fontSize).toBe('14px');
    expect(localStorage.getItem(CHAVE_FONTE_ARMAZENADA)).toBe('14px');

    // Médio (Padrão) remove override
    btnMd.click();
    fixture.detectChanges();
    expect(component.tamanhoFonte()).toBe('md');
    expect(document.documentElement.style.fontSize).toBe('');
    expect(localStorage.getItem(CHAVE_FONTE_ARMAZENADA)).toBeNull();
  });

  /* --------------------------------------------------------------------------
     3. Notificações de E-mail
     -------------------------------------------------------------------------- */
  it('permite alternar o envio de emails de atividade', () => {
    const toggle = fixture.nativeElement.querySelector('[data-toggle-email]') as HTMLButtonElement;
    expect(toggle).not.toBeNull();

    // Começa ativo por padrão
    expect(component.notificacoesEmail()).toBe(true);

    // Clica para desativar
    toggle.click();
    fixture.detectChanges();
    expect(component.notificacoesEmail()).toBe(false);
    expect(localStorage.getItem(CHAVE_EMAILS_ATIVIDADE)).toBe('false');

    // Clica para reativar
    toggle.click();
    fixture.detectChanges();
    expect(component.notificacoesEmail()).toBe(true);
    expect(localStorage.getItem(CHAVE_EMAILS_ATIVIDADE)).toBe('true');
  });

  /* --------------------------------------------------------------------------
     4. Sair da Conta
     -------------------------------------------------------------------------- */
  it('chama logout ao clicar em Sair', () => {
    const btnSair = fixture.nativeElement.querySelector('[data-btn-sair]') as HTMLButtonElement;
    expect(btnSair).not.toBeNull();

    btnSair.click();
    expect(mockAuthService.logout).toHaveBeenCalled();
  });

  /* --------------------------------------------------------------------------
     5. Exclusão de Conta
     -------------------------------------------------------------------------- */
  it('abre modal de exclusão e só habilita confirmação com texto exato', () => {
    const btnAbrir = fixture.nativeElement.querySelector('[data-btn-abrir-exclusao]') as HTMLButtonElement;
    btnAbrir.click();
    fixture.detectChanges();

    expect(component.modalExclusaoAberto()).toBe(true);

    const inputConfirmar = fixture.nativeElement.querySelector('[data-input-confirmar-exclusao]') as HTMLInputElement;
    const btnConfirmar = fixture.nativeElement.querySelector('[data-btn-confirmar-exclusao]') as HTMLButtonElement;

    expect(inputConfirmar).not.toBeNull();
    expect(btnConfirmar.disabled).toBe(true);

    // Texto incorreto
    component.textoConfirmacao.set('excluir conta');
    fixture.detectChanges();
    expect(btnConfirmar.disabled).toBe(true);

    // Texto correto
    component.textoConfirmacao.set('excluir minha conta');
    fixture.detectChanges();
    expect(btnConfirmar.disabled).toBe(false);

    // Clica para confirmar e simula resposta do backend
    btnConfirmar.click();
    fixture.detectChanges();

    const req = httpTesting.expectOne(`${environment.apiUrl}/usuarios/me`);
    expect(req.request.method).toBe('DELETE');
    req.flush({ mensagem: 'Conta removida com sucesso' });

    expect(mockAuthService.limparSessaoLocal).toHaveBeenCalled();
    expect(component.modalExclusaoAberto()).toBe(false);
  });
});
