import { Component } from '@angular/core';

import { CardProjeto } from '../../shared/components/card-projeto/card-projeto';

@Component({
  selector: 'app-pagina-inicial',
  imports: [CardProjeto],
  templateUrl: './pagina-inicial.html',
  styleUrl: './pagina-inicial.css',
})
export class PaginaInicial {}
