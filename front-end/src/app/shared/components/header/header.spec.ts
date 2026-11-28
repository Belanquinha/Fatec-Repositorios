import { ApplicationRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';

import { AuthService } from '../../../core/auth/auth.service';
import { Header } from './header';

describe('Header', () => {
  let component: Header;
  let fixture: ComponentFixture<Header>;
  let authServiceDouble: {
    quandoPronto: ReturnType<typeof vi.fn>;
    inicializar: ReturnType<typeof vi.fn>;
    obterUsuarioLogado: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
    isAdmin: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    authServiceDouble = {
      quandoPronto: vi.fn().mockResolvedValue(undefined),
      inicializar: vi.fn().mockResolvedValue(undefined),
      obterUsuarioLogado: vi.fn().mockResolvedValue(null),
      logout: vi.fn(),
      isAdmin: vi.fn().mockReturnValue(false),
    };

    await TestBed.configureTestingModule({
      imports: [Header],
      providers: [provideRouter([]), { provide: AuthService, useValue: authServiceDouble }],
    }).compileComponents();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  function inicializacaoPendente(): () => void {
    let liberar!: () => void;
    authServiceDouble.quandoPronto.mockReturnValue(
      new Promise<void>((resolve) => {
        liberar = resolve;
      }),
    );
    return liberar;
  }

  function criarHeader(): void {
    fixture = TestBed.createComponent(Header);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  async function renderizar(): Promise<void> {
    await new Promise<void>((resolve) => setTimeout(resolve, 0));
    TestBed.inject(ApplicationRef).tick();
  }

  it('should create', () => {
    criarHeader();

    expect(component).toBeTruthy();
  });

  it('deve aguardar quandoPronto() antes de consultar o usuário', async () => {
    const liberar = inicializacaoPendente();

    criarHeader();

    expect(authServiceDouble.quandoPronto).toHaveBeenCalledTimes(1);
    expect(authServiceDouble.obterUsuarioLogado).not.toHaveBeenCalled();
    expect(component.logado()).toBe(false);

    liberar();
    await renderizar();

    expect(authServiceDouble.obterUsuarioLogado).toHaveBeenCalled();
  });

  it('visitante vê botões apontando para /entrar no topo e no menu lateral', async () => {
    criarHeader();
    await renderizar();

    const btnTopo = fixture.nativeElement.querySelector('[data-header-login]') as HTMLAnchorElement;
    const btnDrawer = fixture.nativeElement.querySelector('[data-drawer-login]') as HTMLAnchorElement;

    expect(btnTopo).not.toBeNull();
    expect(btnTopo.getAttribute('href')).toBe('/entrar');
    expect(btnDrawer).not.toBeNull();
    expect(btnDrawer.getAttribute('href')).toBe('/entrar');
  });

  it('deve refletir o estado de sessão depois que o MSAL inicializa', async () => {
    const liberar = inicializacaoPendente();
    authServiceDouble.obterUsuarioLogado.mockResolvedValue({
      nome: 'Aluno da CPS',
      email: 'aluno@aluno.cps.sp.gov.br',
    });

    criarHeader();

    expect(component.logado()).toBe(false);
    expect(component.usuario()).toBeNull();

    liberar();
    await renderizar();

    expect(component.logado()).toBe(true);
    expect(component.usuario()?.email).toBe('aluno@aluno.cps.sp.gov.br');
    expect(component.primeiroNome()).toBe('Aluno');
  });

  it('deve trocar o botão de entrar pelo perfil na tela após a inicialização', async () => {
    const liberar = inicializacaoPendente();
    authServiceDouble.obterUsuarioLogado.mockResolvedValue({
      nome: 'Aluno da CPS',
      email: 'aluno@aluno.cps.sp.gov.br',
    });

    criarHeader();

    expect(fixture.nativeElement.querySelector('[data-header-login]')).not.toBeNull();

    liberar();
    await renderizar();

    expect(fixture.nativeElement.querySelector('[data-header-login]')).toBeNull();
    expect(fixture.nativeElement.querySelector('[data-drawer-login]')).toBeNull();
    const htmlDepois = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(htmlDepois).toContain('Aluno');
  });

  it('não deve lançar quando a inicialização do MSAL falha', async () => {
    const erroSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    authServiceDouble.quandoPronto.mockRejectedValue(new Error('falha de rede'));

    criarHeader();
    await renderizar();

    expect(component.logado()).toBe(false);
    expect(component.usuario()).toBeNull();
    expect(erroSpy).toHaveBeenCalled();
  });

  it('visitante não vê o link "Upload de projeto" no drawer', async () => {
    criarHeader();
    await renderizar();

    const nav = fixture.nativeElement.querySelector('#menu-lateral') as HTMLElement;
    expect(nav.querySelector('a[href="/selecionar-modelo"]')).toBeNull();
  });

  it('aluno vê o link "Upload de projeto" no drawer', async () => {
    authServiceDouble.obterUsuarioLogado.mockResolvedValue({
      nome: 'Aluno da CPS',
      email: 'aluno@aluno.cps.sp.gov.br',
      role: 'ALUNO',
    });

    criarHeader();
    await renderizar();

    const nav = fixture.nativeElement.querySelector('#menu-lateral') as HTMLElement;
    expect(nav.querySelector('a[href="/selecionar-modelo"]')).not.toBeNull();
  });

  it('ADMIN não vê o link "Upload de projeto" no drawer', async () => {
    authServiceDouble.obterUsuarioLogado.mockResolvedValue({
      nome: 'Admin da CPS',
      email: 'admin@cps.sp.gov.br',
      role: 'ADMIN',
    });

    criarHeader();
    await renderizar();

    const nav = fixture.nativeElement.querySelector('#menu-lateral') as HTMLElement;
    expect(nav.querySelector('a[href="/selecionar-modelo"]')).toBeNull();
  });
});

describe('Header (drawer a11y, item 24)', () => {
  let component: Header;
  let fixture: ComponentFixture<Header>;
  let authServiceDouble: {
    quandoPronto: ReturnType<typeof vi.fn>;
    obterUsuarioLogado: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
    isAdmin: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    authServiceDouble = {
      quandoPronto: vi.fn().mockResolvedValue(undefined),
      obterUsuarioLogado: vi.fn().mockResolvedValue(null),
      logout: vi.fn(),
      isAdmin: vi.fn().mockReturnValue(false),
    };

    await TestBed.configureTestingModule({
      imports: [Header],
      providers: [provideRouter([]), { provide: AuthService, useValue: authServiceDouble }],
    }).compileComponents();

    fixture = TestBed.createComponent(Header);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    document.body.style.overflow = '';
    vi.restoreAllMocks();
  });

  function navEl(): HTMLElement {
    return fixture.nativeElement.querySelector('#menu-lateral') as HTMLElement;
  }

  function toggles(): HTMLElement[] {
    return Array.from(
      fixture.nativeElement.querySelectorAll('[data-menu-toggle]'),
    ) as HTMLElement[];
  }

  async function estabilizar(): Promise<void> {
    await new Promise<void>((resolve) => setTimeout(resolve, 0));
    TestBed.inject(ApplicationRef).tick();
  }

  function clicarToggleAbrir(): void {
    toggles()[0].click();
    fixture.detectChanges();
  }

  function clicarToggleFechar(): void {
    const lista = toggles();
    lista[lista.length - 1].click();
    fixture.detectChanges();
  }

  it('expõe role=dialog + aria-modal aberto e inert + aria-hidden fechado', async () => {
    await estabilizar();
    expect(navEl().getAttribute('aria-hidden')).toBe('true');
    expect(navEl().hasAttribute('inert')).toBe(true);
    expect(navEl().getAttribute('role')).toBeNull();

    clicarToggleAbrir();

    expect(navEl().getAttribute('role')).toBe('dialog');
    expect(navEl().getAttribute('aria-modal')).toBe('true');
    expect(navEl().hasAttribute('inert')).toBe(false);

    clicarToggleFechar();

    expect(navEl().getAttribute('aria-hidden')).toBe('true');
    expect(navEl().hasAttribute('inert')).toBe(true);
  });

  it('fecha com Escape (HostListener)', async () => {
    await estabilizar();
    clicarToggleAbrir();
    expect(component.estadoDoMenuAberto).toBe(true);

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }));
    fixture.detectChanges();

    expect(component.estadoDoMenuAberto).toBe(false);
  });

  it('restaura o overflow anterior do body em vez de zerar', () => {
    document.body.style.overflow = 'auto';
    component.mudarMenu();
    expect(document.body.style.overflow).toBe('hidden');

    component.fecharMenu();
    expect(document.body.style.overflow).toBe('auto');
  });

  it('retorna o foco ao toggle ao fechar', () => {
    const toggle = fixture.nativeElement.querySelector(
      '[data-menu-toggle]',
    ) as HTMLElement;
    toggle.focus();
    expect(document.activeElement).toBe(toggle);

    component.mudarMenu();
    component.fecharMenu();

    expect(document.activeElement).toBe(toggle);
  });
});

