import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { vi } from 'vitest';

import { AuthService } from '../../core/auth/auth.service';
import { MicrosoftLoginButton } from '../../shared/components/microsoft-login-button/microsoft-login-button';
import { Autenticacao } from './autenticacao';

describe('Autenticacao (/entrar)', () => {
  let component: Autenticacao;
  let fixture: ComponentFixture<Autenticacao>;
  let router: Router;
  let authServiceDouble: {
    loginComEmailSenha: ReturnType<typeof vi.fn>;
    iniciarCadastro: ReturnType<typeof vi.fn>;
    reenviarCodigoCadastro: ReturnType<typeof vi.fn>;
    confirmarCadastro: ReturnType<typeof vi.fn>;
    solicitarRecuperacaoSenha: ReturnType<typeof vi.fn>;
    redefinirSenha: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    authServiceDouble = {
      loginComEmailSenha: vi.fn().mockResolvedValue({ accessToken: 'jwt' }),
      iniciarCadastro: vi.fn().mockResolvedValue({
        mensagem: 'Enviamos um código de 6 dígitos para o seu e-mail.',
        email: 'ana@gmail.com',
        expiraEmSegundos: 900,
        reenvioEmSegundos: 60,
      }),
      reenviarCodigoCadastro: vi.fn().mockResolvedValue({
        mensagem: 'Novo código enviado.',
        email: 'ana@gmail.com',
        expiraEmSegundos: 900,
        reenvioEmSegundos: 60,
      }),
      confirmarCadastro: vi.fn().mockResolvedValue({ accessToken: 'jwt' }),
      solicitarRecuperacaoSenha: vi.fn().mockResolvedValue({
        mensagem: 'Código de recuperação enviado.',
        email: 'docente@cps.sp.gov.br',
        expiraEmSegundos: 900,
        reenvioEmSegundos: 60,
      }),
      redefinirSenha: vi.fn().mockResolvedValue({
        mensagem: 'Senha redefinida com sucesso!',
      }),
    };

    await TestBed.configureTestingModule({
      imports: [Autenticacao],
      providers: [provideRouter([]), { provide: AuthService, useValue: authServiceDouble }],
    })
      .overrideComponent(Autenticacao, {
        remove: { imports: [MicrosoftLoginButton] },
        add: { schemas: [CUSTOM_ELEMENTS_SCHEMA] },
      })
      .compileComponents();

    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockResolvedValue(true);

    fixture = TestBed.createComponent(Autenticacao);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('inicia na aba de login e exibe o botão Microsoft institucional', () => {
    expect(component.abaAtiva()).toBe('login');
    expect(fixture.nativeElement.querySelector('app-microsoft-login-button')).not.toBeNull();
  });

  it('exibe prévia de PROFESSOR para @cps.sp.gov.br e ALUNO para demais domínios no cadastro', () => {
    component.selecionarAba('cadastro');
    fixture.detectChanges();

    component.emailCadastro.set('docente@cps.sp.gov.br');
    fixture.detectChanges();
    expect(component.previaPapelCadastro()).toBe('PROFESSOR');
    expect(fixture.nativeElement.querySelector('[data-role-preview="PROFESSOR"]')).not.toBeNull();

    component.emailCadastro.set('estudante@aluno.cps.sp.gov.br');
    fixture.detectChanges();
    expect(component.previaPapelCadastro()).toBe('ALUNO');
    expect(fixture.nativeElement.querySelector('[data-role-preview="ALUNO"]')).not.toBeNull();

    component.emailCadastro.set('pessoa@gmail.com');
    fixture.detectChanges();
    expect(component.previaPapelCadastro()).toBe('ALUNO');
  });

  it('executa fluxo de cadastro em 2 etapas (enviar código -> confirmar 6 dígitos -> navegar)', async () => {
    component.selecionarAba('cadastro');
    component.nomeCadastro.set('Ana Souza');
    component.emailCadastro.set('ana@gmail.com');
    component.senhaCadastro.set('senhaSegura123');
    component.confirmarSenhaCadastro.set('senhaSegura123');

    await component.submeterInicioCadastro();
    fixture.detectChanges();

    expect(authServiceDouble.iniciarCadastro).toHaveBeenCalledWith(
      'Ana Souza',
      'ana@gmail.com',
      'senhaSegura123',
    );
    expect(component.etapaCadastro()).toBe('codigo');

    component.digitosCodigo.set(['4', '8', '2', '9', '1', '0']);
    await component.submeterConfirmacaoCadastro();

    expect(authServiceDouble.confirmarCadastro).toHaveBeenCalledWith('ana@gmail.com', '482910');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/');
  });

  it('executa login por e-mail e senha e navega para returnUrl', async () => {
    component.returnUrl.set('/projeto-forms');
    component.emailLogin.set('ana@gmail.com');
    component.senhaLogin.set('senhaSegura123');

    await component.submeterLogin();

    expect(authServiceDouble.loginComEmailSenha).toHaveBeenCalledWith(
      'ana@gmail.com',
      'senhaSegura123',
    );
    expect(router.navigateByUrl).toHaveBeenCalledWith('/projeto-forms');
  });

  it('executa fluxo completo de recuperação de senha e retorna ao login com sucesso', async () => {
    component.emailLogin.set('docente@cps.sp.gov.br');
    component.abrirRecuperacaoSenha();
    fixture.detectChanges();

    expect(component.abaAtiva()).toBe('recuperar');
    expect(component.emailRecuperacao()).toBe('docente@cps.sp.gov.br');

    await component.submeterSolicitacaoRecuperacao();
    fixture.detectChanges();

    expect(authServiceDouble.solicitarRecuperacaoSenha).toHaveBeenCalledWith(
      'docente@cps.sp.gov.br',
    );
    expect(component.etapaRecuperacao()).toBe('redefinir');

    component.digitosCodigo.set(['1', '2', '3', '4', '5', '6']);
    component.novaSenhaRecuperacao.set('novaSenhaForte99');
    component.confirmarNovaSenha.set('novaSenhaForte99');

    await component.submeterRedefinicaoSenha();
    fixture.detectChanges();

    expect(authServiceDouble.redefinirSenha).toHaveBeenCalledWith(
      'docente@cps.sp.gov.br',
      '123456',
      'novaSenhaForte99',
    );
    expect(component.abaAtiva()).toBe('login');
    expect(component.mensagemSucesso()).toContain('Senha redefinida');
  });
});
