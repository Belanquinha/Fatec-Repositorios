import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ProjetoService } from './projeto.service';
import { environment } from '../../../environments/environment';
import { authInterceptor } from '../auth/auth.interceptor';
import type { InstituicaoOption, ProjetoCreatePayload } from '../models/projeto.model';

/**
 * Contrato HTTP do front-end com a API.
 *
 * Story 5.1 (FR12) e Story 3.2 dependem destas chamadas: se a URL, o método ou o
 * formato do payload divergirem do back-end, a tela quebra em silêncio — ou pior,
 * o serviço pode mascarar a falha devolvendo conteúdo fictício, e o usuário vê
 * projetos que não existem como se fossem reais.
 *
 * Estes testes fixam a URL e o verbo de cada chamada, que é o contrato compartilhado
 * com a suíte E2E de API do back-end.
 */
describe('ProjetoService (contrato HTTP)', () => {
  let service: ProjetoService;
  let http: HttpTestingController;
  const base = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ProjetoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  describe('leituras públicas (Epic 5 / FR12)', () => {
    it('deve pedir os projetos públicos em GET /projetos/publicos', () => {
      let recebido: unknown;
      service.listarProjetosPublicos().subscribe((r) => (recebido = r));

      const req = http.expectOne(`${base}/projetos/publicos`);
      expect(req.request.method).toBe('GET');
      req.flush([]);

      expect(recebido).toEqual([]);
    });

    it('deve pedir o catálogo de instituições em GET /instituicoes', () => {
      service.listarInstituicoes().subscribe();

      const req = http.expectOne(`${base}/instituicoes`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });

    it('deve preservar todos os campos do catálogo vindos da API', () => {
      let recebido: InstituicaoOption[] = [];
      service.listarInstituicoes().subscribe((r) => (recebido = r));

      http.expectOne(`${base}/instituicoes`).flush([
        { codigoUnidade: '004', nome: 'Fatec Americana', cidade: 'Americana', ativo: true },
      ]);

      expect(recebido[0].codigoUnidade).toBe('004');
      expect(recebido[0].nome).toBe('Fatec Americana');
    });

    it('deve pedir a lista de professores para o autocomplete em GET /professores', () => {
      service.listarProfessores().subscribe();

      const req = http.expectOne(`${base}/professores`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });
  });

  /**
   * Regressão do bug relatado: os fallbacks ficavam dentro do serviço, com
   * `catchError(() => of(...))`. Isso não só mostrava conteúdo falso — também
   * deixava código morto em todas as telas, porque os handlers de erro dos
   * componentes nunca eram chamados e o painel "Não foi possível carregar os
   * projetos" jamais aparecia.
   */
  describe('erros não podem ser mascarados por dados fictícios', () => {
    it('deve propagar a falha do catálogo de projetos em vez de devolver projetos fictícios', () => {
      let erro: unknown;
      let emitido: unknown;
      service.listarProjetosPublicos().subscribe({
        next: (r) => (emitido = r),
        error: (e) => (erro = e),
      });

      http
        .expectOne(`${base}/projetos/publicos`)
        .flush('boom', { status: 500, statusText: 'Internal Server Error' });

      expect(erro).toBeTruthy();
      expect((erro as { status: number }).status).toBe(500);
      expect(emitido).toBeUndefined();
    });

    it('deve propagar a falha do catálogo de instituições em vez de devolver FATECs fictícias', () => {
      let erro: unknown;
      let emitido: unknown;
      service.listarInstituicoes().subscribe({
        next: (r) => (emitido = r),
        error: (e) => (erro = e),
      });

      http
        .expectOne(`${base}/instituicoes`)
        .error(new ProgressEvent('error'), { status: 0, statusText: 'Unknown Error' });

      expect(erro).toBeTruthy();
      expect(emitido).toBeUndefined();
    });

    it('deve devolver lista vazia (e não fictícia) quando o catálogo vem vazio', () => {
      let recebido: unknown;
      service.listarProjetosPublicos().subscribe((r) => (recebido = r));

      http.expectOne(`${base}/projetos/publicos`).flush([]);

      expect(recebido).toEqual([]);
    });
  });

  describe('submissão (Story 3.1 / 3.2)', () => {
    const payload: ProjetoCreatePayload = {
      titulo: 'Plataforma de Repositório',
      descricaoCurta: 'Sistema web',
      conteudoEditorJs: '{"blocks":[]}',
      linkRepositorio: 'https://github.com/fatec/repositorio',
      palavrasChave: ['Java'],
      anoPublicado: 2026,
      instituicaoId: 'inst-001',
      emailProfessorResponsavel: 'professor@cps.sp.gov.br',
      integrantes: [{ nome: 'Ana', linkLinkedin: 'https://linkedin.com/in/ana' }],
    };

    it('deve criar o projeto em POST /projetos', () => {
      service.criarProjeto(payload).subscribe();

      const req = http.expectOne(`${base}/projetos`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(payload);
      req.flush({ id: 'p1', estado: 'AGUARDANDO_APROVACAO' });
    });

    it('deve enviar emailProfessorResponsavel, sem o qual o back-end recusa (AC 3.2)', () => {
      // O front-end é responsável por sempre enviar o campo; o teste trava esse acordo.
      service.criarProjeto(payload).subscribe();

      const req = http.expectOne(`${base}/projetos`);
      expect(req.request.body.emailProfessorResponsavel).toBe('professor@cps.sp.gov.br');
      req.flush({});
    });

    it('deve enviar multipart com o campo "file" ao subir a capa', () => {
      const arquivo = new File(['conteudo'], 'capa.png', { type: 'image/png' });
      service.uploadImagem(arquivo).subscribe();

      const req = http.expectOne(`${base}/uploads`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body instanceof FormData).toBe(true);
      expect((req.request.body as FormData).get('file')).toBe(arquivo);
      req.flush({ url: '/uploads/capa.png', nomeArquivo: 'capa.png' });
    });

    it('deve enviar o corpo EditorJS serializado como texto', () => {
      service.criarProjeto(payload).subscribe();

      const req = http.expectOne(`${base}/projetos`);
      expect(typeof req.request.body.conteudoEditorJs).toBe('string');
      expect(() => JSON.parse(req.request.body.conteudoEditorJs as string)).not.toThrow();
      req.flush({});
    });
  });

  describe('sessão do aluno (Story 3.1 — Meus Projetos)', () => {
    it('deve listar os projetos do usuário em GET /projetos/meus', () => {
      service.obterMeusProjetos().subscribe();

      const req = http.expectOne(`${base}/projetos/meus`);
      expect(req.request.method).toBe('GET');
      req.flush([]);
    });

    it('deve abrir o detalhe em GET /projetos/{id}', () => {
      service.obterProjetoPorId('abc-123').subscribe();

      const req = http.expectOne(`${base}/projetos/abc-123`);
      expect(req.request.method).toBe('GET');
      req.flush({ id: 'abc-123' });
    });

    it('deve propagar o erro 401 do back-end em vez de mascará-lo', () => {
      let erro: unknown;
      service.obterMeusProjetos().subscribe({ error: (e) => (erro = e) });

      http.expectOne(`${base}/projetos/meus`).flush(
        { codigo: 401, mensagem: 'Não autorizado' },
        { status: 401, statusText: 'Unauthorized' }
      );

      // Um catálogo de fallback silencioso aqui esconderia sessão expirada.
      expect(erro).toBeTruthy();
      expect((erro as { status: number }).status).toBe(401);
    });
  });
});

describe('authInterceptor (NFR2 — o front-end não decide papel, só anexa o token)', () => {
  let http: HttpTestingController;

  function criarContexto() {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
    return TestBed.inject(ProjetoService);
  }

  afterEach(() => {
    localStorage.clear();
    http.verify();
  });

  it('deve anexar o header Authorization: Bearer quando há sessão', () => {
    localStorage.setItem('accessToken', 'jwt-da-sessao');
    const service = criarContexto();

    service.obterMeusProjetos().subscribe();

    const req = http.expectOne((r) => r.url.includes('/projetos/meus'));
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-da-sessao');
    req.flush([]);
  });

  it('deve enviar a requisição sem Authorization quando não há sessão', () => {
    const service = criarContexto();

    service.listarInstituicoes().subscribe();

    const req = http.expectOne((r) => r.url.includes('/instituicoes'));
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  it('não deve enviar header Authorization vazia quando o token é string vazia', () => {
    localStorage.setItem('accessToken', '');
    const service = criarContexto();

    service.listarInstituicoes().subscribe();

    const req = http.expectOne((r) => r.url.includes('/instituicoes'));
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });
});