describe('Header (modo escuro)', () => {
  let component: Header;
  let fixture: ComponentFixture<Header>;
  let authServiceDouble: {
    quandoPronto: ReturnType<typeof vi.fn>;
    obterUsuarioLogado: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
    isAdmin: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    authServiceDouble = {
      quandoPronto: vi.fn().mockResolvedValue(undefined),
      obterUsuarioLogado: vi.fn().mockResolvedValue(null),
      logout: vi.fn(),
      isAdmin: vi.fn().mockReturnValue(false),
    };

    await TestBed.configureTestingModule({
      imports: [Header],
      providers: [provideRouter([]), { provide: AuthService, useValue: authServiceDouble }],
    }).compileComponents();

    localStorage.removeItem('fatec-tema');
    document.documentElement.classList.remove('dark');
    fixture = TestBed.createComponent(Header);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.removeItem('fatec-tema');
    document.documentElement.classList.remove('dark');
    document.body.style.overflow = '';
    vi.restoreAllMocks();
  });

  async function estabilizar(): Promise<void> {
    await new Promise<void>((resolve) => setTimeout(resolve, 0));
    TestBed.inject(ApplicationRef).tick();
  }

  function interruptor(): HTMLElement {
    return fixture.nativeElement.querySelector('[role="switch"]') as HTMLElement;
  }

  it('expõe o switch com aria-checked refletindo o tema', async () => {
    await estabilizar();
    expect(interruptor().getAttribute('aria-checked')).toBe('false');
    expect(component.modoEscuro()).toBe(false);
  });

  it('alterna para escuro ao clicar, sem fechar o drawer', async () => {
    await estabilizar();
    (fixture.nativeElement.querySelector('[data-menu-toggle]') as HTMLElement).click();
    fixture.detectChanges();
    expect(component.estadoDoMenuAberto).toBe(true);

    interruptor().click();
    fixture.detectChanges();

    expect(component.modoEscuro()).toBe(true);
    expect(interruptor().getAttribute('aria-checked')).toBe('true');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
    expect(localStorage.getItem('fatec-tema')).toBe('escuro');
    expect(component.estadoDoMenuAberto).toBe(true);
  });

  it('permite alternar o modo escuro diretamente pelo botão rápido da barra superior', async () => {
    await estabilizar();
    const btnTopo = fixture.nativeElement.querySelector(
      '[data-header-theme-toggle]',
    ) as HTMLButtonElement;
    expect(btnTopo).not.toBeNull();
    expect(btnTopo.getAttribute('aria-pressed')).toBe('false');

    btnTopo.click();
    fixture.detectChanges();

    expect(component.modoEscuro()).toBe(true);
    expect(btnTopo.getAttribute('aria-pressed')).toBe('true');
    expect(document.documentElement.classList.contains('dark')).toBe(true);
  });
});
