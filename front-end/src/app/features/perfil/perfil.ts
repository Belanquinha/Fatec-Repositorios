import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../core/auth/auth.service';
import { UsuarioLogado } from '../../core/auth/models/usuario-logado';
import { ProjetoService } from '../../core/services/projeto.service';
import { InstituicaoOption } from '../../core/models/projeto.model';

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

  contadorVinculos = computed(() => `${this.selecao().length} de ${this.LIMITE_VINCULOS} unidades`);
  noTeto = computed(() => this.selecao().length >= this.LIMITE_VINCULOS);
  temAlteracoes = computed(() => {
    const salvos = this.vinculos().map((v) => v.id).sort();
    const atuais = this.selecao().map((v) => v.id).sort();
    return (
      salvos.length !== atuais.length || salvos.some((id, i) => id !== atuais[i])
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
        this.erroContexto.set('Não foi possível carregar seus projetos agora. Tente novamente mais tarde.');
        this.carregando.set(false);
      },
    });
  }

  private carregarContextoProfessor(): void {
    this.carregandoVinculos.set(true);
    this.erroVinculos.set(null);
    forkJoin({
      catalogo: this.projetoService.listarInstituicoes().pipe(catchError(() => of(null))),
      vinculos: this.projetoService.obterMeusVinculos().pipe(catchError(() => of(null))),
    }).subscribe(({ catalogo, vinculos }) => {
      if (catalogo === null || vinculos === null) {
        this.erroVinculos.set(
          'Não foi possível carregar suas instituições agora. Verifique sua conexão.'
        );
        this.catalogo.set(catalogo ?? []);
        this.vinculos.set([]);
        this.selecao.set([]);
      } else {
        const ativas = (catalogo ?? []).filter((i) => i.ativo);
        this.catalogo.set(ativas);
        // Garante ordem por nome como o servidor promete.
        const ordenados = [...(vinculos ?? [])].sort((a, b) => a.nome.localeCompare(b.nome, 'pt-BR'));
        this.vinculos.set(ordenados);
        this.selecao.set([...ordenados]);
      }
      this.carregandoVinculos.set(false);
      this.carregando.set(false);
    });
  }

  tentarNovamenteVinculos(): void {
    this.carregarContextoProfessor();
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
          a.nome.localeCompare(b.nome, 'pt-BR')
        );
        this.vinculos.set(ordenados);
        this.selecao.set([...ordenados]);
        this.salvandoVinculos.set(false);
        this.sucessoVinculos.set(
          ordenados.length === 0
            ? 'Vínculos atualizados. Você está sem instituições vinculadas.'
            : `Vínculos atualizados: ${ordenados.length} de ${this.LIMITE_VINCULOS} unidades.`
        );
      },
      error: (err) => {
        console.error('Erro ao salvar vínculos do professor:', err);
        this.salvandoVinculos.set(false);
        // Mantém chips e contagem; mostra o envelope do servidor em painel neutro.
        const mensagem =
          err?.error?.mensagem ||
          err?.error?.message ||
          'Não foi possível salvar os vínculos agora. Tente novamente.';
        this.erroSalvarVinculos.set(mensagem);
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
          this.sugestoesVinculo[this.destaqueVinculo] ?? this.sugestoesVinculo[0];
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

  private normalizar(texto: string): string {
    return (texto || '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .trim();
  }
}
