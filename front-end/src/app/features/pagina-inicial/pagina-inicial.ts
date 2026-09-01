import { Component } from '@angular/core';

import { CardProjeto } from '../../shared/components/card-projeto/card-projeto';
import { RouterLink } from "@angular/router";

@Component({
  selector: 'app-pagina-inicial',
  imports: [CardProjeto, RouterLink],
  templateUrl: './pagina-inicial.html',
  styleUrl: './pagina-inicial.css',
})
export class PaginaInicial {}
