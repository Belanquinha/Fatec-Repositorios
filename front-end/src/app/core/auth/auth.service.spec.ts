import { TestBed } from '@angular/core/testing';
import { MSAL_INSTANCE } from '@azure/msal-angular';
import { IPublicClientApplication } from '@azure/msal-browser';

import { AuthService } from './auth.service';

interface InstanciaMsalDouble {
  initialize: ReturnType<typeof vi.fn>;
  handleRedirectPromise: ReturnType<typeof vi.fn>;
  getAllAccounts: ReturnType<typeof vi.fn>;
  loginRedirect: ReturnType<typeof vi.fn>;
  logoutRedirect: ReturnType<typeof vi.fn>;
  acquireTokenSilent: ReturnType<typeof vi.fn>;
}

function criarInstanciaMsal(overrides: Partial<InstanciaMsalDouble> = {}): InstanciaMsalDouble {
  return {
    initialize: vi.fn().mockResolvedValue(undefined),
    handleRedirectPromise: vi.fn().mockResolvedValue(null),
    getAllAccounts: vi.fn().mockReturnValue([]),
    loginRedirect: vi.fn().mockResolvedValue(undefined),
    logoutRedirect: vi.fn().mockResolvedValue(undefined),
    acquireTokenSilent: vi.fn().mockResolvedValue({ accessToken: 'token-graph' }),
    ...overrides,
  };
}

function criarAuthService(instancia: InstanciaMsalDouble): AuthService {
  TestBed.configureTestingModule({
    providers: [
      AuthService,
      { provide: MSAL_INSTANCE, useValue: instancia as unknown as IPublicClientApplication },
    ],
  });
  return TestBed.inject(AuthService);
}

