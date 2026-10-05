import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { CardProjeto } from '../../shared/components/card-projeto/card-projeto';
import { ProjetoService } from '../../core/services/projeto.service';
import { AuthService } from '../../core/auth/auth.service';
import { ProjetoResponseModel, InstituicaoOption } from '../../core/models/projeto.model';
import { UsuarioLogado } from '../../core/auth/models/usuario-logado';

@Component({
  selector: 'app-catalogo-publico',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, CardProjeto],
  templateUrl: './catalogo-publico.html',
  styleUrl: './catalogo-publico.css',
})
export class CatalogoPublico implements OnInit {
  private projetoService = inject(ProjetoService);
  private authService = inject(AuthService);

  todosProjetos = signal<ProjetoResponseModel[]>([]);
  projetosFiltrados = signal<ProjetoResponseModel[]>([]);
  instituicoes = signal<InstituicaoOption[]>([]);

  termoBusca: string = '';
  instituicaoSelecionadaId: string = '';
  tagSelecionada: string = 'Todas';

  /**
   * Filtros derivados dos projetos que o backend devolveu, em vez de uma lista
   * fixa: qualquer tag que exista nos dados fica filtrável e nada é oferecido
   * que não exista no acervo.
   */
  tagsDisponiveis = signal<string[]>(['Todas']);

  carregando = signal<boolean>(true);
  erroCarregamento = signal<string | null>(null);
  erroCarregarInstituicoes = signal(false);
  usuarioLogado = signal<UsuarioLogado | null>(null);

  primeiroNome = computed(() => {
    const nome = this.usuarioLogado()?.nome;
    if (!nome) return '';
    const primeiro = nome.split(' ')[0];
    return primeiro.charAt(0).toUpperCase() + primeiro.slice(1).toLowerCase();
  });

  ngOnInit(): void {
    this.carregarDados();
    this.verificarUsuarioLogado();
  }

  async verificarUsuarioLogado(): Promise<void> {
    this.usuarioLogado.set(await this.authService.obterUsuarioLogado());
  }

  carregarDados(): void {
    this.carregando.set(true);
    this.erroCarregamento.set(null);

    this.projetoService.listarInstituicoes().subscribe({
      next: (insts) => {
        this.instituicoes.set(insts.filter((i) => i.ativo));
        this.erroCarregarInstituicoes.set(false);
      },
      error: (err) => {
        // O filtro por instituição é acessório: um erro aqui não deve derrubar
        // o catálogo, mas também não pode ser escondido atrás de dados falsos.
        this.instituicoes.set([]);
        this.erroCarregarInstituicoes.set(true);
        console.error('Erro ao carregar instituições:', err);
      }
    });

    this.projetoService.listarProjetosPublicos().subscribe({
      next: (projetos) => {
        this.todosProjetos.set(projetos);
        this.atualizarTagsDisponiveis();
        this.aplicarFiltros();
        this.carregando.set(false);
      },
      error: (err) => {
        console.error('Erro ao carregar projetos:', err);
        this.erroCarregamento.set(
          'Não foi possível carregar o catálogo de projetos no momento. Verifique sua conexão e tente novamente.');
        this.carregando.set(false);
      }
    });
  }

  private atualizarTagsDisponiveis(): void {
    const tags = new Set<string>();
    this.todosProjetos().forEach((proj) => proj.palavrasChave?.forEach((t) => tags.add(t)));
    const lista = ['Todas', ...Array.from(tags).sort((a, b) => a.localeCompare(b, 'pt-BR'))];
    this.tagsDisponiveis.set(lista);

    // A tag selecionada pode não existir mais após recarregar
    if (!lista.includes(this.tagSelecionada)) {
      this.tagSelecionada = 'Todas';
    }
  }

  aplicarFiltros(): void {
    const termo = this.termoBusca.trim().toLowerCase();
    const todos = this.todosProjetos();

    this.projetosFiltrados.set(todos.filter((proj) => {
      const bateTermo =
        !termo ||
        proj.titulo.toLowerCase().includes(termo) ||
        proj.descricaoCurta.toLowerCase().includes(termo) ||
        proj.instituicaoNome.toLowerCase().includes(termo) ||
        (proj.palavrasChave && proj.palavrasChave.some((p) => p.toLowerCase().includes(termo)));

      const bateInstituicao =
        !this.instituicaoSelecionadaId ||
        proj.instituicaoId === this.instituicaoSelecionadaId ||
        proj.instituicaoNome === this.instituicaoSelecionadaId;

      const bateTag =
        this.tagSelecionada === 'Todas' ||
        (proj.palavrasChave &&
          proj.palavrasChave.some((p) =>
            p.toLowerCase().includes(this.tagSelecionada.toLowerCase())
          ));

      return bateTermo && bateInstituicao && bateTag;
    }));
  }

  selecionarTag(tag: string): void {
    this.tagSelecionada = tag;
    this.aplicarFiltros();
  }

  limparFiltros(): void {
    this.termoBusca = '';
    this.instituicaoSelecionadaId = '';
    this.tagSelecionada = 'Todas';
    this.aplicarFiltros();
  }

  get temFiltrosAtivos(): boolean {
    return (
      this.termoBusca.trim() !== '' ||
      this.instituicaoSelecionadaId !== '' ||
      this.tagSelecionada !== 'Todas'
    );
  }
}
