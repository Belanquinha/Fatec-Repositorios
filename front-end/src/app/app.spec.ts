import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './core/auth/auth.service';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            inicializar: () => Promise.resolve(),
            quandoPronto: () => Promise.resolve(),
            obterUsuarioLogado: () => Promise.resolve(null),
            loginMicrosoft: () => Promise.resolve(),
            logout: () => {},
            isAdmin: () => false,
            // O botão de login chama estes ao montar; o duplo precisa cobri-los ou o `ngOnInit`
            // estoura antes de qualquer asserção rodar.
            contasDev: () => Promise.resolve([]),
            loginDev: () => Promise.resolve(),
            limparSessaoLocal: () => {},
          },
        },
      ],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render header and footer', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-header')).toBeTruthy();
    expect(compiled.querySelector('app-footer')).toBeTruthy();
  });
});