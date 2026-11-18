import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  QueryList,
  ViewChildren,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService, ErroAutenticacao } from '../../core/auth/auth.service';
import { MicrosoftLoginButton } from '../../shared/components/microsoft-login-button/microsoft-login-button';

export type AbaAutenticacao = 'login' | 'cadastro' | 'recuperar';
export type EtapaCadastro = 'dados' | 'codigo';
export type EtapaRecuperacao = 'solicitar' | 'redefinir';

@Component({
  selector: 'app-autenticacao',
  imports: [CommonModule, FormsModule, RouterModule, MicrosoftLoginButton],
  templateUrl: './autenticacao.html',
  styleUrl: './autenticacao.css',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class Autenticacao implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  @ViewChildren('digitoInput') digitosInputs!: QueryList<ElementRef<HTMLInputElement>>;

  abaAtiva = signal<AbaAutenticacao>('login');
  etapaCadastro = signal<EtapaCadastro>('dados');
  etapaRecuperacao = signal<EtapaRecuperacao>('solicitar');

  carregando = signal(false);
  mensagemErro = signal<string | null>(null);
  mensagemSucesso = signal<string | null>(null);
  cooldownSegundos = signal(0);

  returnUrl = signal<string>('/');

  // Campos de Login
  emailLogin = signal('');
  senhaLogin = signal('');
  mostrarSenhaLogin = signal(false);

  // Campos de Cadastro
  nomeCadastro = signal('');
  emailCadastro = signal('');
  senhaCadastro = signal('');
  confirmarSenhaCadastro = signal('');
  mostrarSenhaCadastro = signal(false);

  // Campos de Recuperação de Senha
  emailRecuperacao = signal('');
  novaSenhaRecuperacao = signal('');
  confirmarNovaSenha = signal('');
  mostrarNovaSenha = signal(false);

  // Código de 6 dígitos compartilhado pela etapa ativa (cadastro ou recuperação)
  digitosCodigo = signal<string[]>(['', '', '', '', '', '']);

  codigoCompleto = computed(() => this.digitosCodigo().join(''));

  /**
   * Prévia puramente informativa do papel com base no domínio digitado.
   * A decisão real de autorização acontece exclusivamente no back-end.
   */
  previaPapelCadastro = computed<'PROFESSOR' | 'ALUNO' | null>(() => {
    const email = this.emailCadastro().trim().toLowerCase();
    const partes = email.split('@');
    if (partes.length !== 2 || !partes[0] || !partes[1].includes('.')) {
      return null;
    }
    if (email.endsWith('@cps.sp.gov.br')) {
      return 'PROFESSOR';
    }
    return 'ALUNO';
  });

  private intervaloCooldown: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    const destino = this.route.snapshot.queryParamMap.get('returnUrl');
    if (destino && destino.startsWith('/') && !destino.startsWith('//')) {
      this.returnUrl.set(destino);
    }

    const modo = this.route.snapshot.queryParamMap.get('modo');
    if (modo === 'cadastro') {
      this.selecionarAba('cadastro');
    } else if (modo === 'recuperar') {
      this.abrirRecuperacaoSenha();
    }
  }

  ngOnDestroy(): void {
    this.pararCooldown();
  }

  selecionarAba(aba: 'login' | 'cadastro'): void {
    this.abaAtiva.set(aba);
    this.limparMensagens();
  }

  abrirRecuperacaoSenha(): void {
    if (this.emailLogin().trim() && !this.emailRecuperacao().trim()) {
      this.emailRecuperacao.set(this.emailLogin().trim());
    }
    this.abaAtiva.set('recuperar');
    this.etapaRecuperacao.set('solicitar');
    this.zerarDigitos();
    this.limparMensagens();
  }

  voltarParaLogin(): void {
    this.abaAtiva.set('login');
    this.limparMensagens();
  }

  voltarParaDadosCadastro(): void {
    this.etapaCadastro.set('dados');
    this.zerarDigitos();
    this.limparMensagens();
  }

  async submeterLogin(): Promise<void> {
    this.limparMensagens();
    const email = this.emailLogin().trim();
    const senha = this.senhaLogin();

    if (!email || !this.emailPareceValido(email)) {
      this.mensagemErro.set('Informe um endereço de e-mail válido.');
      return;
    }
    if (!senha) {
      this.mensagemErro.set('Informe sua senha.');
      return;
    }

    this.carregando.set(true);
    try {
      await this.authService.loginComEmailSenha(email, senha);
      await this.router.navigateByUrl(this.returnUrl());
    } catch (erro) {
      this.tratarErro(erro, 'Não foi possível realizar o login.');
    } finally {
      this.carregando.set(false);
    }
  }

  async submeterInicioCadastro(): Promise<void> {
    this.limparMensagens();
    const nome = this.nomeCadastro().trim();
    const email = this.emailCadastro().trim();
    const senha = this.senhaCadastro();
    const confirmacao = this.confirmarSenhaCadastro();

    if (nome.length < 2) {
      this.mensagemErro.set('Informe seu nome completo (mínimo de 2 caracteres).');
      return;
    }
    if (!this.emailPareceValido(email)) {
      this.mensagemErro.set('Informe um endereço de e-mail válido.');
      return;
    }
    if (senha.length < 8) {
      this.mensagemErro.set('A senha deve ter pelo menos 8 caracteres.');
      return;
    }
    if (senha !== confirmacao) {
      this.mensagemErro.set('A confirmação de senha não confere.');
      return;
    }

    this.carregando.set(true);
    try {
      const resposta = await this.authService.iniciarCadastro(nome, email, senha);
      this.emailCadastro.set(resposta.email || email.toLowerCase());
      this.etapaCadastro.set('codigo');
      this.zerarDigitos();
      this.mensagemSucesso.set(resposta.mensagem);
      this.iniciarCooldown(resposta.reenvioEmSegundos ?? 60);
      this.focarPrimeiroDigito();
    } catch (erro) {
      this.tratarErro(erro, 'Não foi possível enviar o código de verificação.');
    } finally {
      this.carregando.set(false);
    }
  }

  async submeterConfirmacaoCadastro(): Promise<void> {
    this.limparMensagens();
    const codigo = this.codigoCompleto();
    if (!/^\d{6}$/.test(codigo)) {
      this.mensagemErro.set('Digite os 6 números do código enviado ao seu e-mail.');
      return;
    }

    this.carregando.set(true);
    try {
      await this.authService.confirmarCadastro(this.emailCadastro().trim(), codigo);
      await this.router.navigateByUrl(this.returnUrl());
    } catch (erro) {
      this.tratarErro(erro, 'Não foi possível validar o código.');
    } finally {
      this.carregando.set(false);
    }
  }

  async reenviarCodigoCadastro(): Promise<void> {
    if (this.cooldownSegundos() > 0 || this.carregando()) {
      return;
    }
    this.limparMensagens();
    this.carregando.set(true);
    try {
      const resposta = await this.authService.reenviarCodigoCadastro(this.emailCadastro().trim());
      this.zerarDigitos();
      this.mensagemSucesso.set(resposta.mensagem);
      this.iniciarCooldown(resposta.reenvioEmSegundos ?? 60);
      this.focarPrimeiroDigito();
    } catch (erro) {
      this.tratarErro(erro, 'Não foi possível reenviar o código.');
    } finally {
      this.carregando.set(false);
    }
  }

  async submeterSolicitacaoRecuperacao(): Promise<void> {
    this.limparMensagens();
    const email = this.emailRecuperacao().trim();

    if (!this.emailPareceValido(email)) {
      this.mensagemErro.set('Informe um endereço de e-mail válido.');
      return;
    }

    this.carregando.set(true);
    try {
      const resposta = await this.authService.solicitarRecuperacaoSenha(email);
      this.emailRecuperacao.set(resposta.email || email.toLowerCase());
      this.etapaRecuperacao.set('redefinir');
      this.zerarDigitos();
      this.novaSenhaRecuperacao.set('');
      this.confirmarNovaSenha.set('');
      this.mensagemSucesso.set(resposta.mensagem);
      this.iniciarCooldown(resposta.reenvioEmSegundos ?? 60);
      this.focarPrimeiroDigito();
    } catch (erro) {
      this.tratarErro(erro, 'Não foi possível solicitar o código de recuperação.');
    } finally {
      this.carregando.set(false);
    }
  }

  async submeterRedefinicaoSenha(): Promise<void> {
    this.limparMensagens();
    const codigo = this.codigoCompleto();
    const novaSenha = this.novaSenhaRecuperacao();
    const confirmacao = this.confirmarNovaSenha();

    if (!/^\d{6}$/.test(codigo)) {
      this.mensagemErro.set('Digite os 6 números do código recebido por e-mail.');
      return;
    }
    if (novaSenha.length < 8) {
      this.mensagemErro.set('A nova senha deve ter pelo menos 8 caracteres.');
      return;
    }
    if (novaSenha !== confirmacao) {
      this.mensagemErro.set('A confirmação da nova senha não confere.');
      return;
    }

    this.carregando.set(true);
    try {
      const resposta = await this.authService.redefinirSenha(
        this.emailRecuperacao().trim(),
        codigo,
        novaSenha,
      );
      this.emailLogin.set(this.emailRecuperacao().trim());
      this.senhaLogin.set('');
      this.abaAtiva.set('login');
      this.zerarDigitos();
      this.mensagemSucesso.set(resposta.mensagem);
    } catch (erro) {
      this.tratarErro(erro, 'Não foi possível redefinir a senha.');
    } finally {
      this.carregando.set(false);
    }
  }

  aoDigitarCodigo(indice: number, evento: Event): void {
    const input = evento.target as HTMLInputElement;
    const apenasNumeros = (input.value ?? '').replace(/\D/g, '');
    const digito = apenasNumeros.slice(-1);

    const copia = [...this.digitosCodigo()];
    copia[indice] = digito;
    this.digitosCodigo.set(copia);
    input.value = digito;

    if (digito && indice < 5) {
      this.focarDigito(indice + 1);
    }
  }

  aoPressionarTeclaCodigo(indice: number, evento: KeyboardEvent): void {
    if (evento.key === 'Backspace' && !this.digitosCodigo()[indice] && indice > 0) {
      const copia = [...this.digitosCodigo()];
      copia[indice - 1] = '';
      this.digitosCodigo.set(copia);
      this.focarDigito(indice - 1);
      return;
    }
    if (evento.key === 'ArrowLeft' && indice > 0) {
      evento.preventDefault();
      this.focarDigito(indice - 1);
      return;
    }
    if (evento.key === 'ArrowRight' && indice < 5) {
      evento.preventDefault();
      this.focarDigito(indice + 1);
    }
  }

  aoColarCodigo(evento: ClipboardEvent): void {
    const colado = evento.clipboardData?.getData('text') ?? '';
    const numeros = colado.replace(/\D/g, '').slice(0, 6);
    if (!numeros) {
      return;
    }
    evento.preventDefault();
    const novo = ['', '', '', '', '', ''];
    for (let i = 0; i < numeros.length; i++) {
      novo[i] = numeros[i];
    }
    this.digitosCodigo.set(novo);
    this.focarDigito(Math.min(numeros.length, 5));
  }

  private zerarDigitos(): void {
    this.digitosCodigo.set(['', '', '', '', '', '']);
  }

  private focarPrimeiroDigito(): void {
    queueMicrotask(() => this.focarDigito(0));
  }

  private focarDigito(indice: number): void {
    const lista = this.digitosInputs?.toArray();
    const alvo = lista?.[indice]?.nativeElement;
    alvo?.focus();
    alvo?.select();
  }

  private iniciarCooldown(segundos: number): void {
    this.pararCooldown();
    if (segundos <= 0) {
      this.cooldownSegundos.set(0);
      return;
    }
    this.cooldownSegundos.set(segundos);
    this.intervaloCooldown = setInterval(() => {
      const atual = this.cooldownSegundos();
      if (atual <= 1) {
        this.cooldownSegundos.set(0);
        this.pararCooldown();
      } else {
        this.cooldownSegundos.set(atual - 1);
      }
    }, 1000);
  }

  private pararCooldown(): void {
    if (this.intervaloCooldown !== null) {
      clearInterval(this.intervaloCooldown);
      this.intervaloCooldown = null;
    }
  }

  private limparMensagens(): void {
    this.mensagemErro.set(null);
    this.mensagemSucesso.set(null);
  }

  private tratarErro(erro: unknown, padrao: string): void {
    if (erro instanceof ErroAutenticacao) {
      this.mensagemErro.set(erro.message || padrao);
      if (erro.retryAfterSeconds && erro.retryAfterSeconds > 0) {
        this.iniciarCooldown(erro.retryAfterSeconds);
      }
      return;
    }
    if (erro instanceof Error && erro.message) {
      this.mensagemErro.set(erro.message);
      return;
    }
    this.mensagemErro.set(padrao);
  }

  private emailPareceValido(email: string): boolean {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
  }
}
