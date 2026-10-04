import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ProjetoResponseModel } from '../../../core/models/projeto.model';

@Component({
  selector: 'app-card-projeto',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './card-projeto.html',
  styleUrl: './card-projeto.css',
})
export class CardProjeto {
  @Input() projeto?: ProjetoResponseModel;

  /**
   * Os getters abaixo não inventam conteúdo: um campo ausente no projeto resulta
   * em string vazia (ou lista vazia) e o template decide o que exibir no lugar.
   */

  get capaUrl(): string {
    return this.projeto?.imagemCapaUrl ?? '';
  }

  get titulo(): string {
    return this.projeto?.titulo ?? '';
  }

  get descricao(): string {
    return this.projeto?.descricaoCurta ?? '';
  }

  get instituicao(): string {
    return this.projeto?.instituicaoNome ?? '';
  }

  get tags(): string[] {
    return this.projeto?.palavrasChave?.slice(0, 3) ?? [];
  }

  get ano(): number | null {
    return this.projeto?.anoPublicado ?? null;
  }

  /** Sem capa cadastrada, o template mostra um bloco neutro no lugar da imagem. */
  onImgError(event: Event) {
    const target = event.target as HTMLImageElement;
    if (target) {
      target.style.display = 'none';
    }
  }
}
