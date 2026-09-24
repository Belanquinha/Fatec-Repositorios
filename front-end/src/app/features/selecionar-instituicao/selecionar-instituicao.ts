import { Component, OnInit, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ProjetoService } from '../../core/services/projeto.service';
import { InstituicaoOption } from '../../core/models/projeto.model';

@Component({
  selector: 'app-selecionar-instituicao',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './selecionar-instituicao.html',
  styleUrl: './selecionar-instituicao.css',
})
export class SelecionarInstituicao implements OnInit {
  private projetoService = inject(ProjetoService);
  private router = inject(Router);

  @Input() isModal = false;
  @Input() instituicaoAtualId: string | null = null;
  @Output() instituicaoSelecionada = new EventEmitter<InstituicaoOption>();
  @Output() fecharModal = new EventEmitter<void>();

  instituicoes: InstituicaoOption[] = [];
  instituicoesFiltradas: InstituicaoOption[] = [];
  regioesDisponiveis: string[] = [];

  termoBusca = '';
  regiaoSelecionada = 'Todas';
  carregando = true;
  erroCarregamento: string | null = null;

  // Rastreia falhas no carregamento de logos para exibir fallback gracioso
  logosComErro: Set<string> = new Set();

  ngOnInit() {
    this.carregarInstituicoes();
  }

  carregarInstituicoes() {
    this.carregando = true;
    this.erroCarregamento = null;

    this.projetoService.listarInstituicoes().subscribe({
      next: (dados) => {
        this.instituicoes = (dados || []).filter((i) => i.ativo);
        this.extrairRegioes();
        this.aplicarFiltros();
        this.carregando = false;
      },
      error: (err) => {
        console.error('Erro ao carregar instituições:', err);
        this.erroCarregamento = 'Não foi possível carregar a lista de instituições. Verifique sua conexão.';
        this.carregando = false;
      },
    });
  }

  private extrairRegioes() {
    const regioesSet = new Set<string>();
    this.instituicoes.forEach((inst) => {
      if (inst.regiaoAdministrativa && inst.regiaoAdministrativa.trim()) {
        regioesSet.add(inst.regiaoAdministrativa.trim());
      }
    });
    this.regioesDisponiveis = ['Todas', ...Array.from(regioesSet).sort()];
  }

  aplicarFiltros() {
    const termoNormalizado = this.normalizarTexto(this.termoBusca);

    this.instituicoesFiltradas = this.instituicoes.filter((inst) => {
      // Filtro de região
      if (this.regiaoSelecionada !== 'Todas' && inst.regiaoAdministrativa !== this.regiaoSelecionada) {
        return false;
      }

      // Filtro de texto (nome, cidade, código, endereço)
      if (!termoNormalizado) return true;

      const nomeNorm = this.normalizarTexto(inst.nome);
      const cidadeNorm = this.normalizarTexto(inst.cidade || '');
      const codigoNorm = this.normalizarTexto(inst.codigoUnidade || '');
      const enderecoNorm = this.normalizarTexto(inst.endereco || '');

      return (
        nomeNorm.includes(termoNormalizado) ||
        cidadeNorm.includes(termoNormalizado) ||
        codigoNorm.includes(termoNormalizado) ||
        enderecoNorm.includes(termoNormalizado)
      );
    });
  }

  selecionarRegiao(regiao: string) {
    this.regiaoSelecionada = regiao;
    this.aplicarFiltros();
  }

  limparBusca() {
    this.termoBusca = '';
    this.aplicarFiltros();
  }

  selecionar(inst: InstituicaoOption) {
    if (this.isModal) {
      this.instituicaoSelecionada.emit(inst);
    } else {
      this.router.navigate(['/projeto-forms'], {
        queryParams: { instituicaoId: inst.id },
      });
    }
  }

  fechar() {
    this.fecharModal.emit();
  }

  onLogoError(codigoUnidade: string) {
    this.logosComErro.add(codigoUnidade);
  }

  temLogoValida(codigoUnidade: string): boolean {
    return !this.logosComErro.has(codigoUnidade);
  }

  getLogoUrl(codigoUnidade: string): string {
    return `/logos-fatec/${codigoUnidade}.png`;
  }

  /**
   * Extrai o nome principal da FATEC e o nome do patrono para visual hierárquico limpo
   */
  separarNomeEPatrono(nomeCompleto: string): { principal: string; patrono: string } {
    if (!nomeCompleto) return { principal: '', patrono: '' };
    const separadores = [' – ', ' - '];
    for (const sep of separadores) {
      if (nomeCompleto.includes(sep)) {
        const partes = nomeCompleto.split(sep);
        return {
          principal: partes[0].trim(),
          patrono: partes.slice(1).join(' - ').trim(),
        };
      }
    }
    return { principal: nomeCompleto.trim(), patrono: '' };
  }

  private normalizarTexto(texto: string): string {
    return (texto || '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .trim();
  }
}
