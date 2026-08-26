import { Component } from '@angular/core';
import { FormGroup, FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login-instituicao',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login-instituicao.html',
  styleUrl: './login-instituicao.css',
})
export class LoginInstituicao {
  loginForm: FormGroup;
  mensagemErro = '';
  enviando = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      senha: ['', Validators.required],
      lembrar: [false],
    });
  }

  async onSubmit(): Promise<void> {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      this.mensagemErro = 'Preencha email e senha válidos.';
      return;
    }

    const { email, senha } = this.loginForm.value;
    this.enviando = true;
    this.mensagemErro = '';

    try {
      await this.authService.loginInstituicao(email, senha);
      this.router.navigateByUrl('/');
    } catch (erro) {
      this.mensagemErro = erro instanceof Error ? erro.message : 'Erro ao realizar o login.';
    } finally {
      this.enviando = false;
    }
  }
}
