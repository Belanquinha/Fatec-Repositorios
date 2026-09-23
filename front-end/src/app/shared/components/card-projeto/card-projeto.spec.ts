import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CardProjeto } from './card-projeto';

describe('CardProjeto', () => {
  let component: CardProjeto;
  let fixture: ComponentFixture<CardProjeto>;

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
});
