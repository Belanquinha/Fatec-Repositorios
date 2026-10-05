import { ApplicationRef } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { AuthService } from '../../../core/auth/auth.service';
import { MicrosoftLoginButton } from './microsoft-login-button';

describe('MicrosoftLoginButton', () => {
  let fixture: ComponentFixture<MicrosoftLoginButton>;
  let authServiceDouble: {
    inicializar: ReturnType<typeof vi.fn>;
    conta: unknown;
    loginMicrosoftViaApi: ReturnType<typeof vi.fn>;
    limparSessaoLocal: ReturnType<typeof vi.fn>;
    obterUsuarioLogado: ReturnType<typeof vi.fn>;
    loginMicrosoft: ReturnType<typeof vi.fn>;
    logout: ReturnType<typeof vi.fn>;
    isAdmin: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    authServiceDouble = {
      inicializar: vi.fn().mockResolvedValue(undefined),
      conta: undefined,
      loginMicrosoftViaApi: vi.fn().mockResolvedValue({}),
      limparSessaoLocal: vi.fn(),
      obterUsuarioLogado: vi.fn().mockResolvedValue(null),
      loginMicrosoft: vi.fn().mockResolvedValue(undefined),
      logout: vi.fn().mockResolvedValue(undefined),
      isAdmin: vi.fn().mockReturnValue(false),
    };

    await TestBed.configureTestingModule({
      imports: [MicrosoftLoginButton],
      providers: [{ provide: AuthService, useValue: authServiceDouble }],
    }).compileComponents();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  /** `inicializar()` que só resolve quando o teste chamar o retorno. */
  function inicializacaoPendente(): () => void {
    let liberar!: () => void;
    authServiceDouble.inicializar.mockReturnValue(
      new Promise<void>((resolve) => {
        liberar = resolve;
      })
    );
    return liberar;
  }

  function botaoLogin(): HTMLButtonElement {
    return (fixture.nativeElement as HTMLElement).querySelector('button') as HTMLButtonElement;
  }

  async function renderizar(): Promise<void> {
    await new Promise<void>((resolve) => setTimeout(resolve, 0));
    TestBed.inject(ApplicationRef).tick();
  }

  it('should create', () => {
    fixture = TestBed.createComponent(MicrosoftLoginButton);
    fixture.detectChanges();

    expect(fixture.componentInstance).toBeTruthy();
  });

  it('deve manter o botão desabilitado enquanto o MSAL inicializa', async () => {
    inicializacaoPendente();

    fixture = TestBed.createComponent(MicrosoftLoginButton);
    fixture.detectChanges();

    expect(botaoLogin().disabled).toBe(true);
  });

  it('deve habilitar o botão depois que a inicialização resolver', async () => {
    const liberar = inicializacaoPendente();

    fixture = TestBed.createComponent(MicrosoftLoginButton);
    fixture.detectChanges();

    expect(botaoLogin().disabled).toBe(true);

    liberar();
    await renderizar();

    // A aplicação é zoneless: só o markForCheck() do .finally() faz o estado chegar à tela.
    expect(fixture.componentInstance.inicializando).toBe(false);
    expect(botaoLogin().disabled).toBe(false);
  });

  it('deve habilitar o botão mesmo quando a inicialização falha', async () => {
    const erroSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    authServiceDouble.inicializar.mockRejectedValue(new Error('falha de rede'));

    fixture = TestBed.createComponent(MicrosoftLoginButton);
    fixture.detectChanges();

    await renderizar();

    expect(botaoLogin().disabled).toBe(false);
    expect(erroSpy).toHaveBeenCalled();
  });

  describe('login no back-end recusado', () => {
    beforeEach(() => {
      // A Microsoft autentica no navegador, mas o back-end não confirma a sessão.
      authServiceDouble.conta = { name: 'Aluno', username: 'aluno@aluno.cps.sp.gov.br' };
      authServiceDouble.loginMicrosoftViaApi.mockRejectedValue(new Error('backend fora do ar'));
    });

    it('deve descartar a sessão residual para não exibir um perfil que o back-end nunca validou', async () => {
      const erroSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      fixture = TestBed.createComponent(MicrosoftLoginButton);
      fixture.detectChanges();

      await renderizar();

      expect(authServiceDouble.limparSessaoLocal).toHaveBeenCalledTimes(1);
      expect(fixture.componentInstance.usuarioLogado).toBe(false);
      expect(erroSpy).toHaveBeenCalled();
    });

    it('não deve chamar obterUsuarioLogado antes de descartar a sessão', async () => {
      vi.spyOn(console, 'error').mockImplementation(() => {});

      fixture = TestBed.createComponent(MicrosoftLoginButton);
      fixture.detectChanges();

      await renderizar();

      const ordem = authServiceDouble.limparSessaoLocal.mock.invocationCallOrder[0];
      const consulta = authServiceDouble.obterUsuarioLogado.mock.invocationCallOrder[0];
      expect(ordem).toBeLessThan(consulta);
    });
  });

});
