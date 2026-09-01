import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VerProjeto } from './ver-projeto';

describe('VerProjeto', () => {
  let component: VerProjeto;
  let fixture: ComponentFixture<VerProjeto>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VerProjeto],
    }).compileComponents();

    fixture = TestBed.createComponent(VerProjeto);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
