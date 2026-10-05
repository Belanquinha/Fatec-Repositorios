import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Perfil } from './perfil';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../core/auth/auth.service';
import { ProjetoService } from '../../core/services/projeto.service';

describe('Perfil', () => {
  let component: Perfil;
  let fixture: ComponentFixture<Perfil>;

  const aluno = {
    nome: 'Aluno Teste',
    email: 'aluno.teste@aluno.cps.sp.gov.br',
    role: 'ALUNO',
  };

  async function configurar(authMock: unknown, projetosMock: unknown) {
    await TestBed.configureTestingModule({
      imports: [Perfil],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authMock },
        { provide: ProjetoService, useValue: projetosMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Perfil);
    component = fixture.componentInstance;
    await fixture.whenStable();
    fixture.detectChanges();
  }

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('exibe identidade e badge de papel do aluno', async () => {
    await configurar(
      { obterUsuarioLogado: vi.fn().mockResolvedValue(aluno) },
      { obterMeusProjetos: vi.fn().mockReturnValue(of([])) }
    );

    expect(component.usuario()).toEqual(aluno);
    expect(component.rotuloPapel()).toBe('Aluno');
    expect(component.isAluno()).toBe(true);

    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Aluno Teste');
    expect(texto).toContain('aluno.teste@aluno.cps.sp.gov.br');
  });

  it('mostra o contador de projetos do aluno', async () => {
    await configurar(
      { obterUsuarioLogado: vi.fn().mockResolvedValue(aluno) },
      { obterMeusProjetos: vi.fn().mockReturnValue(of([{ id: '1' }, { id: '2' }])) }
    );

    expect(component.totalProjetos()).toBe(2);
    expect(fixture.nativeElement.textContent as string).toContain('2');
  });

  it('mantém a identidade quando o contexto falha (aviso laranja)', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    await configurar(
      { obterUsuarioLogado: vi.fn().mockResolvedValue(aluno) },
      { obterMeusProjetos: vi.fn().mockReturnValue(throwError(() => new Error('backend fora do ar'))) }
    );

    expect(component.usuario()).toEqual(aluno);
    expect(component.erroContexto()).toContain('Não foi possível carregar');
  });

  it('professor vê o estado vazio de vínculos sem chamar /projetos/meus', async () => {
    const obterMeusProjetos = vi.fn();
    await configurar(
      {
        obterUsuarioLogado: vi.fn().mockResolvedValue({
          nome: 'Professor Teste',
          email: 'professor.teste@cps.sp.gov.br',
          role: 'PROFESSOR',
        }),
      },
      { obterMeusProjetos }
    );

    expect(component.isProfessor()).toBe(true);
    expect(component.rotuloPapel()).toBe('Professor');
    expect(obterMeusProjetos).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent as string).toContain('Você ainda não vinculou instituições.');
  });
});
