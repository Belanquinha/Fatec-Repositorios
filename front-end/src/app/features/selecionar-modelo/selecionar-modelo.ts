import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TipoProjeto } from '../../core/models/projeto.model';

@Component({
  selector: 'app-selecionar-modelo',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './selecionar-modelo.html',
  styleUrl: './selecionar-modelo.css',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class SelecionarModelo {
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  selecionar(modelo: TipoProjeto): void {
    const instituicaoId = this.route.snapshot.queryParamMap.get('instituicaoId');
    this.router.navigate(['/projeto-forms'], {
      queryParams: {
        modelo,
        ...(instituicaoId ? { instituicaoId } : {}),
      },
    });
  }
}
