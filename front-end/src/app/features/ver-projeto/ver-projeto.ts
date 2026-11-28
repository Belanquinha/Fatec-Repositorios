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
import { environment } from '../../../environments/environment';
import { ProjetoService } from '../../core/services/projeto.service';
import { ProjetoResponseModel } from '../../core/models/projeto.model';
import { AuthService } from '../../core/auth/auth.service';
import { UsuarioLogado } from '../../core/auth/models/usuario-logado';
import { mensagemErroAmigavel } from '../../core/utils/erro-http';

/** Bloco mínimo do OutputData do Editor.js — só o que a tela precisa renderizar. */
export interface BlocoConteudo {
  type: string;
  data: any;
}

@Component({
  selector: 'app-ver-projeto',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './ver-projeto.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './ver-projeto.css',
})
export class VerProjeto implements OnInit {
  private route = inject(ActivatedRoute);
  private projetoService = inject(ProjetoService);
  private authService = inject(AuthService);

  projeto = signal<ProjetoResponseModel | null>(null);
  carregando = signal(true);
  erroCarregamento = signal<string | null>(null);
  projetoNaoEncontrado = signal(false);
  capaComErro = signal(false);

  // --- Avaliação (professor responsável abrindo pelo card do /perfil) ---
  usuario = signal<UsuarioLogado | null>(null);
  /** Ligado pelo `?avaliar=1` do card de pendente; sem ele o painel não aparece. */
  modoAvaliacao = signal(false);
  erroAvaliacao = signal<string | null>(null);
  sucessoAvaliacao = signal<string | null>(null);
  processandoAcao = signal(false);
  rejeicaoAberta = signal(false);
  motivoRejeicaoCampo = signal('');

  // --- Devolução para alteração (fase 2a, §4.3 / CAP-D1) ---
  devolucaoAberta = signal(false);
  motivoDevolucaoCampo = signal('');

  capaUrl = computed(() => {
    const raw = this.projeto()?.imagemCapaUrl ?? '';
    if (!raw || this.capaComErro()) return '';
    if (/^(https?:\/\/|data:|blob:)/i.test(raw)) return raw;
    if (raw.startsWith('/uploads/')) return `${environment.apiUrl}${raw}`;
    return raw;
  });

  tags = computed(() => this.projeto()?.palavrasChave ?? []);

  temRepositorio = computed(() => !!this.projeto()?.linkRepositorio?.trim());
  ehJogo = computed(() => this.projeto()?.tipoProjeto === 'JOGO' && !!this.projeto()?.temJogoExecutavel);
  tamanhoJogoFormatado = computed(() => {
    const bytes = this.projeto()?.arquivoJogoTamanhoBytes;
    if (!bytes) return '';
    return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  });

  // --- Execução de Jogo via Fatec Launcher ---
  modalSegurancaAberto = signal(false);
  modalLauncherAberto = signal(false);
  gerandoSessaoJogo = signal(false);
  erroSessaoJogo = signal<string | null>(null);
  ultimoUriLauncher = signal<string | null>(null);

  isWindowsDesktop(): boolean {
    if (typeof navigator === 'undefined') return true;
    const ua = navigator.userAgent || '';
    const isMobile = /Android|iPhone|iPad|iPod|Mobile/i.test(ua);
    const isWindows = /Windows/i.test(ua);
    return isWindows && !isMobile;
  }

  abrirAvisoSegurancaJogo(): void {
    this.erroSessaoJogo.set(null);
    this.modalSegurancaAberto.set(true);
  }

  fecharAvisoSegurancaJogo(): void {
    this.modalSegurancaAberto.set(false);
  }

  confirmarJogar(): void {
    const id = this.projeto()?.id;
    if (!id) return;
    this.modalSegurancaAberto.set(false);
    this.gerandoSessaoJogo.set(true);
    this.erroSessaoJogo.set(null);

    this.projetoService.criarSessaoJogo(id).subscribe({
      next: (sessao) => {
        this.gerandoSessaoJogo.set(false);
        const apiBase = this.resolverApiAbsoluta();
        const uri = `fatecrepo://play?api=${encodeURIComponent(apiBase)}&ticket=${encodeURIComponent(sessao.ticket)}`;
        this.ultimoUriLauncher.set(uri);
        this.modalLauncherAberto.set(true);
        this.dispararProtocolo(uri);
      },
      error: (err) => {
        this.gerandoSessaoJogo.set(false);
        this.erroSessaoJogo.set(mensagemErroAmigavel(err, 'Não foi possível iniciar a sessão do jogo.'));
      },
    });
  }

  tentarAbrirLauncherNovamente(): void {
    const uri = this.ultimoUriLauncher();
    if (uri) {
      this.dispararProtocolo(uri);
    }
  }

  fecharModalLauncher(): void {
    this.modalLauncherAberto.set(false);
  }

