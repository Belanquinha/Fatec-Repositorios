import {
  Component,
  OnInit,
  computed,
  inject,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ProjetoService } from '../../core/services/projeto.service';
import { InstituicaoOption } from '../../core/models/projeto.model';

@Component({
  selector: 'app-instituicoes',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './instituicoes.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class Instituicoes implements OnInit {
  private projetoService = inject(ProjetoService);

  todasInstituicoes = signal<InstituicaoOption[]>([]);
  termoBusca = signal<string>('');
  regiaoSelecionada = signal<string>('Todas');
  carregando = signal<boolean>(true);
  erro = signal<string | null>(null);

  regioesDisponiveis = computed(() => {
    const set = new Set<string>();
    for (const inst of this.todasInstituicoes()) {
      if (inst.regiaoAdministrativa?.trim()) {
        set.add(inst.regiaoAdministrativa.trim());
      }
    }
    return ['Todas', ...Array.from(set).sort((a, b) => a.localeCompare(b, 'pt-BR'))];
  });

  instituicoesFiltradas = computed(() => {
    const termo = this.termoBusca().trim().toLowerCase();
    const regiao = this.regiaoSelecionada();

    return this.todasInstituicoes().filter((inst) => {
      const bateRegiao =
        regiao === 'Todas' ||
        (inst.regiaoAdministrativa?.trim().toLowerCase() === regiao.toLowerCase());

      if (!bateRegiao) return false;

      if (!termo) return true;

      const nome = inst.nome?.toLowerCase() ?? '';
      const cidade = inst.cidade?.toLowerCase() ?? '';
      const codigo = inst.codigoUnidade?.toLowerCase() ?? '';
      const endereco = inst.endereco?.toLowerCase() ?? '';

      return (
        nome.includes(termo) ||
        cidade.includes(termo) ||
        codigo.includes(termo) ||
        endereco.includes(termo)
      );
    });
  });

  ngOnInit(): void {
    this.carregarInstituicoes();
  }

  carregarInstituicoes(): void {
    this.carregando.set(true);
    this.erro.set(null);

    this.projetoService.listarInstituicoes().subscribe({
      next: (dados) => {
        // Ordena por nome
        const ordenadas = [...dados].sort((a, b) =>
          a.nome.localeCompare(b.nome, 'pt-BR')
        );
        this.todasInstituicoes.set(ordenadas);
        this.carregando.set(false);
      },
      error: () => {
        this.erro.set('Não foi possível carregar a lista de instituições.');
        this.carregando.set(false);
      },
    });
  }

  limparFiltros(): void {
    this.termoBusca.set('');
    this.regiaoSelecionada.set('Todas');
  }
}
