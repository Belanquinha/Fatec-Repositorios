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
    expect(component.logado).toBe(false);

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

    expect(component.logado).toBe(false);
    expect(component.usuario).toBeNull();

    liberar();
    await renderizar();

    expect(component.logado).toBe(true);
    expect(component.usuario?.email).toBe('aluno@aluno.cps.sp.gov.br');
    expect(component.primeiroNome).toBe('Aluno');
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

    expect(component.logado).toBe(false);
    expect(component.usuario).toBeNull();
    expect(erroSpy).toHaveBeenCalled();
  });
});