  private resolverApiAbsoluta(): string {
    if (/^https?:\/\//i.test(environment.apiUrl)) {
      return environment.apiUrl;
    }
    const origin = typeof window !== 'undefined' ? window.location.origin : 'http://localhost:4200';
    return `${origin}${environment.apiUrl.startsWith('/') ? '' : '/'}${environment.apiUrl}`;
  }

  private dispararProtocolo(uri: string): void {
    if (typeof window !== 'undefined') {
      window.location.href = uri;
    }
  }

  motivoRejeicao = computed(() => this.projeto()?.motivoRejeicao?.trim() ?? '');

  mostraMotivoRejeicao = computed(
    () =>
      this.visaoAvaliacao() &&
      this.projeto()?.estado === 'REJEITADO' &&
      this.motivoRejeicao().length > 0,
  );

  /** Justificativa da devolução: visível ao autor (e ao professor na revisão). */
  motivoDevolucao = computed(() => this.projeto()?.motivoDevolucao?.trim() ?? '');

  mostraMotivoDevolucao = computed(() => this.motivoDevolucao().length > 0);

  /**
   * O card de pendente do /perfil abre esta tela com `?avaliar=1`. Só nesse modo,
   * para um professor logado e com o projeto ainda pendente, o painel de decisão
   * aparece no fim da página. O backend continua sendo a autoridade: PATCH recusa
   * com 403 quem não for o professor responsável indicado.
   */
  podeAvaliar = computed(
    () =>
      this.modoAvaliacao() &&
      this.usuario()?.role === 'PROFESSOR' &&
      (this.projeto()?.estado === 'AGUARDANDO_APROVACAO' ||
        this.projeto()?.estado === 'REENVIADO'),
  );

  /**
   * Visão de avaliação: professor logado lendo um projeto pendente/rejeitado.
   *
   * <p>É o que separa "aluno acompanhando o próprio envio" de "professor revisando".
   * O back-end já nega o detalhe de não-aprovado para quem não é autor nem professor
   * responsável (404), então este computed é a contraparte de apresentação: o motivo
   * da rejeição só é exibido para o professor, nunca para o autor nem para visitante.
   */
  visaoAvaliacao = computed(() => this.usuario()?.role === 'PROFESSOR');

  blocos = computed<BlocoConteudo[]>(() => {
    const raw = this.projeto()?.conteudoEditorJs?.trim() ?? '';
    if (!raw) return [];
    try {
      const parsed = JSON.parse(raw);
      const lista = Array.isArray(parsed) ? parsed : parsed?.blocks;
      if (!Array.isArray(lista)) return [];
      return lista.filter((b) => b && typeof b.type === 'string');
    } catch {
      return [];
    }
  });

