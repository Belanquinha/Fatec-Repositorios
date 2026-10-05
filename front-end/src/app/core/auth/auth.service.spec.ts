import { TestBed } from '@angular/core/testing';
import { MSAL_INSTANCE } from '@azure/msal-angular';
import { IPublicClientApplication } from '@azure/msal-browser';

import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

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

/**
 * JWT com o formato que `sessao-navegador` sabe ler: três segmentos, e um `exp` no payload.
 *
 * A assinatura não é validada no front-end (quem valida é o back-end), então `header.payload.`
 * com assinatura falsa serve. O que importa nos testes é o `exp`.
 */
function criarJwt(expEmSegundos: number): string {
  const payload = btoa(JSON.stringify({ exp: expEmSegundos }))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
  return `header.${payload}.assinatura`;
}

const EM_HORA = 3600;

/** Token válido por uma hora a partir de agora. */
function criarJwtValido(): string {
  return criarJwt(Math.floor(Date.now() / 1000) + EM_HORA);
}

describe('AuthService', () => {
  afterEach(() => {
    sessionStorage.clear();
    localStorage.clear();
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

  describe('obterUsuarioLogado', () => {
    /**
     * Regressão do bug relatado: com a conta no cache do MSAL mas sem sessão
     * confirmada pelo back-end, a UI exibia um perfil no canto. O cache do MSAL
     * prova apenas que a Microsoft autenticou a pessoa no navegador.
     */
    it('deve devolver null mesmo com conta no MSAL quando o back-end não respondeu', async () => {
      const conta = { name: 'Aluno', username: 'aluno@aluno.cps.sp.gov.br' };
      const authService = criarAuthService(
        criarInstanciaMsal({ getAllAccounts: vi.fn().mockReturnValue([conta]) })
      );
      await authService.inicializar();

      await expect(authService.obterUsuarioLogado()).resolves.toBeNull();
    });

    it('deve devolver null quando há identidade guardada mas o token foi removido', async () => {
      localStorage.setItem('usuarioNome', 'Aluno da CPS');
      localStorage.setItem('usuarioEmail', 'aluno@aluno.cps.sp.gov.br');
      localStorage.setItem('usuarioRole', 'ADMIN');

      const authService = criarAuthService(criarInstanciaMsal());

      await expect(authService.obterUsuarioLogado()).resolves.toBeNull();
      // O papel não pode sobreviver à sessão: isAdmin() já exige o token.
      expect(authService.isAdmin()).toBe(false);
    });

    it('deve devolver o usuário quando o back-end confirmou a sessão', async () => {
      localStorage.setItem('accessToken', criarJwtValido());
      localStorage.setItem('usuarioNome', 'Aluno da CPS');
      localStorage.setItem('usuarioEmail', 'aluno@aluno.cps.sp.gov.br');
      localStorage.setItem('usuarioFoto', 'https://exemplo.test/foto.png');
      localStorage.setItem('usuarioRole', 'ADMIN');

      const authService = criarAuthService(criarInstanciaMsal());

      await expect(authService.obterUsuarioLogado()).resolves.toEqual({
        nome: 'Aluno da CPS',
        email: 'aluno@aluno.cps.sp.gov.br',
        foto: 'https://exemplo.test/foto.png',
        role: 'ADMIN',
      });
      expect(authService.isAdmin()).toBe(true);
    });

    /**
     * A chave existir não é prova de nada: o JWT vence e o `localStorage` não. Sem esta checagem a
     * tela anunciava "logado" e toda chamada autenticada voltava 401 — o sintoma que fazia um login
     * de desenvolvimento parececido com defeito.
     */
    it('deve devolver null quando o token guardada venceu, mesmo com as chaves de perfil intactas', async () => {
      localStorage.setItem('accessToken', criarJwt(Math.floor(Date.now() / 1000) - 1));
      localStorage.setItem('usuarioNome', 'Aluno da CPS');
      localStorage.setItem('usuarioEmail', 'aluno@aluno.cps.sp.gov.br');
      localStorage.setItem('usuarioRole', 'ADMIN');

      const authService = criarAuthService(criarInstanciaMsal());

      await expect(authService.obterUsuarioLogado()).resolves.toBeNull();
      // O papel guardado ao lado de um token vencido descreveria uma sessão que a API não reconhece.
      expect(authService.isAdmin()).toBe(false);
    });

    it('deve descartar as chaves de sessão quando o token venceu, em vez de só ignorá-las', async () => {
      localStorage.setItem('accessToken', criarJwt(Math.floor(Date.now() / 1000) - 1));
      localStorage.setItem('usuarioNome', 'Aluno da CPS');
      localStorage.setItem('usuarioEmail', 'aluno@aluno.cps.sp.gov.br');

      const authService = criarAuthService(criarInstanciaMsal());
      await authService.obterUsuarioLogado();

      // Deixar o token vencido no lugar manteria a UI anunciando um login que nada consegue usar.
      expect(localStorage.getItem('accessToken')).toBeNull();
      expect(localStorage.getItem('usuarioNome')).toBeNull();
    });

    it('deve tratar token sem formato de JWT como vencido, sem lançar', async () => {
      localStorage.setItem('accessToken', 'jwt-da-sessao');
      localStorage.setItem('usuarioNome', 'Aluno da CPS');
      localStorage.setItem('usuarioEmail', 'aluno@aluno.cps.sp.gov.br');

      const authService = criarAuthService(criarInstanciaMsal());

      // Na dúvida o estado é "deslogado", que é o estado em que a pessoa ainda se recupera.
      await expect(authService.obterUsuarioLogado()).resolves.toBeNull();
    });

    it('deve tratar token com payload ilegível como vencido, sem lançar', async () => {
      localStorage.setItem('accessToken', 'header.nao-e-json.assinatura');
      localStorage.setItem('usuarioNome', 'Aluno da CPS');
      localStorage.setItem('usuarioEmail', 'aluno@aluno.cps.sp.gov.br');

      const authService = criarAuthService(criarInstanciaMsal());

      await expect(authService.obterUsuarioLogado()).resolves.toBeNull();
    });

    it('deve tratar token sem claim exp como vencido', async () => {
      localStorage.setItem('accessToken', `header.${btoa('{"sub":"x"}')}.assinatura`);
      localStorage.setItem('usuarioNome', 'Aluno da CPS');
      localStorage.setItem('usuarioEmail', 'aluno@aluno.cps.sp.gov.br');

      const authService = criarAuthService(criarInstanciaMsal());

      await expect(authService.obterUsuarioLogado()).resolves.toBeNull();
    });

    it('deve aceitar token recém-emitido, com exp no segundo seguinte', async () => {
      localStorage.setItem('accessToken', criarJwt(Math.floor(Date.now() / 1000) + 10));
      localStorage.setItem('usuarioNome', 'Aluno da CPS');
      localStorage.setItem('usuarioEmail', 'aluno@aluno.cps.sp.gov.br');

      const authService = criarAuthService(criarInstanciaMsal());

      await expect(authService.obterUsuarioLogado()).resolves.not.toBeNull();
    });
  });

  describe('limparSessaoLocal', () => {
    it('deve remover todas as chaves de sessão sem tocar no cache do MSAL', () => {
      localStorage.setItem('accessToken', 'jwt');
      localStorage.setItem('tokenType', 'Bearer');
      localStorage.setItem('expiresInSeconds', '3600');
      localStorage.setItem('usuarioNome', 'Aluno');
      localStorage.setItem('usuarioEmail', 'aluno@aluno.cps.sp.gov.br');
      localStorage.setItem('usuarioFoto', 'foto');
      localStorage.setItem('usuarioRole', 'ADMIN');
      localStorage.setItem('outra-chave', 'deve-sobreviver');

      const instancia = criarInstanciaMsal();
      const authService = criarAuthService(instancia);

      authService.limparSessaoLocal();

      expect(localStorage.getItem('accessToken')).toBeNull();
      expect(localStorage.getItem('usuarioNome')).toBeNull();
      expect(localStorage.getItem('usuarioEmail')).toBeNull();
      expect(localStorage.getItem('usuarioFoto')).toBeNull();
      expect(localStorage.getItem('usuarioRole')).toBeNull();
      expect(localStorage.getItem('outra-chave')).toBe('deve-sobreviver');
      expect(instancia.logoutRedirect).not.toHaveBeenCalled();
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
