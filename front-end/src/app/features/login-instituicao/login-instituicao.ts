import { Component } from '@angular/core';
import { FormGroup, FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from "@angular/router";


@Component({
  selector: 'app-login-instituicao',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login-instituicao.html',
  styleUrl: './login-instituicao.css',
})
export class LoginInstituicao {
  loginForm: FormGroup;

  constructor(private fb: FormBuilder) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      senha: ['', Validators.required],
      lembrar: [false]
    });
  }

  onSubmit() {
    if (this.loginForm.valid) {
      const dados = this.loginForm.value;
      // espaço para serviço HTTP de POST aqui enviando os dados
    }
  }
}