  conteudoTextoPuro = computed(() => {
    const raw = this.projeto()?.conteudoEditorJs?.trim() ?? '';
    if (!raw || this.blocos().length > 0) return '';
    if (raw.startsWith('{') || raw.startsWith('[')) return '';
    return raw;
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    this.modoAvaliacao.set(
      this.route.snapshot.queryParamMap.get('avaliar') === '1',
    );
    this.carregarUsuario();
    if (!id) {
      this.carregando.set(false);
      this.projetoNaoEncontrado.set(true);
      return;
    }
    this.carregarProjeto(id);
  }

  carregarProjeto(id: string): void {
    this.carregando.set(true);
    this.erroCarregamento.set(null);
    this.projetoNaoEncontrado.set(false);
    this.capaComErro.set(false);

    this.projetoService.obterProjetoPorId(id).subscribe({
      next: (proj) => {
        this.projeto.set(proj);
        this.carregando.set(false);
      },
      error: (err) => {
        console.error('Erro ao carregar projeto:', err);
        this.carregando.set(false);
        if (err?.status === 404) {
          this.projetoNaoEncontrado.set(true);
        } else {
          this.erroCarregamento.set(
            'Não foi possível carregar os detalhes do projeto no momento. Verifique sua conexão e tente novamente.',
          );
        }
      },
    });
  }

  tentarNovamente(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.carregarProjeto(id);
  }

  onCapaError(): void {
    this.capaComErro.set(true);
  }

  private carregarUsuario(): void {
    this.authService
      .obterUsuarioLogado()
      .then((u) => this.usuario.set(u))
      .catch(() => this.usuario.set(null));
  }

  aprovar(): void {
    const id = this.projeto()?.id;
    if (!id || this.processandoAcao()) {
      return;
    }
    this.processandoAcao.set(true);
    this.erroAvaliacao.set(null);
    this.sucessoAvaliacao.set(null);
    this.projetoService.aprovarProjeto(id).subscribe({
      next: (aprovado) => {
        this.projeto.set(aprovado);
        this.processandoAcao.set(false);
        this.rejeicaoAberta.set(false);
        this.devolucaoAberta.set(false);
        this.sucessoAvaliacao.set(
          `Projeto "${aprovado.titulo}" aprovado e publicado na vitrine.`,
        );
      },
      error: (err) => {
        console.error('Erro ao aprovar projeto:', err);
        this.processandoAcao.set(false);
        this.erroAvaliacao.set(
          mensagemErroAmigavel(
            err,
            'Não foi possível aprovar o projeto agora. Tente novamente.',
          ),
        );
      },
    });
  }

  abrirRejeicao(): void {
    this.rejeicaoAberta.set(true);
    this.erroAvaliacao.set(null);
    this.sucessoAvaliacao.set(null);
  }

  fecharRejeicao(): void {
    this.rejeicaoAberta.set(false);
    this.motivoRejeicaoCampo.set('');
  }

  confirmarRejeicao(): void {
    const id = this.projeto()?.id;
    const motivo = this.motivoRejeicaoCampo().trim();
    if (!id || this.processandoAcao()) {
      return;
    }
    if (!motivo) {
      this.erroAvaliacao.set('Informe o motivo da rejeição para o aluno.');
      return;
    }
    this.processandoAcao.set(true);
    this.erroAvaliacao.set(null);
    this.sucessoAvaliacao.set(null);
    this.projetoService.rejeitarProjeto(id, motivo).subscribe({
      next: (rejeitado) => {
        this.projeto.set(rejeitado);
        this.processandoAcao.set(false);
        this.rejeicaoAberta.set(false);
        this.devolucaoAberta.set(false);
        this.motivoRejeicaoCampo.set('');
        this.sucessoAvaliacao.set(
          `Projeto "${rejeitado.titulo}" rejeitado com retorno ao aluno.`,
        );
      },
      error: (err) => {
        console.error('Erro ao rejeitar projeto:', err);
        this.processandoAcao.set(false);
        this.erroAvaliacao.set(
          mensagemErroAmigavel(
            err,
            'Não foi possível rejeitar o projeto agora. Tente novamente.',
          ),
        );
      },
    });
  }

  abrirDevolucao(): void {
    this.devolucaoAberta.set(true);
    this.erroAvaliacao.set(null);
    this.sucessoAvaliacao.set(null);
  }

  fecharDevolucao(): void {
    this.devolucaoAberta.set(false);
    this.motivoDevolucaoCampo.set('');
  }

  /** Devolve com justificativa para ALTERACOES_SOLICITADAS (o autor edita e reenvia). */
  confirmarDevolucao(): void {
    const id = this.projeto()?.id;
    const motivo = this.motivoDevolucaoCampo().trim();
    if (!id || this.processandoAcao()) {
      return;
    }
    if (!motivo) {
      this.erroAvaliacao.set('Informe o motivo da devolução para o aluno.');
      return;
    }
    this.processandoAcao.set(true);
    this.erroAvaliacao.set(null);
    this.sucessoAvaliacao.set(null);
    this.projetoService.devolverProjeto(id, { motivo }).subscribe({
      next: (devolvido) => {
        this.projeto.set(devolvido);
        this.processandoAcao.set(false);
        this.devolucaoAberta.set(false);
        this.motivoDevolucaoCampo.set('');
        this.sucessoAvaliacao.set(
          `Projeto "${devolvido.titulo}" devolvido para alteração pelo autor.`,
        );
      },
      error: (err) => {
        console.error('Erro ao devolver projeto:', err);
        this.processandoAcao.set(false);
        this.erroAvaliacao.set(
          mensagemErroAmigavel(
            err,
            'Não foi possível devolver o projeto agora. Tente novamente.',
          ),
        );
      },
    });
  }

  /** Itens de um bloco `list` — o Editor.js pode entregar strings ou objetos. */
  itensLista(bloco: BlocoConteudo): string[] {
    const items = bloco?.data?.items ?? [];
    if (!Array.isArray(items)) return [];
    return items.map((item) => {
      if (typeof item === 'string') return item;
      if (item && typeof item === 'object') {
        return String(item.content ?? item.text ?? '');
      }
      return String(item ?? '');
    });
  }

  listaOrdenada(bloco: BlocoConteudo): boolean {
    return bloco?.data?.style === 'ordered';
  }

  urlImagemBloco(bloco: BlocoConteudo): string {
    const url: string = bloco?.data?.file?.url ?? bloco?.data?.url ?? '';
    if (!url) return '';
    if (/^(https?:\/\/|data:|blob:)/i.test(url)) return url;
    if (url.startsWith('/uploads/')) return `${environment.apiUrl}${url}`;
    return url;
  }

  legendaImagemBloco(bloco: BlocoConteudo): string {
    return String(bloco?.data?.caption ?? '');
  }

  dataFormatada(iso?: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '';
    return d.toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });
  }
}
