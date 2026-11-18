import {
  Component,
  OnInit,
  computed,
  inject,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ProjetoService } from '../../core/services/projeto.service';
import { PerfilInstituicaoModel } from '../../core/models/projeto.model';
import { CardProjeto } from '../../shared/components/card-projeto/card-projeto';

@Component({
  selector: 'app-perfil-instituicao',
  standalone: true,
  imports: [CommonModule, RouterModule, CardProjeto],
  templateUrl: './perfil-instituicao.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class PerfilInstituicao implements OnInit {
  private route = inject(ActivatedRoute);
  private projetoService = inject(ProjetoService);

  perfil = signal<PerfilInstituicaoModel | null>(null);
  cursoFiltroId = signal<string | null>(null);
  carregando = signal<boolean>(true);
  erro = signal<string | null>(null);

  projetosFiltrados = computed(() => {
    const dados = this.perfil();
    if (!dados || !dados.projetos) return [];

    const cursoId = this.cursoFiltroId();
    if (!cursoId) return dados.projetos;

    return dados.projetos.filter((p) => p.cursoId === cursoId);
  });

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      if (id) {
        this.carregarPerfil(id);
      } else {
        this.erro.set('Identificador da instituição não fornecido.');
        this.carregando.set(false);
      }
    });
  }

  carregarPerfil(id: string): void {
    this.carregando.set(true);
    this.erro.set(null);
    this.cursoFiltroId.set(null);

    this.projetoService.obterPerfilInstituicao(id).subscribe({
      next: (dados) => {
        this.perfil.set(dados);
        this.carregando.set(false);
      },
      error: () => {
        this.erro.set('Instituição não encontrada ou erro ao carregar os dados.');
        this.carregando.set(false);
      },
    });
  }

  filtrarPorCurso(cursoId: string | null): void {
    this.cursoFiltroId.set(cursoId);
  }
}
