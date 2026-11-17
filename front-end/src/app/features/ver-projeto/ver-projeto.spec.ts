import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { VerProjeto } from './ver-projeto';
import { ProjetoService } from '../../core/services/projeto.service';
import { AuthService } from '../../core/auth/auth.service';
import { ProjetoResponseModel } from '../../core/models/projeto.model';

describe('VerProjeto', () => {
  let fixture: ComponentFixture<VerProjeto>;
  let component: VerProjeto;
  let obterPorId: ReturnType<typeof vi.fn>;

  const projetoMock: ProjetoResponseModel = {
    id: 'p1',
    titulo: 'Plataforma de Repositório',
    descricaoCurta: 'Sistema web para organizar trabalhos.',
    conteudoEditorJs: JSON.stringify({
      blocks: [
        { type: 'header', data: { text: 'Visão geral', level: 2 } },
        { type: 'paragraph', data: { text: 'Texto do projeto' } },
      ],
    }),
    linkRepositorio: 'https://github.com/fatec/projeto',
    imagemCapaUrl: '/uploads/capa.png',
    palavrasChave: ['Angular', 'Java'],
    anoPublicado: 2026,
    estado: 'APROVADO',
    emailProfessorResponsavel: 'professor@cps.sp.gov.br',
    nomeProfessorResponsavel: 'Prof. Helena',
    instituicaoId: 'inst-1',
    instituicaoNome: 'Fatec Americana',
    autorId: 'u1',
    autorNome: 'Ana',
    integrantes: [{ nome: 'Ana', linkLinkedin: 'https://linkedin.com/in/ana' }],
    criadoEm: '2026-01-01T00:00:00Z',
    atualizadoEm: '2026-01-01T00:00:00Z',
  };

  async function configurar(idRota: string | null, resposta: unknown) {
    obterPorId = vi.fn();
    if (resposta instanceof Error) {
      obterPorId.mockReturnValue(throwError(() => resposta));
    } else {
      obterPorId.mockReturnValue(of((resposta ?? projetoMock) as ProjetoResponseModel));
    }

    await TestBed.configureTestingModule({
      imports: [VerProjeto],
      providers: [
        { provide: ProjetoService, useValue: { obterProjetoPorId: obterPorId } },
        { provide: AuthService, useValue: { obterUsuarioLogado: vi.fn().mockResolvedValue(null) } },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: () => idRota },
              queryParamMap: { get: () => null },
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VerProjeto);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('deve criar e carregar o projeto da rota', async () => {
    await configurar('p1', projetoMock);
    expect(component).toBeTruthy();
    expect(component.projeto()?.id).toBe('p1');
    expect(component.carregando()).toBe(false);
  });

  it('deve interpretar os blocos do Editor.js sem inventar conteúdo', async () => {
    await configurar('p1', projetoMock);
    expect(component.blocos().length).toBe(2);
    expect(component.blocos()[0].type).toBe('header');
    expect(component.conteudoTextoPuro()).toBe('');
  });

  it('não deve quebrar com conteudoEditorJs inválido', async () => {
    await configurar('p1', { ...projetoMock, conteudoEditorJs: '{invalido' });
    expect(component.blocos()).toEqual([]);
  });

  it('deve marcar não encontrado quando a rota não tem id', async () => {
    await configurar(null, null);
    expect(component.projetoNaoEncontrado()).toBe(true);
    expect(obterPorId).not.toHaveBeenCalled();
  });

  it('deve marcar não encontrado no 404 do backend', async () => {
    const erro404 = Object.assign(new Error('Não encontrado'), { status: 404 });
    await configurar('inexistente', erro404);
    expect(component.projetoNaoEncontrado()).toBe(true);
  });

  it('exibe o nome do professor responsável e nunca o e-mail (LGPD)', async () => {
    await configurar('p1', projetoMock);
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('Prof. Helena');
    expect(html.textContent).not.toContain('professor@cps.sp.gov.br');
  });
});

describe('VerProjeto (avaliação do professor responsável)', () => {
  const pendente: ProjetoResponseModel = {
    id: 'p1',
    titulo: 'Projeto Pendente',
    descricaoCurta: 'Aguardando decisão.',
    conteudoEditorJs: '',
    linkRepositorio: '',
    imagemCapaUrl: '',
    palavrasChave: [],
    anoPublicado: 2026,
    estado: 'AGUARDANDO_APROVACAO',
    emailProfessorResponsavel: 'professor@cps.sp.gov.br',
    nomeProfessorResponsavel: 'Prof. Helena',
    instituicaoId: 'inst-1',
    instituicaoNome: 'Fatec Americana',
    autorId: 'u1',
    autorNome: 'Ana',
    integrantes: [],
    criadoEm: '2026-01-01T00:00:00Z',
    atualizadoEm: '2026-01-01T00:00:00Z',
  };

  const professor = {
    nome: 'Professor Teste',
    email: 'professor.teste@cps.sp.gov.br',
    role: 'PROFESSOR',
  };

  const pendenteRejeitado: ProjetoResponseModel = {
    ...pendente,
    id: 'p-rejeitado',
    estado: 'REJEITADO',
    motivoRejeicao: 'Fora do escopo do curso',
  };

  let fixture: ComponentFixture<VerProjeto>;
  let component: VerProjeto;
  let aprovarProjeto: ReturnType<typeof vi.fn>;
  let rejeitarProjeto: ReturnType<typeof vi.fn>;

  async function configurar(opcoes: {
    avaliar?: string | null;
    usuario?: unknown;
    resposta?: unknown;
  }) {
    aprovarProjeto = vi.fn().mockReturnValue(of({ ...pendente, estado: 'APROVADO' }));
    rejeitarProjeto = vi.fn().mockReturnValue(of({ ...pendente, estado: 'REJEITADO' }));

    await TestBed.configureTestingModule({
      imports: [VerProjeto],
      providers: [
        {
          provide: ProjetoService,
          useValue: {
            obterProjetoPorId: vi.fn().mockReturnValue(of(opcoes.resposta ?? pendente)),
            aprovarProjeto,
            rejeitarProjeto,
          },
        },
        {
          provide: AuthService,
          useValue: { obterUsuarioLogado: vi.fn().mockResolvedValue(opcoes.usuario ?? null) },
        },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: { get: () => 'p1' },
              queryParamMap: { get: () => opcoes.avaliar ?? null },
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VerProjeto);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('não mostra o painel sem ?avaliar=1', async () => {
    await configurar({ avaliar: null, usuario: professor });
    expect(component.podeAvaliar()).toBe(false);
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Aprovar projeto');
  });

  it('não mostra o painel para quem não é professor', async () => {
    await configurar({ avaliar: '1', usuario: { nome: 'Aluno', email: 'a@x', role: 'ALUNO' } });
    expect(component.podeAvaliar()).toBe(false);
  });

  it('professor logado é tratado como dono da visão de avaliação', async () => {
    await configurar({ avaliar: null, usuario: professor });
    expect(component.visaoAvaliacao()).toBe(true);
  });

  it('visitante anônimo (ou sessão vencida) não entra em visão de avaliação', async () => {
    await configurar({ avaliar: null, usuario: null });
    expect(component.visaoAvaliacao()).toBe(false);
  });

  it('aluno logado nunca entra em visão de avaliação, mesmo com ?avaliar=1', async () => {
    await configurar({ avaliar: '1', usuario: { nome: 'Aluno', email: 'a@x', role: 'ALUNO' } });
    expect(component.visaoAvaliacao()).toBe(false);
  });

  it('ADMIN não entra em visão de avaliação', async () => {
    await configurar({ avaliar: null, usuario: { nome: 'Admin', email: 'admin@cps.sp.gov.br', role: 'ADMIN' } });
    expect(component.visaoAvaliacao()).toBe(false);
  });

  it('professor vê o motivo da rejeição de projeto rejeitado', async () => {
    await configurar({ avaliar: null, usuario: professor, resposta: pendenteRejeitado });
    expect(component.projeto()).toBeTruthy();
    expect(component.mostraMotivoRejeicao()).toBe(true);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Fora do escopo do curso');
  });

  it('fora da visão de avaliação o motivo da rejeição some ( LGPD / retorno só ao avaliador )', async () => {
    await configurar({ avaliar: null, usuario: null, resposta: pendenteRejeitado });
    expect(component.mostraMotivoRejeicao()).toBe(false);
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Fora do escopo do curso');
  });

  it('professor com ?avaliar=1 e projeto pendente vê o painel de decisão', async () => {
    await configurar({ avaliar: '1', usuario: professor });
    expect(component.podeAvaliar()).toBe(true);
    const texto = (fixture.nativeElement as HTMLElement).textContent as string;
    expect(texto).toContain('Avaliação do projeto');
    expect(texto).toContain('Aprovar projeto');
    expect(texto).toContain('Rejeitar');
  });

  it('aprovar chama o serviço e registra o sucesso', async () => {
    await configurar({ avaliar: '1', usuario: professor });
    component.aprovar();
    await fixture.whenStable();

    expect(aprovarProjeto).toHaveBeenCalledWith('p1');
    expect(component.sucessoAvaliacao()).toContain('aprovado');
    expect(component.podeAvaliar()).toBe(false);
  });

  it('rejeitar exige motivo antes de chamar o serviço', async () => {
    await configurar({ avaliar: '1', usuario: professor });
    component.abrirRejeicao();
    component.confirmarRejeicao();

    expect(component.erroAvaliacao()).toContain('motivo');
    expect(rejeitarProjeto).not.toHaveBeenCalled();
  });

  it('rejeitar com motivo chama o serviço e registra o sucesso', async () => {
    await configurar({ avaliar: '1', usuario: professor });
    component.abrirRejeicao();
    component.motivoRejeicaoCampo.set('Falta referencial teórico');
    component.confirmarRejeicao();
    await fixture.whenStable();

    expect(rejeitarProjeto).toHaveBeenCalledWith('p1', 'Falta referencial teórico');
    expect(component.sucessoAvaliacao()).toContain('rejeitado');
  });
});
