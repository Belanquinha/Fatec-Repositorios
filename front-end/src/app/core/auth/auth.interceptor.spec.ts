import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { authInterceptor } from './auth.interceptor';

function criarContexto() {
  TestBed.configureTestingModule({
    providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()],
  });
  return {
    http: TestBed.inject(HttpClient),
    backend: TestBed.inject(HttpTestingController),
  };
}

function semearSessao(): void {
  localStorage.setItem('accessToken', 'jwt-da-sessao');
  localStorage.setItem('usuarioNome', 'Aluno da CPS');
  localStorage.setItem('usuarioEmail', 'aluno@aluno.cps.sp.gov.br');
  localStorage.setItem('usuarioRole', 'ADMIN');
}

describe('authInterceptor', () => {
  afterEach(() => {
    localStorage.clear();
  });

  it('deve anexar Authorization: Bearer quando há sessão', () => {
    semearSessao();
    const { http, backend } = criarContexto();

    http.get('/api/projetos/meus').subscribe();

    const req = backend.expectOne('/api/projetos/meus');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-da-sessao');
    req.flush([]);
  });

  it('não deve anexar header quando não há sessão', () => {
    const { http, backend } = criarContexto();

    http.get('/api/projetos/meus').subscribe();

    const req = backend.expectOne('/api/projetos/meus');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  it('não deve anexar header vazio quando o token é string vazia', () => {
    localStorage.setItem('accessToken', '');
    const { http, backend } = criarContexto();

    http.get('/api/projetos/meus').subscribe();

    const req = backend.expectOne('/api/projetos/meus');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  describe('401', () => {
    it('deve descartar a sessão quando o back-end recusa o token', () => {
      semearSessao();
      const { http, backend } = criarContexto();

      http.get('/api/projetos/meus').subscribe({ error: () => undefined });
      backend.expectOne('/api/projetos/meus').flush('nope', { status: 401, statusText: 'Unauthorized' });

      // Manter a sessão faria a UI continuar anunciando login cujas chamadas só falham.
      expect(localStorage.getItem('accessToken')).toBeNull();
      expect(localStorage.getItem('usuarioNome')).toBeNull();
      expect(localStorage.getItem('usuarioRole')).toBeNull();
    });

    it('deve propagar o erro original para quem chamou', () => {
      semearSessao();
      const { http, backend } = criarContexto();
      let recebido: unknown;

      http.get('/api/projetos/meus').subscribe({ error: (erro) => (recebido = erro) });
      backend.expectOne('/api/projetos/meus').flush('nope', { status: 401, statusText: 'Unauthorized' });

      // O tratamento de erro de quem chamou precisa continuar funcionando: engolir o erro
      // deixaria a tela em estado de carregamento eterno.
      expect((recebido as { status?: number } | undefined)?.status).toBe(401);
    });

    it('deve preservar as chaves que não são de sessão', () => {
      semearSessao();
      localStorage.setItem('msal.cache-id-token', 'cache do MSAL');
      const { http, backend } = criarContexto();

      http.get('/api/projetos/meus').subscribe({ error: () => undefined });
      backend.expectOne('/api/projetos/meus').flush('nope', { status: 401, statusText: 'Unauthorized' });

      // Apagar o cache do MSAL expulsaria a pessoa da Microsoft e custaria um MFA no próximo login.
      expect(localStorage.getItem('msal.cache-id-token')).toBe('cache do MSAL');
    });
  });

  describe('erros que não devem descartar a sessão', () => {
    it('não deve limpar a sessão em 403', () => {
      semearSessao();
      const { http, backend } = criarContexto();

      http.get('/api/projetos/meus').subscribe({ error: () => undefined });
      backend.expectOne('/api/projetos/meus').flush('proibido', { status: 403, statusText: 'Forbidden' });

      // 403 é "autenticado, mas não autorizado": um professor tentando aprovar projeto de outra
      // instituição. Expulsar a pessoa por um erro que ela mesma pode corrigir seria errado.
      expect(localStorage.getItem('accessToken')).toBe('jwt-da-sessao');
      expect(localStorage.getItem('usuarioRole')).toBe('ADMIN');
    });

    it('não deve limpar a sessão em 500', () => {
      semearSessao();
      const { http, backend } = criarContexto();

      http.get('/api/projetos/meus').subscribe({ error: () => undefined });
      backend.expectOne('/api/projetos/meus').flush('erro', { status: 500, statusText: 'Server Error' });

      // O back-end está fora do ar, não a sessão inválida.
      expect(localStorage.getItem('accessToken')).toBe('jwt-da-sessao');
    });

    it('não deve limpar a sessão em erro de rede', () => {
      semearSessao();
      const { http, backend } = criarContexto();

      http.get('/api/projetos/meus').subscribe({ error: () => undefined });
      backend.expectOne('/api/projetos/meus').error(new ProgressEvent('erro de rede'));

      // Um 401 não se resolve repetindo a requisição; uma queda de rede se resolve. Descartar a
      // sessão aqui transformaria uma falha passageira em perda de login.
      expect(localStorage.getItem('accessToken')).toBe('jwt-da-sessao');
    });
  });
});