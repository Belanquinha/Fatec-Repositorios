import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UploadProjeto } from './upload-projeto';

import { provideHttpClient, withXhr } from '@angular/common/http';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { Router, provideRouter } from '@angular/router';
import { ProjetoService } from '../../core/services/projeto.service';

describe('UploadProjeto', () => {
  let component: UploadProjeto;
  let fixture: ComponentFixture<UploadProjeto>;

  const mockProjetoService = {
    listarInstituicoes: () => of([]),
    uploadImagem: () => of({ url: '/uploads/img.png', nomeArquivo: 'img.png' }),
    criarProjeto: () => of({ id: '123', titulo: 'Teste' }),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UploadProjeto],
      providers: [
        provideHttpClient(withXhr()),
        provideRouter([]),
        { provide: ProjetoService, useValue: mockProjetoService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UploadProjeto);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

/**
 * Item 1 (revisão 2026-10-27): Enter no formulário não navega nem envia — o
 * `ngSubmit` é engolido em todas as seções. O envio real só acontece pelo
 * botão da última seção.
 */
describe('UploadProjeto (Enter não navega nem envia, item 1)', () => {
  let component: UploadProjeto;
  let fixture: ComponentFixture<UploadProjeto>;

  const mockProjetoService = {
    listarInstituicoes: () => of([]),
    uploadImagem: () => of({ url: '/uploads/img.png', nomeArquivo: 'img.png' }),
    criarProjeto: vi.fn(() => of({ id: '123', titulo: 'Teste' })),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [UploadProjeto],
      providers: [
        provideHttpClient(withXhr()),
        provideRouter([]),
        { provide: ProjetoService, useValue: mockProjetoService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UploadProjeto);
    component = fixture.componentInstance;
    // Formulário válido na seção 4 ANTES do primeiro render: trocar a seção
    // depois que o template já foi pintado dispara NG0100 no zoneless.
    component.titulo = 'Projeto Teste';
    component.descricaoCurta = 'Descrição curta';
    component.instituicaoId = 'inst-1';
    // O `cursoId` é obrigatório na validação (o back-end rejeita sem ele), então
    // sem isto todo envio cairia em "Selecione o curso do projeto." antes de
    // chegar na regra que o teste exercita.
    component.cursoId = 'curso-1';
    component.professorEmail = 'prof@cps.sp.gov.br';
    component.imagemCapaUrl = '/uploads/capa.png';
    component.integrantes = [{ id: 1, nome: 'Aluno', linkedin: '' }];
    component.secaoAtual = 4;
    await fixture.whenStable();
  });

  function submeterFormulario() {
    const form: HTMLFormElement = fixture.nativeElement.querySelector('form');
    form.dispatchEvent(
      new Event('submit', { bubbles: true, cancelable: true }),
    );
  }

  it('submit do form (Enter) não avança de seção em nenhuma tela e não envia', async () => {
    for (const secao of [1, 2, 3, 4]) {
      component.secaoAtual = secao;

      submeterFormulario();
      await fixture.whenStable();

      expect(component.secaoAtual).toBe(secao);
    }

    expect(mockProjetoService.criarProjeto).not.toHaveBeenCalled();
  });

  it('clique em "Enviar para Avaliação Acadêmica" dispara o envio real', async () => {
    fixture.detectChanges();

    const botoes = Array.from(
      fixture.nativeElement.querySelectorAll('button'),
    ) as HTMLButtonElement[];
    const botaoEnviar = botoes.find((b) =>
      b.textContent?.includes('Enviar para Avaliação Acadêmica'),
    );
    expect(botaoEnviar).toBeTruthy();

    botaoEnviar!.click();
    await fixture.whenStable();

    expect(mockProjetoService.criarProjeto).toHaveBeenCalledTimes(1);
  });
});

/**
 * Itens 3 e 4 (decisão 2026-10-26): o campo do professor é somente e-mail.
 * `type=text` + `inputmode=email` evita o balão nativo do navegador e a
 * busca por nome (Story 3.2 original) foi removida do front-end.
 */
describe('UploadProjeto (campo do professor só e-mail, itens 3 e 4)', () => {
  let component: UploadProjeto;
  let fixture: ComponentFixture<UploadProjeto>;

  const mockProjetoService = {
    listarInstituicoes: () => of([]),
    buscarProfessores: vi.fn(() => of([])),
    obterProfessorPorId: vi.fn(() =>
      of({ id: 'p1', nome: 'Maria', email: 'maria@cps.sp.gov.br' }),
    ),
    uploadImagem: () => of({ url: '/uploads/img.png', nomeArquivo: 'img.png' }),
    criarProjeto: vi.fn(() => of({ id: '123', titulo: 'Teste' })),
  };

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [UploadProjeto],
      providers: [
        provideHttpClient(withXhr()),
        provideRouter([]),
        { provide: ProjetoService, useValue: mockProjetoService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UploadProjeto);
    component = fixture.componentInstance;
    component.secaoAtual = 2;
    await fixture.whenStable();
    fixture.detectChanges();
  });

  function campoProfessor(): HTMLInputElement {
    return fixture.nativeElement.querySelector('input[name="professorEmail"]');
  }

  it('campo é type=text com inputmode=email (sem balão nativo de validação)', () => {
    const input = campoProfessor();

    expect(input).toBeTruthy();
    expect(input.type).toBe('text');
    expect(input.getAttribute('inputmode')).toBe('email');
  });

  it('digitar um nome no campo não dispara busca por sugestão', () => {
    const input = campoProfessor();
    input.value = 'maria silva';
    input.dispatchEvent(new Event('input'));

    expect(mockProjetoService.buscarProfessores).not.toHaveBeenCalled();
  });

  it('deve barrar o envio com e-mail de professor inválido', async () => {
    component.titulo = 'Projeto Teste';
    component.descricaoCurta = 'Descrição curta';
    component.instituicaoId = 'inst-1';
    component.cursoId = 'curso-1';
    component.professorEmail = 'sem-arroba';
    component.imagemCapaUrl = '/uploads/capa.png';
    component.integrantes = [{ id: 1, nome: 'Aluno', linkedin: '' }];

    await component.enviarParaAvaliacao();

    expect(mockProjetoService.criarProjeto).not.toHaveBeenCalled();
    expect(component.erroMensagem()).toContain('e-mail válido');
    expect(component.secaoAtual).toBe(2);
  });

  it('aceita e-mail de professor fora do tenant (Convidado) sem chamar a busca', async () => {
    component.titulo = 'Projeto Teste';
    component.descricaoCurta = 'Descrição curta';
    component.instituicaoId = 'inst-1';
    component.cursoId = 'curso-1';
    component.professorEmail = 'prof.externo@gmail.com';
    component.imagemCapaUrl = '/uploads/capa.png';
    component.integrantes = [{ id: 1, nome: 'Aluno', linkedin: '' }];

    await component.enviarParaAvaliacao();

    expect(mockProjetoService.criarProjeto).toHaveBeenCalledTimes(1);
    expect(mockProjetoService.buscarProfessores).not.toHaveBeenCalled();
    expect(mockProjetoService.obterProfessorPorId).not.toHaveBeenCalled();
  });
});

/**
 * Item 5 (2026-10-26): a tela de sucesso oferece ir para a aba "Meus
 * projetos" do Perfil (`/perfil`), além de cadastrar outro projeto.
 */
describe('UploadProjeto (tela de sucesso, item 5)', () => {
  let component: UploadProjeto;
  let fixture: ComponentFixture<UploadProjeto>;
  let router: Router;

  const mockProjetoService = {
    listarInstituicoes: () => of([]),
    uploadImagem: () => of({ url: '/uploads/img.png', nomeArquivo: 'img.png' }),
    criarProjeto: () => of({ id: '123', titulo: 'Teste' }),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UploadProjeto],
      providers: [
        provideHttpClient(withXhr()),
        provideRouter([]),
        { provide: ProjetoService, useValue: mockProjetoService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UploadProjeto);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    component.enviadoComSucesso.set(true);
    await fixture.whenStable();
    fixture.detectChanges();
  });

  function botaoPorTexto(texto: string): HTMLButtonElement | undefined {
    const botoes = Array.from(
      fixture.nativeElement.querySelectorAll('button'),
    ) as HTMLButtonElement[];
    return botoes.find((b) => b.textContent?.includes(texto));
  }

  it('exibe "Ver meus projetos" e "Cadastrar Outro Projeto"', () => {
    expect(botaoPorTexto('Ver meus projetos')).toBeTruthy();
    expect(botaoPorTexto('Cadastrar Outro Projeto')).toBeTruthy();
  });

  it('"Ver meus projetos" navega para /perfil', () => {
    const navigateSpy = vi.spyOn(router, 'navigate');

    botaoPorTexto('Ver meus projetos')!.click();

    expect(navigateSpy).toHaveBeenCalledWith(['/perfil']);
  });

  it('"Cadastrar Outro Projeto" reinicia o formulário', () => {
    const resetSpy = vi.spyOn(component, 'resetarFormulario');

    botaoPorTexto('Cadastrar Outro Projeto')!.click();

    expect(resetSpy).toHaveBeenCalledTimes(1);
  });
});

/**
 * Limites de imagem (capa + Editor.js): o front recusa antes de enviar para
 * que um arquivo grande vire mensagem acionável, e não um 413 do nginx.
 */
describe('UploadProjeto (limites de imagem)', () => {
  let component: UploadProjeto;
  let fixture: ComponentFixture<UploadProjeto>;

  const mockProjetoService = {
    listarInstituicoes: () => of([]),
    uploadImagem: vi.fn(() =>
      of({ url: '/uploads/img.png', nomeArquivo: 'img.png' }),
    ),
    criarProjeto: vi.fn(() => of({ id: '123', titulo: 'Teste' })),
  };

  function eventoComArquivo(file: File) {
    return { target: { files: [file], value: 'capa.png' } } as unknown as Event;
  }

  beforeEach(async () => {
    vi.clearAllMocks();
    await TestBed.configureTestingModule({
      imports: [UploadProjeto],
      providers: [
        provideHttpClient(withXhr()),
        provideRouter([]),
        { provide: ProjetoService, useValue: mockProjetoService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UploadProjeto);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('deve recusar capa com tipo não permitido (ex.: GIF)', () => {
    const gif = new File([new ArrayBuffer(1024)], 'capa.gif', {
      type: 'image/gif',
    });

    component.onCapaSelecionada(eventoComArquivo(gif));

    expect(component.capaArquivo).toBeNull();
    expect(component.erroMensagem()).toContain('Tipo de arquivo não permitido');
  });

  it('deve recusar capa acima de 10MB antes de enviar', () => {
    const grande = new File([new ArrayBuffer(11 * 1024 * 1024)], 'capa.png', {
      type: 'image/png',
    });

    component.onCapaSelecionada(eventoComArquivo(grande));

    expect(component.capaArquivo).toBeNull();
    expect(component.erroMensagem()).toContain('10MB');
  });

  it('deve aceitar capa válida e limpar o erro', () => {
    const ok = new File([new ArrayBuffer(1024)], 'capa.png', {
      type: 'image/png',
    });

    component.onCapaSelecionada(eventoComArquivo(ok));

    expect(component.capaArquivo).toBe(ok);
    expect(component.erroMensagem()).toBeNull();
  });

  it('deve barrar o envio quando a capa burlou a seleção e é grande demais', async () => {
    component.titulo = 'Projeto Teste';
    component.descricaoCurta = 'Descrição curta';
    component.instituicaoId = 'inst-1';
    component.cursoId = 'curso-1';
    component.professorEmail = 'prof@cps.sp.gov.br';
    component.integrantes = [{ id: 1, nome: 'Aluno', linkedin: '' }];
    component.capaArquivo = new File(
      [new ArrayBuffer(11 * 1024 * 1024)],
      'capa.png',
      {
        type: 'image/png',
      },
    );

    await component.enviarParaAvaliacao();

    expect(mockProjetoService.criarProjeto).not.toHaveBeenCalled();
    expect(component.erroMensagem()).toContain('10MB');
  });

  it('deve barrar o envio com capa embutida em base64 (upload falhou)', async () => {
    component.titulo = 'Projeto Teste';
    component.descricaoCurta = 'Descrição curta';
    component.instituicaoId = 'inst-1';
    component.cursoId = 'curso-1';
    component.professorEmail = 'prof@cps.sp.gov.br';
    component.integrantes = [{ id: 1, nome: 'Aluno', linkedin: '' }];
    component.capaArquivo = null;
    component.imagemCapaUrl = 'data:image/png;base64,AAAA';

    await component.enviarParaAvaliacao();

    expect(mockProjetoService.criarProjeto).not.toHaveBeenCalled();
    expect(component.erroMensagem()).toContain('embutida');
  });
});
