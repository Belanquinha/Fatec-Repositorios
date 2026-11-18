import { Injectable, computed, signal } from '@angular/core';

import { CHAVE_TEMA_ARMAZENADO } from '../theme/theme.service';

/** Chave do localStorage onde o consentimento LGPD do usuário é persistido. */
export const CHAVE_CONSENTIMENTO_COOKIES = 'fatec-lgpd-cookies';

/** Registro de consentimento conforme a LGPD (Lei nº 13.709/2018). */
export interface ConsentimentoCookies {
  versao: 1;
  essenciais: true;
  preferencias: boolean;
  analiticos: boolean;
  atualizadoEm: string;
}

export interface OpcoesConsentimentoCookies {
  preferencias: boolean;
  analiticos: boolean;
}

/**
 * Gerencia o aviso e as preferências de cookies/armazenamento local (LGPD).
 *
 * - Exibe o banner inicial enquanto não houver manifestação registrada.
 * - Segue privacidade por padrão (opt-in): categorias opcionais iniciam desmarcadas
 *   na personalização até escolha explícita do titular.
 * - Ao recusar cookies de preferências, limpa chaves opcionais persistidas (`fatec-tema`).
 */
@Injectable({ providedIn: 'root' })
export class CookieConsentService {
  private readonly consentimentoAtual = signal<ConsentimentoCookies | null>(null);
  private readonly modalAberto = signal(false);

  /** Consentimento salvo atualmente (`null` enquanto o usuário não respondeu). */
  readonly consentimento = this.consentimentoAtual.asReadonly();
  /** Indica se o diálogo detalhado de preferências está aberto. */
  readonly preferenciasAbertas = this.modalAberto.asReadonly();
  /** Exibe a barra inferior apenas quando ainda não há decisão e o modal está fechado. */
  readonly bannerVisivel = computed(
    () => this.consentimentoAtual() === null && !this.modalAberto(),
  );

  constructor() {
    this.consentimentoAtual.set(this.lerConsentimentoSalvo());
  }

  /** Aceita todas as categorias de cookies e encerra o aviso. */
  aceitarTodos(): void {
    this.registrar({ preferencias: true, analiticos: true });
  }

  /** Mantém apenas os cookies estritamente necessários (rejeita opcionais). */
  rejeitarOpcionais(): void {
    this.registrar({ preferencias: false, analiticos: false });
  }

  /** Salva a combinação personalizada escolhida pelo titular. */
  salvarPreferencias(opcoes: OpcoesConsentimentoCookies): void {
    this.registrar({
      preferencias: Boolean(opcoes.preferencias),
      analiticos: Boolean(opcoes.analiticos),
    });
  }

  /** Abre o painel de gerenciamento granular (acessível pelo banner ou rodapé). */
  abrirPreferencias(): void {
    this.modalAberto.set(true);
  }

  /** Fecha o painel de gerenciamento granular. */
  fecharPreferencias(): void {
    this.modalAberto.set(false);
  }

  private registrar(opcoes: OpcoesConsentimentoCookies): void {
    const novo: ConsentimentoCookies = {
      versao: 1,
      essenciais: true,
      preferencias: opcoes.preferencias,
      analiticos: opcoes.analiticos,
      atualizadoEm: new Date().toISOString(),
    };
    this.consentimentoAtual.set(novo);
    this.modalAberto.set(false);
    this.persistir(novo);

    if (!novo.preferencias) {
      this.limparArmazenamentoOpcional();
    }
  }

  private lerConsentimentoSalvo(): ConsentimentoCookies | null {
    try {
      if (typeof localStorage === 'undefined') return null;
      const bruto = localStorage.getItem(CHAVE_CONSENTIMENTO_COOKIES);
      if (!bruto) return null;
      const parsed = JSON.parse(bruto) as Partial<ConsentimentoCookies>;
      if (
        parsed &&
        parsed.versao === 1 &&
        parsed.essenciais === true &&
        typeof parsed.preferencias === 'boolean' &&
        typeof parsed.analiticos === 'boolean' &&
        typeof parsed.atualizadoEm === 'string'
      ) {
        return parsed as ConsentimentoCookies;
      }
      return null;
    } catch {
      return null;
    }
  }

  private persistir(consentimento: ConsentimentoCookies): void {
    try {
      if (typeof localStorage !== 'undefined') {
        localStorage.setItem(CHAVE_CONSENTIMENTO_COOKIES, JSON.stringify(consentimento));
      }
    } catch {
      // Armazenamento bloqueado no navegador: mantém a decisão em memória na sessão.
    }
  }

  private limparArmazenamentoOpcional(): void {
    try {
      if (typeof localStorage !== 'undefined') {
        localStorage.removeItem(CHAVE_TEMA_ARMAZENADO);
      }
    } catch {
      // Ignora caso localStorage esteja indisponível.
    }
  }
}
