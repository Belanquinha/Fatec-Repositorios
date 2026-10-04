import { Component, OnInit, inject } from '@angular/core';
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

  todosProjetos: ProjetoResponseModel[] = [];
  projetosFiltrados: ProjetoResponseModel[] = [];
  instituicoes: InstituicaoOption[] = [];

  termoBusca: string = '';
  instituicaoSelecionadaId: string = '';
  tagSelecionada: string = 'Todas';

  /**
   * Filtros derivados dos projetos que o backend devolveu, em vez de uma lista
   * fixa: qualquer tag que exista nos dados fica filtrável e nada é oferecido
   * que não exista no acervo.
   */
  tagsDisponiveis: string[] = ['Todas'];

  carregando: boolean = true;
  erroCarregamento: string | null = null;
  erroCarregarInstituicoes = false;
  usuarioLogado: UsuarioLogado | null = null;

  ngOnInit(): void {
    this.carregarDados();
    this.verificarUsuarioLogado();
  }

  async verificarUsuarioLogado(): Promise<void> {
    this.usuarioLogado = await this.authService.obterUsuarioLogado();
  }

  carregarDados(): void {
    this.carregando = true;
    this.erroCarregamento = null;

    this.projetoService.listarInstituicoes().subscribe({
      next: (insts) => {
        this.instituicoes = insts.filter((i) => i.ativo);
        this.erroCarregarInstituicoes = false;
      },
      error: (err) => {
        // O filtro por instituição é acessório: um erro aqui não deve derrubar
        // o catálogo, mas também não pode ser escondido atrás de dados falsos.
        this.instituicoes = [];
        this.erroCarregarInstituicoes = true;
        console.error('Erro ao carregar instituições:', err);
      }
    });

    this.projetoService.listarProjetosPublicos().subscribe({
      next: (projetos) => {
        this.todosProjetos = projetos;
        this.atualizarTagsDisponiveis();
        this.aplicarFiltros();
        this.carregando = false;
      },
      error: (err) => {
        console.error('Erro ao carregar projetos:', err);
        this.erroCarregamento =
          'Não foi possível carregar o catálogo de projetos no momento. Verifique sua conexão e tente novamente.';
        this.carregando = false;
      }
    });
  }

  private atualizarTagsDisponiveis(): void {
    const tags = new Set<string>();
    this.todosProjetos.forEach((proj) => proj.palavrasChave?.forEach((t) => tags.add(t)));
    this.tagsDisponiveis = ['Todas', ...Array.from(tags).sort((a, b) => a.localeCompare(b, 'pt-BR'))];

    // A tag selecionada pode não existir mais após recarregar
    if (!this.tagsDisponiveis.includes(this.tagSelecionada)) {
      this.tagSelecionada = 'Todas';
    }
  }

  aplicarFiltros(): void {
    const termo = this.termoBusca.trim().toLowerCase();

    this.projetosFiltrados = this.todosProjetos.filter((proj) => {
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
    });
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

  get primeiroNome(): string {
    if (!this.usuarioLogado?.nome) return '';
    const nome = this.usuarioLogado.nome.split(' ')[0];
    return nome.charAt(0).toUpperCase() + nome.slice(1).toLowerCase();
  }
}
