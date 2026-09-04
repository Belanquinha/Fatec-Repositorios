import { Component } from '@angular/core';
import { RouterLink } from "@angular/router";
import { CommonModule } from '@angular/common';
import { CadastroInstituicao } from "../cadastro-instituicao/cadastro-instituicao";

@Component({
  selector: 'app-main-admin',
  imports: [RouterLink, CommonModule, CadastroInstituicao],
  templateUrl: './main-admin.html',
  styleUrl: './main-admin.css',
})
export class MainAdmin {
  cadastroInstituicaoAberto = false;

  toggleCadastroInstituicao() {
    this.cadastroInstituicaoAberto = !this.cadastroInstituicaoAberto;
  }
}
