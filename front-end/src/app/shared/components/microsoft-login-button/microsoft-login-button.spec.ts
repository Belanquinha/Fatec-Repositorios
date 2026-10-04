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
    contasDev: ReturnType<typeof vi.fn>;
    loginDev: ReturnType<typeof vi.fn>;
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
      contasDev: vi.fn().mockResolvedValue([]),
      loginDev: vi.fn().mockResolvedValue(undefined),
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

  describe('atalhos de desenvolvimento', () => {
    function botoesDev(): HTMLButtonElement[] {
      return Array.from(
        (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('.dev-login-botao')
      );
    }

    it('não deve renderizar atalhos quando a lista vem vazia', async () => {
      fixture = TestBed.createComponent(MicrosoftLoginButton);
      fixture.detectChanges();
      await renderizar();

      // É assim que o build de produção se comporta: a lista chega vazia e nada é desenhado.
      expect(botoesDev()).toHaveLength(0);
    });

    it('deve renderizar um botão por papel devolvido pelo back-end', async () => {
      authServiceDouble.contasDev.mockResolvedValue([
        { email: 'gabriel@aluno.cps.sp.gov.br', rotulo: 'Aluno' },
        { email: 'maria@cps.sp.gov.br', rotulo: 'Professor' },
        { email: 'admin@cps.sp.gov.br', rotulo: 'Admin' },
      ]);

      fixture = TestBed.createComponent(MicrosoftLoginButton);
      fixture.detectChanges();
      await renderizar();

      expect(botoesDev()).toHaveLength(3);
      expect(botoesDev().map((botao) => botao.textContent?.trim())).toEqual([
        'Aluno',
        'Professor',
        'Admin',
      ]);
    });

    it('deve pedir a lista de atalhos ao montar', async () => {
      fixture = TestBed.createComponent(MicrosoftLoginButton);
      fixture.detectChanges();
      await renderizar();

      expect(authServiceDouble.contasDev).toHaveBeenCalledTimes(1);
    });

    it('deve chamar loginDev com o e-mail do atalho clicado', async () => {
      authServiceDouble.contasDev.mockResolvedValue([
        { email: 'maria@cps.sp.gov.br', rotulo: 'Professor' },
      ]);
      fixture = TestBed.createComponent(MicrosoftLoginButton);
      fixture.detectChanges();
      await renderizar();

      await fixture.componentInstance.entrarComo({ email: 'maria@cps.sp.gov.br', rotulo: 'Professor' });

      expect(authServiceDouble.loginDev).toHaveBeenCalledWith('maria@cps.sp.gov.br');
    });

    it('deve recarregar a página após entrar, para que header e guardas leiam o papel novo', async () => {
      const recarregar = vi.fn();
      const original = window.location;
      Object.defineProperty(window, 'location', {
        configurable: true,
        value: { ...original, reload: recarregar },
      });

      try {
        authServiceDouble.contasDev.mockResolvedValue([
          { email: 'admin@cps.sp.gov.br', rotulo: 'Admin' },
        ]);
        fixture = TestBed.createComponent(MicrosoftLoginButton);
        fixture.detectChanges();
        await renderizar();

        await fixture.componentInstance.entrarComo({ email: 'admin@cps.sp.gov.br', rotulo: 'Admin' });

        // O header e os guardas leem a sessão uma vez, na inicialização. Sem recarregar, a tela
        // continuaria mostrando as permissões da conta anterior.
        expect(recarregar).toHaveBeenCalledTimes(1);
      } finally {
        Object.defineProperty(window, 'location', { configurable: true, value: original });
      }
    });

    it('não deve recarregar quando o login de desenvolvimento falha', async () => {
      const erroSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      const recarregar = vi.fn();
      const original = window.location;
      Object.defineProperty(window, 'location', {
        configurable: true,
        value: { ...original, reload: recarregar },
      });

      try {
        authServiceDouble.loginDev.mockRejectedValue(new Error('E-mail inválido'));
        authServiceDouble.contasDev.mockResolvedValue([
          { email: 'nao-e-email', rotulo: 'Quebrado' },
        ]);
        fixture = TestBed.createComponent(MicrosoftLoginButton);
        fixture.detectChanges();
        await renderizar();

        await fixture.componentInstance.entrarComo({ email: 'nao-e-email', rotulo: 'Quebrado' });

        expect(recarregar).not.toHaveBeenCalled();
      } finally {
        Object.defineProperty(window, 'location', { configurable: true, value: original });
      }
    });

    it('deve exibir a mensagem de erro devolvida pelo back-end', async () => {
      const erroSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
      authServiceDouble.loginDev.mockRejectedValue(new Error('E-mail inválido'));
      authServiceDouble.contasDev.mockResolvedValue([{ email: 'nao-e-email', rotulo: 'Quebrado' }]);

      fixture = TestBed.createComponent(MicrosoftLoginButton);
      fixture.detectChanges();
      await renderizar();

      await fixture.componentInstance.entrarComo({ email: 'nao-e-email', rotulo: 'Quebrado' });
      await renderizar();

      expect(erroSpy).toHaveBeenCalled();
      expect(fixture.componentInstance.erroDev).toBe('E-mail inválido');
      expect((fixture.nativeElement as HTMLElement).textContent).toContain('E-mail inválido');
    });

    it('não deve desabilitar os atalhos enquanto a inicialização do MSAL não resolve', async () => {
      inicializacaoPendente();
      authServiceDouble.contasDev.mockResolvedValue([
        { email: 'maria@cps.sp.gov.br', rotulo: 'Professor' },
      ]);

      fixture = TestBed.createComponent(MicrosoftLoginButton);
      fixture.detectChanges();
      await renderizar();

      // Os atalhos não dependem do MSAL: esperar pela Microsoft para testar professor significaria
      // continuar refazendo MFA, que é exatamente o que eles existem para evitar.
      expect(botaoLogin().disabled).toBe(true);
      expect(botoesDev()[0].disabled).toBe(false);
    });
  });
});
