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
import { PerfilPublicoUsuarioModel } from '../../core/models/projeto.model';
import { CardProjeto } from '../../shared/components/card-projeto/card-projeto';

@Component({
  selector: 'app-perfil-publico',
  standalone: true,
  imports: [CommonModule, RouterModule, CardProjeto],
  templateUrl: './perfil-publico.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class PerfilPublico implements OnInit {
  private route = inject(ActivatedRoute);
  private projetoService = inject(ProjetoService);

  perfil = signal<PerfilPublicoUsuarioModel | null>(null);
  carregando = signal<boolean>(true);
  erro = signal<string | null>(null);

  iniciais = computed(() => {
    const nome = this.perfil()?.nome?.trim();
    if (!nome) return '??';
    const partes = nome.split(/\s+/);
    if (partes.length === 1) return partes[0].slice(0, 2).toUpperCase();
    return (partes[0][0] + partes[partes.length - 1][0]).toUpperCase();
  });

  rotuloRole = computed(() => {
    const role = this.perfil()?.role;
    if (role === 'PROFESSOR') return 'Professor(a)';
    if (role === 'ADMIN') return 'Administrador';
    return 'Aluno(a)';
  });

  ehProfessor = computed(() => this.perfil()?.role === 'PROFESSOR');

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      if (id) {
        this.carregarPerfil(id);
      } else {
        this.erro.set('Identificador do usuário não fornecido.');
        this.carregando.set(false);
      }
    });
  }

  carregarPerfil(id: string): void {
    this.carregando.set(true);
    this.erro.set(null);

    this.projetoService.obterPerfilUsuario(id).subscribe({
      next: (dados) => {
        this.perfil.set(dados);
        this.carregando.set(false);
      },
      error: () => {
        this.erro.set('Usuário não encontrado ou erro ao carregar os dados.');
        this.carregando.set(false);
      },
    });
  }
}
