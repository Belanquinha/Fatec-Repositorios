import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CatalogoPublico } from './catalogo-publico';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ProjetoService } from '../../core/services/projeto.service';
import { AuthService } from '../../core/auth/auth.service';

describe('CatalogoPublico', () => {
  let component: CatalogoPublico;
  let fixture: ComponentFixture<CatalogoPublico>;

  const mockProjetoService = {
    listarInstituicoes: () =>
      of([
        { id: '1', codigoUnidade: '001', nome: 'Fatec Ipiranga', ativo: true },
        { id: '2', codigoUnidade: '002', nome: 'Fatec Pompeia', ativo: true },
      ]),
    listarProjetosPublicos: () =>
      of([
        {
          id: '1',
          titulo: 'Projeto Teste',
          descricaoCurta: 'Descricao teste',
          conteudoEditorJs: '',
          linkRepositorio: '',
          imagemCapaUrl: 'capa_card.png',
          palavrasChave: ['Web', 'IA'],
          anoPublicado: 2026,
          estado: 'APROVADO' as const,
          emailProfessorResponsavel: 'prof@cps.sp.gov.br',
          instituicaoId: '1',
          instituicaoNome: 'Fatec Ipiranga',
          autorId: 'u1',
          autorNome: 'Aluno Teste',
          integrantes: [],
          criadoEm: '2026-01-01',
          atualizadoEm: '2026-01-01',
        },
      ]),
  };

  const mockAuthService = {
    obterUsuarioLogado: () => Promise.resolve(null),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CatalogoPublico],
      providers: [
        provideHttpClient(),
        provideRouter([]),
        { provide: ProjetoService, useValue: mockProjetoService },
        { provide: AuthService, useValue: mockAuthService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CatalogoPublico);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load projects on init', () => {
    expect(component.todosProjetos.length).toBe(1);
    expect(component.projetosFiltrados.length).toBe(1);
    expect(component.carregando).toBe(false);
  });

  it('should filter projects by search term', () => {
    component.termoBusca = 'Teste';
    component.aplicarFiltros();
    expect(component.projetosFiltrados.length).toBe(1);

    component.termoBusca = 'Inexistente';
    component.aplicarFiltros();
    expect(component.projetosFiltrados.length).toBe(0);
  });

  it('should reset filters when limparFiltros is called', () => {
    component.termoBusca = 'Inexistente';
    component.aplicarFiltros();
    expect(component.projetosFiltrados.length).toBe(0);

    component.limparFiltros();
    expect(component.termoBusca).toBe('');
    expect(component.projetosFiltrados.length).toBe(1);
  });
});
