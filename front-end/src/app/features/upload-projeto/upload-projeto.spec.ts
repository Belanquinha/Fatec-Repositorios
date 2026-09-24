import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UploadProjeto } from './upload-projeto';

import { provideHttpClient } from '@angular/common/http';
import { of } from 'rxjs';
import { ProjetoService } from '../../core/services/projeto.service';

import { provideRouter } from '@angular/router';

describe('UploadProjeto', () => {
  let component: UploadProjeto;
  let fixture: ComponentFixture<UploadProjeto>;

  const mockProjetoService = {
    listarInstituicoes: () => of([]),
    listarProfessores: () => of([]),
    uploadImagem: () => of({ url: '/uploads/img.png', nomeArquivo: 'img.png' }),
    criarProjeto: () => of({ id: '123', titulo: 'Teste' }),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UploadProjeto],
      providers: [
        provideHttpClient(),
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
