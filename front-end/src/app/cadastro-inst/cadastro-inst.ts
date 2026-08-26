import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormControl, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-cadastro-inst',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
templateUrl: './cadastro-inst.html',  styleUrls: ['./cadastro-inst.css']
})
export class CadastroInstComponent {
  form = new FormGroup({
    nomeInstituicao: new FormControl('', Validators.required),
    cidade: new FormControl('', Validators.required),
    estado: new FormControl('', Validators.required),
    telefoneGestor: new FormControl(''),
    senhaGestor: new FormControl('')
  });

  constructor(private router: Router) {}

  onSubmit() {
    if (this.form.valid) {
      console.log('Dados do formulário:', this.form.value);
    }
  }

  irParaHome() {
    this.router.navigate(['/']);
  }
}