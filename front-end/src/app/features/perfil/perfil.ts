import {
  Component,
  OnInit,
  OnDestroy,
  ViewChild,
  ElementRef,
  HostListener,
  computed,
  inject,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../core/auth/auth.service';
import { UsuarioLogado } from '../../core/auth/models/usuario-logado';
import { ProjetoService } from '../../core/services/projeto.service';
import {
  InstituicaoOption,
  ProjetoResponseModel,
} from '../../core/models/projeto.model';
import { mensagemErroAmigavel } from '../../core/utils/erro-http';
import { environment } from '../../../environments/environment';

/**
 * Tela de Perfil (layout do mock "LuePad" · cor de ação #D32F2F).
 *
 * Aluno e professor compartilham o mesmo layout: identidade + abas + grade de
 * cards. A aba "Seus Projetos" lista o que o próprio usuário submeteu e já está
 * aprovado (`GET /projetos/meus` fatiado no cliente). A aba "Pendentes" muda de
 * significado por papel: para o aluno, os projetos enviados que ainda estão em
 * análise; para o professor, a fila aguardando a sua aprovação
 * (`GET /projetos/pendentes`). A aba "Recusados" é visual por enquanto.
 *
 * O fatiamento por estado é feito no cliente porque `/projetos/meus` devolve
 * todos os estados de uma vez; o backend continua sendo a autoridade nas
 * aprovações/rejeições.
 */

type AbaPerfil = 'publicados' | 'pendentes';

/** Estados que, para o aluno, significam "enviado e ainda não avaliado". */
const ESTADOS_EM_ANALISE: readonly ProjetoResponseModel['estado'][] = [
  'AGUARDANDO_APROVACAO',
  'REENVIADO',
  'ALTERACOES_SOLICITADAS',
];

@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './perfil.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './perfil.css',
})
export class Perfil implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private projetoService = inject(ProjetoService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  usuario = signal<UsuarioLogado | null>(null);
  carregando = signal(true);
  erroContexto = signal<string | null>(null);
  /** Aviso de ação ainda não implementada ("Em breve"), some sozinho. */
  avisoGeral = signal<string | null>(null);

  // --- Abas / grade ---
  abaAtiva = signal<AbaPerfil>('publicados');
  /** Signal (não campo solto): o computed da busca precisa ser invalidado. */
  buscaPerfil = signal('');
  meusProjetos = signal<ProjetoResponseModel[]>([]);
  carregandoMeus = signal(false);
  erroMeus = signal<string | null>(null);

  isAluno = computed(() => this.usuario()?.role === 'ALUNO');
  isProfessor = computed(() => this.usuario()?.role === 'PROFESSOR');

  // --- Aba "Instituições (0–4)" do professor (Frame B) ---
  readonly LIMITE_VINCULOS = 4;
  readonly LIMITE_SUGESTOES = 20;

  /** Vínculos persistidos (espelho do servidor). */
  vinculos = signal<InstituicaoOption[]>([]);
  /** Seleção local de trabalho — só o Save chama o servidor. */
  selecao = signal<InstituicaoOption[]>([]);
  catalogo = signal<InstituicaoOption[]>([]);

  buscaVinculo = '';
  sugestoesVinculo: InstituicaoOption[] = [];
  totalEncontradas = 0;
  mostrarDropdownVinculo = signal(false);
  destaqueVinculo = -1;

  carregandoVinculos = signal(false);
  erroVinculos = signal<string | null>(null);
  salvandoVinculos = signal(false);
  erroSalvarVinculos = signal<string | null>(null);
  sucessoVinculos = signal<string | null>(null);
  /** Recusa inline da 5ª (AD-4): cinza neutro + aria-live, sem modal. */
  tetoRecusado = signal(false);
  logosComErro = new Set<string>();

  /** Modal "Mudar instituições": a edição dos vínculos sai do fim da página. */
  modalVinculosAberto = signal(false);
  @ViewChild('modalVinculos') modalVinculos!: ElementRef<HTMLElement>;
  /** Foco a restaurar ao fechar o modal (o Tab volta para o botão). */
  private ultimoFocoAntesDoModal: HTMLElement | null = null;
  /** `overflow` do body antes da trava — restaurado ao fechar (não zerado). */
  private overflowAnteriorDoBodyModal: string | null = null;

  // --- Pendentes para aprovação (professor responsável indicado) ---
  pendentes = signal<ProjetoResponseModel[]>([]);
  carregandoPendentes = signal(false);
  erroPendentes = signal<string | null>(null);
  sucessoAcao = signal<string | null>(null);
  processandoId = signal<string | null>(null);
  rejeicaoAbertaId = signal<string | null>(null);
  motivosRejeicao = signal<Record<string, string>>({});
  /** Capas que falharam no <img>: o card mostra o bloco neutro no lugar. */
  capasComErro = new Set<string>();

  // --- Derivados das abas ---

  /** Aprovados que o próprio usuário submeteu (aba "Seus Projetos"). */
  totalPublicados = computed(
    () => this.meusProjetos().filter((p) => p.estado === 'APROVADO').length,
  );

  /** Enviados pelo aluno e ainda em análise. */
  totalEmAnalise = computed(
    () =>
      this.meusProjetos().filter((p) =>
        ESTADOS_EM_ANALISE.includes(p.estado),
      ).length,
  );

  /**
   * "Pendentes" é papel-dependente: aluno vê o que mandou e está em análise;
   * professor vê a fila de aprovação (`/projetos/pendentes`).
   */
  totalPendentes = computed(() =>
    this.isProfessor() ? this.pendentes().length : this.totalEmAnalise(),
  );

  projetosAba = computed<ProjetoResponseModel[]>(() => {
    if (this.abaAtiva() === 'publicados') {
      return this.meusProjetos().filter((p) => p.estado === 'APROVADO');
    }
    if (this.isProfessor()) {
      return this.pendentes();
    }
    return this.meusProjetos().filter((p) =>
      ESTADOS_EM_ANALISE.includes(p.estado),
    );
  });

  carregandoAba = computed(() =>
    this.abaAtiva() === 'pendentes' && this.isProfessor()
      ? this.carregandoPendentes()
      : this.carregandoMeus(),
  );

  erroAba = computed(() =>
    this.abaAtiva() === 'pendentes' && this.isProfessor()
      ? this.erroPendentes()
      : this.erroMeus(),
  );

  /** Busca local por título, descrição ou instituição (a aba já vem fatiada). */
  projetosFiltrados = computed(() => {
    const lista = this.projetosAba();
    const termo = this.normalizar(this.buscaPerfil());
    if (!termo) {
      return lista;
    }
    return lista.filter(
      (p) =>
        this.normalizar(p.titulo).includes(termo) ||
        this.normalizar(p.descricaoCurta).includes(termo) ||
        this.normalizar(p.instituicaoNome).includes(termo),
    );
  });

  textoPaginacao = computed(() => {
    const total = this.projetosFiltrados().length;
    const rotulo =
      this.abaAtiva() === 'publicados'
        ? this.isProfessor()
          ? 'projetos aprovados'
          : 'projetos publicados'
        : 'projetos pendentes';
    return `Exibindo 1–${total} de ${total} ${rotulo}`;
  });

  mensagemVazia = computed(() => {
    if (this.abaAtiva() === 'publicados') {
      return this.isProfessor()
        ? 'Nenhum projeto aprovado por você ainda.'
        : 'Você ainda não publicou nenhum projeto.';
    }
    return this.isProfessor()
      ? 'Nenhum projeto aguardando sua aprovação.'
      : 'Nenhum projeto enviado em análise.';
  });

  notaVazia = computed(() => {
    if (this.abaAtiva() === 'publicados') {
      return this.isProfessor()
        ? 'Quando você aprovar um projeto, ele aparece aqui.'
        : 'Projetos aprovados pelo professor aparecem aqui.';
    }
    return this.isProfessor()
      ? 'Projetos indicados ao seu e-mail institucional aparecem aqui para revisão.'
      : 'Projetos enviados aparecem aqui até o professor responsável avaliar.';
  });

  contadorVinculos = computed(
    () => `${this.selecao().length} de ${this.LIMITE_VINCULOS} unidades`,
  );
  noTeto = computed(() => this.selecao().length >= this.LIMITE_VINCULOS);
  temAlteracoes = computed(() => {
    const salvos = this.vinculos()
      .map((v) => v.id)
      .sort();
    const atuais = this.selecao()
      .map((v) => v.id)
      .sort();
    return (
      salvos.length !== atuais.length ||
      salvos.some((id, i) => id !== atuais[i])
    );
  });

  /** Avatar quadrado com duas iniciais, como no mock. */
  iniciais = computed(() => {
    const nome = (this.usuario()?.nome ?? '').trim();
    if (!nome) {
      return '?';
    }
    const partes = nome.split(/\s+/);
    const primeira = partes[0].charAt(0);
    const segunda = partes.length > 1 ? partes[partes.length - 1].charAt(0) : '';
    return (primeira + segunda).toUpperCase();
  });

  /** "Fatec Ipiranga / Fatec São Paulo" a partir dos vínculos do professor. */
  instituicoesResumo = computed(() => {
    const lista = this.vinculos();
    if (lista.length === 0) {
      return '';
    }
    return lista.map((i) => i.nome).join(' / ');
  });

  async ngOnInit(): Promise<void> {
    const logado = await this.authService.obterUsuarioLogado();
    if (!logado) {
      // O guard barra a rota, mas a sessão pode vencer com a tela aberta.
      await this.router.navigateByUrl('/');
      return;
    }
    this.usuario.set(logado);

    // Deep link opcional: /perfil?instituicoes=1 rola até a seção de vínculos.
    if (this.route.snapshot.queryParamMap.get('instituicoes') === '1') {
      setTimeout(() => this.irParaInstituicoes(), 200);
    }

    if (logado.role === 'PROFESSOR') {
      this.carregarVinculos();
      this.carregarPendentes();
    }
    this.carregarMeusProjetos();
  }

  ngOnDestroy(): void {
    if (
      typeof document !== 'undefined' &&
      this.overflowAnteriorDoBodyModal !== null
    ) {
      document.body.style.overflow = this.overflowAnteriorDoBodyModal;
      this.overflowAnteriorDoBodyModal = null;
    }
  }

  /** "Seus Projetos" em todos os estados; as abas fatiam no cliente. */
  carregarMeusProjetos(): void {
    this.carregandoMeus.set(true);
    this.erroMeus.set(null);
    this.projetoService.obterMeusProjetos().subscribe({
      next: (projetos) => {
        this.meusProjetos.set(projetos ?? []);
        this.carregandoMeus.set(false);
        this.carregando.set(false);
      },
      error: (err) => {
        console.error('Erro ao carregar projetos do perfil:', err);
        // Identidade do token continua exibida: o que falhou foi o contexto,
        // não a sessão (DESIGN 4.6, estado de erro).
        this.erroMeus.set(
          'Não foi possível carregar seus projetos agora. Tente novamente mais tarde.',
        );
        this.carregandoMeus.set(false);
        this.carregando.set(false);
      },
    });
  }

  /** Vínculos + catálogo com falhas independentes: um lado bom nunca é apagado pelo outro. */
  carregarVinculos(): void {
    this.carregandoVinculos.set(true);
    this.erroVinculos.set(null);
    forkJoin({
      catalogo: this.projetoService
        .listarInstituicoes()
        .pipe(catchError(() => of(null))),
      vinculos: this.projetoService
        .obterMeusVinculos()
        .pipe(catchError(() => of(null))),
    }).subscribe(({ catalogo, vinculos }) => {
      if (vinculos === null) {
        this.erroVinculos.set(
          'Não foi possível carregar suas instituições agora. Verifique sua conexão.',
        );
      } else {
        // Garante ordem por nome como o servidor promete.
        const ordenados = [...(vinculos ?? [])].sort((a, b) =>
          a.nome.localeCompare(b.nome, 'pt-BR'),
        );
        this.vinculos.set(ordenados);
        // A seleção só é sincronizada quando os vínculos carregaram de verdade:
        // uma falha transitória do catálogo jamais zera vínculos salvos
        // (PUT [] acidental apagaria tudo).
        this.selecao.set([...ordenados]);
      }
      if (catalogo === null) {
        this.erroVinculos.set(
          'Não foi possível carregar suas instituições agora. Verifique sua conexão.',
        );
      } else {
        const ativas = (catalogo ?? []).filter((i) => i.ativo);
        this.catalogo.set(ativas);
      }
      this.carregandoVinculos.set(false);
      this.carregando.set(false);
    });
  }

  /** Pendentes onde o professor é o responsável indicado (vínculo não é barreira). */
  carregarPendentes(): void {
    this.carregandoPendentes.set(true);
    this.erroPendentes.set(null);
    this.projetoService.listarPendentesAprovacao().subscribe({
      next: (lista) => {
        this.pendentes.set(lista ?? []);
        this.carregandoPendentes.set(false);
      },
      error: (err) => {
        console.error('Erro ao carregar pendentes para aprovação:', err);
        this.erroPendentes.set(
          mensagemErroAmigavel(
            err,
            'Não foi possível carregar os projetos pendentes agora. Tente novamente.',
          ),
        );
        this.carregandoPendentes.set(false);
      },
    });
  }

  tentarNovamentePendentes(): void {
    this.carregarPendentes();
  }

  recarregarAba(): void {
    if (this.abaAtiva() === 'pendentes' && this.isProfessor()) {
      this.carregarPendentes();
    } else {
      this.carregarMeusProjetos();
    }
  }

  selecionarAba(aba: AbaPerfil): void {
    if (this.abaAtiva() === aba) {
      return;
    }
    this.abaAtiva.set(aba);
    // Erros e sucessos são da aba: trocar de aba não arrasta o aviso da outra.
    this.erroPendentes.set(null);
    this.sucessoAcao.set(null);
    this.rejeicaoAbertaId.set(null);
  }

  abrirRejeicao(id: string): void {
    this.rejeicaoAbertaId.set(id);
    this.erroPendentes.set(null);
    this.sucessoAcao.set(null);
  }

  fecharRejeicao(): void {
    this.rejeicaoAbertaId.set(null);
  }

  onMotivoRejeicao(id: string, valor: string): void {
    this.motivosRejeicao.set({ ...this.motivosRejeicao(), [id]: valor });
  }

  confirmarRejeicao(id: string): void {
    const motivo = (this.motivosRejeicao()[id] || '').trim();
    if (!motivo) {
      this.erroPendentes.set('Informe o motivo da rejeição para o aluno.');
      return;
    }
    if (this.processandoId()) {
      return;
    }
    this.processandoId.set(id);
    this.erroPendentes.set(null);
    this.sucessoAcao.set(null);
    this.projetoService.rejeitarProjeto(id, motivo).subscribe({
      next: (rejeitado) => {
        this.pendentes.set(this.pendentes().filter((p) => p.id !== id));
        this.processandoId.set(null);
        this.rejeicaoAbertaId.set(null);
        this.sucessoAcao.set(
          `Projeto "${rejeitado.titulo}" rejeitado com retorno ao aluno.`,
        );
      },
      error: (err) => {
        console.error('Erro ao rejeitar projeto:', err);
        this.processandoId.set(null);
        this.erroPendentes.set(
          mensagemErroAmigavel(
            err,
            'Não foi possível rejeitar o projeto agora. Tente novamente.',
          ),
        );
      },
    });
  }

  tentarNovamenteVinculos(): void {
    // Retry só da seção de vínculos: não dispara a rede dos pendentes.
    this.carregarVinculos();
  }

  // --- Autocomplete multi (tipo-para-revelar, Frame B §3) ---
  onBuscaVinculo(termo: string): void {
    this.buscaVinculo = termo;
    this.tetoRecusado.set(false);
    // Consulta vazia: sem popup (State Patterns).
    if (!termo.trim()) {
      this.sugestoesVinculo = [];
      this.totalEncontradas = 0;
      this.mostrarDropdownVinculo.set(false);
      this.destaqueVinculo = -1;
      return;
    }
    this.mostrarDropdownVinculo.set(true);
    this.aplicarFiltroVinculos();
  }

  abrirDropdownVinculo(): void {
    if (!this.buscaVinculo.trim() || this.carregandoVinculos()) {
      return;
    }
    this.mostrarDropdownVinculo.set(true);
    this.aplicarFiltroVinculos();
  }

  ocultarDropdownVinculoComDelay(): void {
    // Delay curto para o mousedown da opção vencer a corrida com o blur.
    setTimeout(() => {
      this.mostrarDropdownVinculo.set(false);
      this.destaqueVinculo = -1;
    }, 150);
  }

  private aplicarFiltroVinculos(): void {
    const termo = this.normalizar(this.buscaVinculo);
    const selecionados = new Set(this.selecao().map((s) => s.id));
    const encontradas = this.catalogo().filter((inst) => {
      if (selecionados.has(inst.id)) {
        return false;
      }
      return (
        this.normalizar(inst.nome).includes(termo) ||
        this.normalizar(inst.cidade || '').includes(termo) ||
        this.normalizar(inst.codigoUnidade || '').includes(termo) ||
        this.normalizar(inst.regiaoAdministrativa || '').includes(termo)
      );
    });
    // Contagem anunciada no aria-live é o total real, não as linhas renderizadas.
    this.totalEncontradas = encontradas.length;
    this.sugestoesVinculo = encontradas.slice(0, this.LIMITE_SUGESTOES);
    this.destaqueVinculo = this.sugestoesVinculo.length > 0 ? 0 : -1;
  }

  adicionarVinculo(inst: InstituicaoOption): void {
    if (this.selecao().some((s) => s.id === inst.id)) {
      return;
    }
    if (this.selecao().length >= this.LIMITE_VINCULOS) {
      // Teto: recusa inline em cinza, popup continua aberto, sem modal.
      this.tetoRecusado.set(true);
      return;
    }
    this.tetoRecusado.set(false);
    this.selecao.set([...this.selecao(), inst]);
    this.sucessoVinculos.set(null);
    this.erroSalvarVinculos.set(null);
    // Cada confirmação acrescenta um chip e reabre a consulta.
    this.buscaVinculo = '';
    this.sugestoesVinculo = [];
    this.totalEncontradas = 0;
    this.mostrarDropdownVinculo.set(false);
    this.destaqueVinculo = -1;
  }

  removerVinculo(id: string): void {
    this.tetoRecusado.set(false);
    this.selecao.set(this.selecao().filter((s) => s.id !== id));
    this.sucessoVinculos.set(null);
  }

  cancelarEdicaoVinculos(): void {
    this.selecao.set([...this.vinculos()]);
    this.buscaVinculo = '';
    this.sugestoesVinculo = [];
    this.mostrarDropdownVinculo.set(false);
    this.tetoRecusado.set(false);
    this.erroSalvarVinculos.set(null);
    this.sucessoVinculos.set(null);
  }

  salvarVinculos(): void {
    if (this.salvandoVinculos() || !this.temAlteracoes()) {
      return;
    }
    this.salvandoVinculos.set(true);
    this.erroSalvarVinculos.set(null);
    this.sucessoVinculos.set(null);
    const ids = this.selecao().map((s) => s.id);
    this.projetoService.salvarMeusVinculos(ids).subscribe({
      next: (persistidos) => {
        const ordenados = [...(persistidos ?? [])].sort((a, b) =>
          a.nome.localeCompare(b.nome, 'pt-BR'),
        );
        this.vinculos.set(ordenados);
        this.selecao.set([...ordenados]);
        this.salvandoVinculos.set(false);
        this.sucessoVinculos.set(
          ordenados.length === 0
            ? 'Vínculos atualizados. Você está sem instituições vinculadas.'
            : `Vínculos atualizados: ${ordenados.length} de ${this.LIMITE_VINCULOS} unidades.`,
        );
      },
      error: (err) => {
        console.error('Erro ao salvar vínculos do professor:', err);
        this.salvandoVinculos.set(false);
        // Mantém chips e contagem; mostra o envelope do servidor em painel neutro.
        this.erroSalvarVinculos.set(
          mensagemErroAmigavel(
            err,
            'Não foi possível salvar os vínculos agora. Tente novamente.',
          ),
        );
      },
    });
  }

  onVinculoKeydown(event: KeyboardEvent): void {
    if (!this.mostrarDropdownVinculo() || this.sugestoesVinculo.length === 0) {
      if (event.key === 'ArrowDown' && this.buscaVinculo.trim()) {
        event.preventDefault();
        this.abrirDropdownVinculo();
      }
      return;
    }
    const total = this.sugestoesVinculo.length;
    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        this.destaqueVinculo = (this.destaqueVinculo + 1) % total;
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.destaqueVinculo = (this.destaqueVinculo - 1 + total) % total;
        break;
      case 'Home':
        event.preventDefault();
        this.destaqueVinculo = 0;
        break;
      case 'End':
        event.preventDefault();
        this.destaqueVinculo = total - 1;
        break;
      case 'Enter': {
        event.preventDefault();
        const alvo =
          this.sugestoesVinculo[this.destaqueVinculo] ??
          this.sugestoesVinculo[0];
        if (alvo) {
          this.adicionarVinculo(alvo);
        }
        break;
      }
      case 'Escape':
        event.preventDefault();
        event.stopPropagation();
        this.mostrarDropdownVinculo.set(false);
        this.destaqueVinculo = -1;
        break;
    }
  }

  limparBuscaVinculo(): void {
    this.buscaVinculo = '';
    this.sugestoesVinculo = [];
    this.totalEncontradas = 0;
    this.mostrarDropdownVinculo.set(false);
    this.destaqueVinculo = -1;
  }

  getLogoUrl(codigoUnidade?: string): string {
    return codigoUnidade ? `/logos-fatec/${codigoUnidade}.png` : '';
  }

  onLogoError(codigoUnidade: string): void {
    this.logosComErro.add(codigoUnidade);
  }

  temLogoValida(codigoUnidade: string): boolean {
    return !this.logosComErro.has(codigoUnidade);
  }

  // --- Ações de identidade ---

  /** "Mudar Instituição": professor abre o modal de vínculos; aluno recebe aviso. */
  irParaInstituicoes(): void {
    if (!this.isProfessor()) {
      this.anunciarEmBreve('Mudar instituição');
      return;
    }
    this.abrirModalVinculos();
  }

  /** Abre o modal: foco entra no diálogo e o body perde a rolagem. */
  abrirModalVinculos(): void {
    this.modalVinculosAberto.set(true);
    if (
      typeof document !== 'undefined' &&
      document.activeElement instanceof HTMLElement
    ) {
      this.ultimoFocoAntesDoModal = document.activeElement;
    }
    this.aplicarTravaDeRolagemModal();
    // O @if só cria o nó na próxima detecção de mudanças: foco depois dela.
    setTimeout(() => this.modalVinculos?.nativeElement.focus(), 0);
  }

  /** Fecha sem salvar: a seleção local fica intacta para a próxima abertura. */
  fecharModalVinculos(): void {
    if (!this.modalVinculosAberto()) {
      return;
    }
    this.modalVinculosAberto.set(false);
    this.aplicarTravaDeRolagemModal();
    this.restaurarFocoModal();
  }

  private restaurarFocoModal(): void {
    const alvo = this.ultimoFocoAntesDoModal;
    this.ultimoFocoAntesDoModal = null;
    if (alvo && typeof document !== 'undefined' && document.contains(alvo)) {
      alvo.focus();
    }
  }

  private aplicarTravaDeRolagemModal(): void {
    if (typeof document === 'undefined') return;
    if (this.modalVinculosAberto()) {
      if (this.overflowAnteriorDoBodyModal === null) {
        this.overflowAnteriorDoBodyModal = document.body.style.overflow;
      }
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = this.overflowAnteriorDoBodyModal ?? '';
      this.overflowAnteriorDoBodyModal = null;
    }
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    this.fecharModalVinculos();
  }

  anunciarEmBreve(rotulo: string): void {
    this.avisoGeral.set(`${rotulo} estará disponível em breve.`);
    setTimeout(() => this.avisoGeral.set(null), 4000);
  }

  // --- Card ---

  /** Capa do projeto: absolutas/data/blob passam direto; "/uploads/x" vai para a API. */
  capaUrl(p: ProjetoResponseModel): string {
    const raw = p?.imagemCapaUrl ?? '';
    if (!raw || this.capasComErro.has(p.id)) return '';
    if (/^(https?:\/\/|data:|blob:)/i.test(raw)) return raw;
    if (raw.startsWith('/uploads/')) return `${environment.apiUrl}${raw}`;
    return raw;
  }

  onCapaErro(id: string): void {
    this.capasComErro.add(id);
  }

  /** "11/09/2026" a partir do ISO do `criadoEm`; string vazia quando inválido. */
  dataFormatada(iso?: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '';
    return d.toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  }

  private normalizar(texto: string): string {
    return (texto || '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .trim();
  }
}
