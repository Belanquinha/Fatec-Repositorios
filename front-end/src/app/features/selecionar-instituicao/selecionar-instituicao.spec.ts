import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SelecionarInstituicao } from './selecionar-instituicao';
import { ProjetoService } from '../../core/services/projeto.service';
import { of } from 'rxjs';
import { provideRouter } from '@angular/router';

describe('SelecionarInstituicao', () => {
  let component: SelecionarInstituicao;
  let fixture: ComponentFixture<SelecionarInstituicao>;

  const instituicoesMock = [
    {
      id: 'inst-1',
      codigoUnidade: '291',
      nome: 'Fatec Adamantina',
      cidade: 'Adamantina',
      estado: 'SP',
      regiaoAdministrativa: 'Presidente Prudente',
      ativo: true,
    },
    {
      id: 'inst-2',
      codigoUnidade: '004',
      nome: 'Fatec Americana – Ministro Ralph Biasi',
      cidade: 'Americana',
      estado: 'SP',
      regiaoAdministrativa: 'Campinas',
      ativo: true,
    },
  ];

  const mockProjetoService = {
    listarInstituicoes: () => of(instituicoesMock),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SelecionarInstituicao],
      providers: [
        provideRouter([]),
        { provide: ProjetoService, useValue: mockProjetoService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SelecionarInstituicao);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve criar o componente', () => {
    expect(component).toBeTruthy();
  });

  it('deve carregar e listar as instituições', () => {
    expect(component.instituicoes.length).toBe(2);
    expect(component.instituicoesFiltradas.length).toBe(2);
  });

  it('deve filtrar instituições por termo de busca', () => {
    component.termoBusca = 'Americana';
    component.aplicarFiltros();
    expect(component.instituicoesFiltradas.length).toBe(1);
    expect(component.instituicoesFiltradas[0].codigoUnidade).toBe('004');
  });

  it('deve filtrar instituições por código', () => {
    component.termoBusca = '291';
    component.aplicarFiltros();
    expect(component.instituicoesFiltradas.length).toBe(1);
    expect(component.instituicoesFiltradas[0].nome).toBe('Fatec Adamantina');
  });

  it('deve filtrar instituições por região administrativa', () => {
    component.selecionarRegiao('Campinas');
    expect(component.instituicoesFiltradas.length).toBe(1);
    expect(component.instituicoesFiltradas[0].nome).toContain('Americana');
  });

  it('deve separar o nome e o patrono corretamente', () => {
    const res = component.separarNomeEPatrono('Fatec Americana – Ministro Ralph Biasi');
    expect(res.principal).toBe('Fatec Americana');
    expect(res.patrono).toBe('Ministro Ralph Biasi');
  });
});
