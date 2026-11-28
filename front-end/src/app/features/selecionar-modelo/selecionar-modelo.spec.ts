import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router, ActivatedRoute } from '@angular/router';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { SelecionarModelo } from './selecionar-modelo';

describe('SelecionarModelo', () => {
  let fixture: ComponentFixture<SelecionarModelo>;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SelecionarModelo],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: {
                get: (k: string) => (k === 'instituicaoId' ? 'inst-1' : null),
              },
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SelecionarModelo);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('navega para /projeto-forms?modelo=GITHUB preservando instituicaoId', () => {
    const spy = vi.spyOn(router, 'navigate');
    const btn = fixture.nativeElement.querySelector('[data-testid="modelo-github"]') as HTMLButtonElement;
    btn.click();

    expect(spy).toHaveBeenCalledWith(['/projeto-forms'], {
      queryParams: { modelo: 'GITHUB', instituicaoId: 'inst-1' },
    });
  });

  it('navega para /projeto-forms?modelo=JOGO preservando instituicaoId', () => {
    const spy = vi.spyOn(router, 'navigate');
    const btn = fixture.nativeElement.querySelector('[data-testid="modelo-jogo"]') as HTMLButtonElement;
    btn.click();

    expect(spy).toHaveBeenCalledWith(['/projeto-forms'], {
      queryParams: { modelo: 'JOGO', instituicaoId: 'inst-1' },
    });
  });
});
