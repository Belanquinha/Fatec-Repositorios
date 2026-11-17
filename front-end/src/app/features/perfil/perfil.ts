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
import { Router, RouterModule } from '@angular/router';
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
 * Tela de Perfil (DESIGN 4.6 + UX-SELECAO-INSTITUICOES-PROFESSOR Frame B).
 *
 * Identidade (nome, e-mail, foto, papel) é somente leitura, vinda da sessão.
 * Contexto do papel vem da API: aluno → contador de `GET /projetos/meus`;
 * professor → aba "Instituições (0–4)" com `institution-autocomplete
 * mode=multi`: digita-para-revelar, chips locais, contador por extenso,
 * recusa inline da 5ª em cinza (nunca vermelho CPS) e Save explícito via
 * `PUT /professores/me/instituicoes` (0–4, idempotente).
 */
@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './perfil.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './perfil.css',
})
export class Perfil implements OnInit {
  private authService = inject(AuthService);
  private projetoService = inject(ProjetoService);
  private router = inject(Router);

  usuario = signal<UsuarioLogado | null>(null);
  carregando = signal(true);
  erroContexto = signal<string | null>(null);
  totalProjetos = signal<number | null>(null);

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

  inicial = computed(() => {
    const nome = this.usuario()?.nome?.trim();
    return nome ? nome.charAt(0).toUpperCase() : '?';
  });

  rotuloPapel = computed(() => {
    switch (this.usuario()?.role) {
      case 'ALUNO':
        return 'Aluno';
      case 'PROFESSOR':
        return 'Professor';
      case 'ADMIN':
        return 'Administrador';
      default:
        return 'Usuário';
    }
  });

  async ngOnInit(): Promise<void> {
    const logado = await this.authService.obterUsuarioLogado();
    if (!logado) {
      // O guard barra a rota, mas a sessão pode vencer com a tela aberta.
      await this.router.navigateByUrl('/');
      return;
    }
    this.usuario.set(logado);

    if (logado.role === 'ALUNO') {
      this.carregarContextoAluno();
    } else if (logado.role === 'PROFESSOR') {
      this.carregarContextoProfessor();
    } else {
      this.carregando.set(false);
    }
  }

  private carregarContextoAluno(): void {
    this.projetoService.obterMeusProjetos().subscribe({
      next: (projetos) => {
        this.totalProjetos.set((projetos ?? []).length);
        this.carregando.set(false);
      },
      error: (err) => {
        console.error('Erro ao carregar projetos do perfil:', err);
        // Identidade do token continua exibida: o que falhou foi o contexto,
        // não a sessão (DESIGN 4.6, estado de erro).
        this.erroContexto.set(
          'Não foi possível carregar seus projetos agora. Tente novamente mais tarde.',
        );
        this.carregando.set(false);
      },
    });
  }

  private carregarContextoProfessor(): void {
    this.carregarVinculos();
    this.carregarPendentes();
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

  // --- Card de pendente (mesma geometria do card do catálogo) ---

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
