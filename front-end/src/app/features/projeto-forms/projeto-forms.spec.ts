import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProjetoForms } from './projeto-forms';

describe('ProjetoForms', () => {
  let component: ProjetoForms;
  let fixture: ComponentFixture<ProjetoForms>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProjetoForms],
    }).compileComponents();

    fixture = TestBed.createComponent(ProjetoForms);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
