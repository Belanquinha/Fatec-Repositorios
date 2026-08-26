import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CadastroInstituicao } from './cadastro-instituicao';

describe('CadastroInstituicao', () => {
  let component: CadastroInstituicao;
  let fixture: ComponentFixture<CadastroInstituicao>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CadastroInstituicao],
    }).compileComponents();

    fixture = TestBed.createComponent(CadastroInstituicao);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
