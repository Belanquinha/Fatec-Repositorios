import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LoginInstituicao } from './login-instituicao';

describe('LoginInstituicao', () => {
  let component: LoginInstituicao;
  let fixture: ComponentFixture<LoginInstituicao>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginInstituicao],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginInstituicao);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
