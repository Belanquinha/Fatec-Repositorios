import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { VerProjeto } from './ver-projeto';
import { ProjetoService } from '../../core/services/projeto.service';
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
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => idRota } } },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VerProjeto);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
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
});
