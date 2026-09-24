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

  get capaUrl(): string {
    return this.projeto?.imagemCapaUrl || 'capa_card.png';
  }

  get titulo(): string {
    return this.projeto?.titulo || 'Golden Maker';
  }

  get descricao(): string {
    return (
      this.projeto?.descricaoCurta ||
      'Este site oferece um espaço livre e criativo para produtores independentes mostrarem seu talento.'
    );
  }

  get instituicao(): string {
    return this.projeto?.instituicaoNome || 'Fatec Ipiranga';
  }

  get tags(): string[] {
    if (this.projeto?.palavrasChave && this.projeto.palavrasChave.length > 0) {
      return this.projeto.palavrasChave.slice(0, 3);
    }
    return ['Tecnologia', 'Inovação'];
  }

  get ano(): number {
    return this.projeto?.anoPublicado || 2026;
  }

  onImgError(event: Event) {
    const target = event.target as HTMLImageElement;
    if (target) {
      target.src = 'capa_card.png';
    }
  }
}
