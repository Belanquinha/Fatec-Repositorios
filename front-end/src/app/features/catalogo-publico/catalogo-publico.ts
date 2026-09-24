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

  tagsDisponiveis: string[] = [
    'Todas',
    'Web',
    'IoT',
    'IA & Dados',
    'Gestão',
    'Mobile',
    'Sustentabilidade',
    'Saúde'
  ];

  carregando: boolean = true;
  erroCarregamento: string | null = null;
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
      },
      error: () => {
        // Fallback silencioso tratado pelo serviço
      }
    });

    this.projetoService.listarProjetosPublicos().subscribe({
      next: (projetos) => {
        this.todosProjetos = projetos;
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
