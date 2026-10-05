import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CardProjeto } from './card-projeto';
import { environment } from '../../../../environments/environment';
import { ProjetoResponseModel } from '../../../core/models/projeto.model';

describe('CardProjeto', () => {
  let component: CardProjeto;
  let fixture: ComponentFixture<CardProjeto>;

  const projetoCompleto: ProjetoResponseModel = {
    id: 'p1',
    titulo: 'Plataforma de Repositório',
    descricaoCurta: 'Sistema web para organizar trabalhos de conclusão.',
    conteudoEditorJs: '',
    linkRepositorio: '',
    imagemCapaUrl: '/uploads/capa.png',
    palavrasChave: ['Java', 'Web', 'Angular', 'IA'],
    anoPublicado: 2026,
    estado: 'AGUARDANDO_APROVACAO',
    emailProfessorResponsavel: 'professor@cps.sp.gov.br',
    instituicaoId: 'inst-1',
    instituicaoNome: 'Fatec Americana',
    autorId: 'u1',
    autorNome: 'Ana',
    integrantes: [],
    criadoEm: '2026-01-01T00:00:00Z',
    atualizadoEm: '2026-01-01T00:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CardProjeto],
    }).compileComponents();

    fixture = TestBed.createComponent(CardProjeto);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  /**
   * `setInput` em vez de atribuir o campo: é a API que marca o componente como
   * sujo corretamente. Atribuir `component.projeto` direto deixa o `@if` do
   * template ser reavaliado no mesmo ciclo e o dev mode acusa NG0100.
   */
  function definirProjeto(projeto: Partial<ProjetoResponseModel>): void {
    fixture.componentRef.setInput('projeto', projeto);
    fixture.detectChanges();
  }

  describe('projeto com todos os campos', () => {
    beforeEach(() => {
      definirProjeto(projetoCompleto);
    });

    it('deve exibir os dados reais do projeto', () => {
      expect(component.capaUrl).toBe(`${environment.apiUrl}/uploads/capa.png`);
      expect(component.titulo).toBe('Plataforma de Repositório');
      expect(component.instituicao).toBe('Fatec Americana');
      expect(component.ano).toBe(2026);
    });

    it('deve mostrar no máximo 3 tags', () => {
      expect(component.tags).toEqual(['Java', 'Web', 'Angular']);
    });
  });

  describe('projeto sem os campos preenchidos', () => {
    beforeEach(() => {
      definirProjeto({
        ...projetoCompleto,
        titulo: '',
        descricaoCurta: '',
        imagemCapaUrl: '',
        instituicaoNome: '',
        palavrasChave: [],
        anoPublicado: undefined as unknown as number,
        estado: undefined as unknown as ProjetoResponseModel['estado'],
      });
    });

    it('não deve inventar título, descrição, instituição ou ano', () => {
      expect(component.titulo).toBe('');
      expect(component.descricao).toBe('');
      expect(component.instituicao).toBe('');
      expect(component.ano).toBeNull();
      expect(component.tags).toEqual([]);
    });

    it('não deve exibir selo de estado algum', () => {
      const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(html).not.toContain('Aprovado');
      expect(html).not.toContain('Em avaliação');
      expect(html).not.toContain('check_circle');
      expect(
        (fixture.nativeElement as HTMLElement).querySelector('.badge-status')
      ).toBeNull();
    });

    it('não deve renderizar a imagem quando não há capa', () => {
      expect(component.capaUrl).toBe('');

      const img = (fixture.nativeElement as HTMLElement).querySelector('img.imagem-capa');
      expect(img).toBeNull();
      const html = (fixture.nativeElement as HTMLElement).textContent ?? '';
      expect(html).toContain('image_not_supported');
    });
  });
});