describe('AuthService', () => {
  afterEach(() => {
    sessionStorage.clear();
  });

  describe('conta', () => {
    it('deve devolver undefined antes da inicialização, sem tocar no cache do MSAL', () => {
      const instancia = criarInstanciaMsal();
      const authService = criarAuthService(instancia);

      expect(authService.conta).toBeUndefined();
      expect(instancia.getAllAccounts).not.toHaveBeenCalled();
    });

    it('nunca deve lançar uninitialized_public_client_application em acesso antecipado', () => {
      const instancia = criarInstanciaMsal({
        getAllAccounts: vi.fn(() => {
          throw new Error('uninitialized_public_client_application');
        }),
      });
      const authService = criarAuthService(instancia);

      expect(() => authService.conta).not.toThrow();
      expect(authService.conta).toBeUndefined();
    });

    it('deve devolver a primeira conta após a inicialização', async () => {
      const conta = { name: 'Aluno', username: 'aluno@aluno.cps.sp.gov.br' };
      const instancia = criarInstanciaMsal({ getAllAccounts: vi.fn().mockReturnValue([conta]) });
      const authService = criarAuthService(instancia);

      await authService.inicializar();

      expect(authService.conta).toBe(conta);
    });

    it('deve devolver undefined quando não há conta alguma', async () => {
      const authService = criarAuthService(criarInstanciaMsal());

      await authService.inicializar();

      expect(authService.conta).toBeUndefined();
    });
  });

  describe('inicializar', () => {
    it('deve chamar initialize() uma única vez quando chamado várias vezes em sequência', async () => {
      const instancia = criarInstanciaMsal();
      const authService = criarAuthService(instancia);

      await authService.inicializar();
      await authService.inicializar();
      await authService.inicializar();

      expect(instancia.initialize).toHaveBeenCalledTimes(1);
      expect(instancia.handleRedirectPromise).toHaveBeenCalledTimes(1);
    });

    it('deve chamar initialize() uma única vez com dois componentes concorrentes', async () => {
      const instancia = criarInstanciaMsal();
      const authService = criarAuthService(instancia);

      await Promise.all([authService.inicializar(), authService.inicializar()]);

      expect(instancia.initialize).toHaveBeenCalledTimes(1);
    });

    it('deve resolver o redirect pendente depois do initialize()', async () => {
      const instancia = criarInstanciaMsal();
      const authService = criarAuthService(instancia);

      await authService.inicializar();

      expect(instancia.initialize).toHaveBeenCalledTimes(1);
      expect(instancia.handleRedirectPromise).toHaveBeenCalledTimes(1);
    });
  });

  describe('quandoPronto', () => {
    it('deve aguardar a inicialização já disparada por outro componente', async () => {
      const instancia = criarInstanciaMsal();
      const authService = criarAuthService(instancia);

      const initPendente = authService.inicializar();
      const pronto = authService.quandoPronto();

      await Promise.all([initPendente, pronto]);

      expect(instancia.initialize).toHaveBeenCalledTimes(1);
    });

    it('deve inicializar quando ainda ninguém chamou inicializar()', async () => {
      const instancia = criarInstanciaMsal();
      const authService = criarAuthService(instancia);

      await authService.quandoPronto();

      expect(instancia.initialize).toHaveBeenCalledTimes(1);
    });

    it('deve resolver a mesma promessa para chamadas concorrentes', async () => {
      const instancia = criarInstanciaMsal();
      const authService = criarAuthService(instancia);

      const [a, b] = await Promise.all([authService.quandoPronto(), authService.quandoPronto()]);

      expect(a).toBe(b);
      expect(instancia.initialize).toHaveBeenCalledTimes(1);
    });
  });

  describe('cache de sessão corrompido', () => {
    it('deve limpar as chaves msal.* do sessionStorage em no_token_request_cache_error', async () => {
      sessionStorage.setItem('msal.<id>-accesstoken', 'token-corrompido');
      sessionStorage.setItem('msal.<id>-account-id', 'conta');
      sessionStorage.setItem('msal.<id>-appMetadata', '{}');
      sessionStorage.setItem('outra-chave', 'deve-sobreviver');

      const instancia = criarInstanciaMsal({
        handleRedirectPromise: vi.fn().mockRejectedValue({ errorCode: 'no_token_request_cache_error' }),
      });
      const authService = criarAuthService(instancia);

      await expect(authService.inicializar()).resolves.toBeUndefined();

      expect(sessionStorage.getItem('msal.<id>-accesstoken')).toBeNull();
      expect(sessionStorage.getItem('msal.<id>-account-id')).toBeNull();
      expect(sessionStorage.getItem('msal.<id>-appMetadata')).toBeNull();
      expect(sessionStorage.getItem('outra-chave')).toBe('deve-sobreviver');
    });

    it('deve propagar erros que não sejam de cache de sessão', async () => {
      const instancia = criarInstanciaMsal({
        handleRedirectPromise: vi.fn().mockRejectedValue({ errorCode: 'outro_erro' }),
      });
      const authService = criarAuthService(instancia);

      await expect(authService.inicializar()).rejects.toMatchObject({ errorCode: 'outro_erro' });
    });
  });

  describe('login institucional', () => {
    it('não deve mais expor loginInstituicao nem chamar POST /auth/login', () => {
      const authService = criarAuthService(criarInstanciaMsal());

      expect((authService as unknown as Record<string, unknown>)['loginInstituicao']).toBeUndefined();
    });
  });

  describe('loginMicrosoft', () => {
    it('deve aguardar a inicialização antes de chamar loginRedirect', async () => {
      let liberar!: () => void;
      const instancia = criarInstanciaMsal({
        initialize: vi.fn(() => new Promise<void>((resolve) => (liberar = resolve))),
      });
      const authService = criarAuthService(instancia);

      const login = authService.loginMicrosoft();
      await Promise.resolve();

      // Ainda inicializando: tocar na instância agora lançaria uninitialized_public_client_application.
      expect(instancia.loginRedirect).not.toHaveBeenCalled();

      liberar();
      await login;

      expect(instancia.initialize).toHaveBeenCalledTimes(1);
      expect(instancia.loginRedirect).toHaveBeenCalledTimes(1);
    });

    it('deve aguardar a inicialização disparada por outro componente', async () => {
      let liberar!: () => void;
      const instancia = criarInstanciaMsal({
        initialize: vi.fn(() => new Promise<void>((resolve) => (liberar = resolve))),
      });
      const authService = criarAuthService(instancia);

      const initPendente = authService.inicializar();
      const login = authService.loginMicrosoft();
      await Promise.resolve();

      expect(instancia.loginRedirect).not.toHaveBeenCalled();

      liberar();
      await Promise.all([initPendente, login]);

      expect(instancia.loginRedirect).toHaveBeenCalledTimes(1);
    });

    it('deve propagar a falha de inicialização sem chamar loginRedirect', async () => {
      const instancia = criarInstanciaMsal({
        initialize: vi.fn().mockRejectedValue(new Error('falha de rede')),
      });
      const authService = criarAuthService(instancia);

      await expect(authService.loginMicrosoft()).rejects.toThrow('falha de rede');
      expect(instancia.loginRedirect).not.toHaveBeenCalled();
    });
  });

  describe('logout', () => {
    it('deve aguardar a inicialização antes de chamar logoutRedirect', async () => {
      let liberar!: () => void;
      const instancia = criarInstanciaMsal({
        initialize: vi.fn(() => new Promise<void>((resolve) => (liberar = resolve))),
      });
      const authService = criarAuthService(instancia);

      const logout = authService.logout();
      await Promise.resolve();

      expect(instancia.logoutRedirect).not.toHaveBeenCalled();

      liberar();
      await logout;

      expect(instancia.logoutRedirect).toHaveBeenCalledTimes(1);
    });
  });

  describe('nova tentativa após falha de inicialização', () => {
    it('deve permitir retry: uma falha transitória não pode ser memorizada para sempre', async () => {
      const initialize = vi
        .fn()
        .mockRejectedValueOnce(new Error('falha de rede'))
        .mockResolvedValueOnce(undefined);
      const instancia = criarInstanciaMsal({ initialize });
      const authService = criarAuthService(instancia);

      await expect(authService.inicializar()).rejects.toThrow('falha de rede');
      await expect(authService.inicializar()).resolves.toBeUndefined();

      expect(initialize).toHaveBeenCalledTimes(2);
      expect(instancia.handleRedirectPromise).toHaveBeenCalledTimes(1);
    });

    it('quandoPronto() também deve conseguir repetir depois de uma falha', async () => {
      const initialize = vi
        .fn()
        .mockRejectedValueOnce(new Error('falha de rede'))
        .mockResolvedValueOnce(undefined);
      const authService = criarAuthService(criarInstanciaMsal({ initialize }));

      await expect(authService.quandoPronto()).rejects.toThrow('falha de rede');
      await expect(authService.quandoPronto()).resolves.toBeUndefined();

      expect(initialize).toHaveBeenCalledTimes(2);
    });
  });

  describe('conta após falha de inicialização', () => {
    it('não deve ler o cache do MSAL enquanto a inicialização ainda está em andamento', () => {
      // `pronto` é atribuído de forma síncrona, antes de `initialize()` resolver: testar apenas
      // a existência da promessa não distingue "em andamento" de "concluída com sucesso".
      const instancia = criarInstanciaMsal({
        initialize: vi.fn(() => new Promise<void>(() => {})),
      });
      const authService = criarAuthService(instancia);

      void authService.inicializar();

      expect(authService.conta).toBeUndefined();
      expect(instancia.getAllAccounts).not.toHaveBeenCalled();
    });

    it('não deve ler o cache do MSAL quando a inicialização falhou', async () => {
      const instancia = criarInstanciaMsal({
        initialize: vi.fn().mockRejectedValue(new Error('falha de rede')),
      });
      const authService = criarAuthService(instancia);

      await expect(authService.inicializar()).rejects.toThrow('falha de rede');

      // A promessa rejeitada é truthy, mas a instância continua não inicializada: ler o cache
      // aqui lançaria uninitialized_public_client_application.
      expect(instancia.getAllAccounts).not.toHaveBeenCalled();
      expect(authService.conta).toBeUndefined();
    });

    it('deve ler o cache normalmente depois que uma retentativa funciona', async () => {
      const conta = { name: 'Aluno', username: 'aluno@aluno.cps.sp.gov.br' };
      const initialize = vi
        .fn()
        .mockRejectedValueOnce(new Error('falha de rede'))
        .mockResolvedValueOnce(undefined);
      const instancia = criarInstanciaMsal({ initialize, getAllAccounts: vi.fn().mockReturnValue([conta]) });
      const authService = criarAuthService(instancia);

      await expect(authService.inicializar()).rejects.toThrow('falha de rede');
      expect(authService.conta).toBeUndefined();

      await authService.inicializar();

      expect(authService.conta).toBe(conta);
    });
  });
});
