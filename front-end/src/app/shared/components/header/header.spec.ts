import { ApplicationRef, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';

import { AuthService } from '../../../core/auth/auth.service';
import { MicrosoftLoginButton } from '../microsoft-login-button/microsoft-login-button';
import { Header } from './header';

describe('Header', () => {
  let component: Header;
  let fixture: ComponentFixture<Header>;
  let authServiceDouble: {
    quandoPronto: ReturnType<typeof vi.fn>;
    inicializar: ReturnType<typeof vi.fn>;
    obterUsuarioLogado: ReturnType<typeof vi.fn>;
    loginMicrosoft: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
    isAdmin: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    authServiceDouble = {
      quandoPronto: vi.fn().mockResolvedValue(undefined),
      inicializar: vi.fn().mockResolvedValue(undefined),
      obterUsuarioLogado: vi.fn().mockResolvedValue(null),
      loginMicrosoft: vi.fn().mockResolvedValue(undefined),
      logout: vi.fn(),
      isAdmin: vi.fn().mockReturnValue(false),
    };

    await TestBed.configureTestingModule({
      imports: [Header],
      providers: [provideRouter([]), { provide: AuthService, useValue: authServiceDouble }],
    })
      .overrideComponent(Header, {
        // O filho `MicrosoftLoginButton` também chama markForCheck(). Removê-lo garante que
        // renderizar depois do `await` dependa só do header notificar o scheduler zoneless.
        remove: { imports: [MicrosoftLoginButton] },
        add: { schemas: [CUSTOM_ELEMENTS_SCHEMA] },
      })
      .compileComponents();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  /** Faz `quandoPronto()` só resolver quando o teste chamar o retorno. */
  function inicializacaoPendente(): () => void {
    let liberar!: () => void;
    authServiceDouble.quandoPronto.mockReturnValue(
      new Promise<void>((resolve) => {
        liberar = resolve;
      })
    );
    return liberar;
  }

  function criarHeader(): void {
    fixture = TestBed.createComponent(Header);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  /**
   * A aplicação é zoneless: o que roda a renderização é o `ApplicationRef.tick()` agendado pelo
   * `markForCheck()`. Reproduzir esse tick é o que prova que a UI reflete a sessão sozinha,
   * sem depender de um `detectChanges()` manual.
   */
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

  it('deve trocar o link de login pelo perfil na tela após a inicialização', async () => {
    const liberar = inicializacaoPendente();
    authServiceDouble.obterUsuarioLogado.mockResolvedValue({
      nome: 'Aluno da CPS',
      email: 'aluno@aluno.cps.sp.gov.br',
    });

    criarHeader();

    const htmlAntes = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(htmlAntes).toContain('Login institucional');

    liberar();
    await renderizar();

    const htmlDepois = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(htmlDepois).not.toContain('Login institucional');
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
});

/**
 * Item 24: drawer a11y — role/dialog + aria-modal ao abrir, inert ao fechar,
 * retorno de foco, Escape e preservação do overflow anterior do body.
 */
describe('Header (drawer a11y, item 24)', () => {
  let component: Header;
  let fixture: ComponentFixture<Header>;
  let authServiceDouble: {
    quandoPronto: ReturnType<typeof vi.fn>;
    obterUsuarioLogado: ReturnType<typeof vi.fn>;
    loginMicrosoft: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
    isAdmin: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    authServiceDouble = {
      quandoPronto: vi.fn().mockResolvedValue(undefined),
      obterUsuarioLogado: vi.fn().mockResolvedValue(null),
      loginMicrosoft: vi.fn().mockResolvedValue(undefined),
      logout: vi.fn(),
      isAdmin: vi.fn().mockReturnValue(false),
    };

    await TestBed.configureTestingModule({
      imports: [Header],
      providers: [provideRouter([]), { provide: AuthService, useValue: authServiceDouble }],
    })
      .overrideComponent(Header, {
        remove: { imports: [MicrosoftLoginButton] },
        add: { schemas: [CUSTOM_ELEMENTS_SCHEMA] },
      })
      .compileComponents();

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
      fixture.nativeElement.querySelectorAll('[data-menu-toggle]')
    ) as HTMLElement[];
  }

  /**
   * Mesmo padrão do bloco acima (zoneless): descarrega a navegação inicial do
   * Router e as continuações de `iniciarSessao()` antes de alternar o drawer.
   */
  async function estabilizar(): Promise<void> {
    await new Promise<void>((resolve) => setTimeout(resolve, 0));
    TestBed.inject(ApplicationRef).tick();
  }

  /**
   * O drawer é alternado por clique real (como o usuário faz), nunca por
   * chamada imperativa + `detectChanges`: `estadoDoMenuAberto` é uma
   * propriedade simples e, no modo zoneless, só o evento notifica o
   * scheduler e marca a view como suja. Chamada direta deixa a view limpa —
   * o `detectChanges` vira no-op e o `checkNoChanges` do dev-mode acusa
   * NG0100 em `aria-expanded` (quirk pré-existente do harness, reproduz no
   * código original sem as mudanças de a11y).
   */
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
      '[data-menu-toggle]'
    ) as HTMLElement;
    toggle.focus();
    expect(document.activeElement).toBe(toggle);

    component.mudarMenu();
    component.fecharMenu();

    expect(document.activeElement).toBe(toggle);
  });
});
