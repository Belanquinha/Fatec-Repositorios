import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { environment } from '../../../environments/environment';
import { ProjetoService } from '../../core/services/projeto.service';
import { ProjetoResponseModel } from '../../core/models/projeto.model';

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
  styleUrl: './ver-projeto.css',
})
export class VerProjeto implements OnInit {
  private route = inject(ActivatedRoute);
  private projetoService = inject(ProjetoService);

  projeto = signal<ProjetoResponseModel | null>(null);
  carregando = signal(true);
  erroCarregamento = signal<string | null>(null);
  projetoNaoEncontrado = signal(false);
  capaComErro = signal(false);

  capaUrl = computed(() => {
    const raw = this.projeto()?.imagemCapaUrl ?? '';
    if (!raw || this.capaComErro()) return '';
    if (/^(https?:\/\/|data:|blob:)/i.test(raw)) return raw;
    if (raw.startsWith('/uploads/')) return `${environment.apiUrl}${raw}`;
    return raw;
  });

  tags = computed(() => this.projeto()?.palavrasChave ?? []);

  temRepositorio = computed(() => !!this.projeto()?.linkRepositorio?.trim());

  motivoRejeicao = computed(() => this.projeto()?.motivoRejeicao?.trim() ?? '');

  mostraMotivoRejeicao = computed(
    () => this.projeto()?.estado === 'REJEITADO' && this.motivoRejeicao().length > 0
  );

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
            'Não foi possível carregar os detalhes do projeto no momento. Verifique sua conexão e tente novamente.');
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
    return d.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' });
  }
}
