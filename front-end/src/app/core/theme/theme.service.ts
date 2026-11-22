import { Injectable, computed, signal } from '@angular/core';

/** Tema visual da aplicação. */
export type Tema = 'claro' | 'escuro';
export type ModoTema = 'claro' | 'escuro' | 'sistema';

/** Chave do localStorage onde a escolha do usuário é persistida. */
export const CHAVE_TEMA_ARMAZENADO = 'fatec-tema';

/**
 * Gerencia o modo escuro da aplicação.
 *
 * - Prioridade: escolha salva em `localStorage` > `prefers-color-scheme` > claro.
 * - Aplica/remove a classe `dark` em `<html>` (estratégia `darkMode: 'class'` do Tailwind
 *   + remapeamento das variáveis de `paleta.css` via `.dark`).
 * - Sem escolha salva, acompanha mudanças do tema do sistema operacional em runtime.
 * - Todo acesso a `window`/`document`/`localStorage` é protegido para não quebrar SSR/testes.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly temaAtual = signal<Tema>('claro');
  private readonly modoAtual = signal<ModoTema>('sistema');
  private escolhaManual = false;
  private observadorSistema: MediaQueryList | null = null;
  private aoMudarSistema = (_evento: MediaQueryListEvent): void => {
    if (!this.escolhaManual) {
      this.aplicar(this.observadorSistema?.matches ? 'escuro' : 'claro');
    }
  };

  /** Tema atual (somente leitura: claro ou escuro). */
  readonly tema = this.temaAtual.asReadonly();
  /** Modo de preferência atual: 'claro', 'escuro' ou 'sistema'. */
  readonly modo = this.modoAtual.asReadonly();
  /** `true` quando o modo escuro está ativo. */
  readonly modoEscuro = computed(() => this.temaAtual() === 'escuro');

  constructor() {
    const salvo = this.lerTemaSalvo();
    this.escolhaManual = salvo !== null;
    this.modoAtual.set(salvo ?? 'sistema');
    const inicial = salvo ?? this.lerTemaDoSistema() ?? 'claro';
    this.aplicar(inicial);
    this.observarSistema();
  }

  /** Alterna entre claro e escuro e persiste a escolha do usuário. */
  alternar(): void {
    this.definir(this.temaAtual() === 'escuro' ? 'claro' : 'escuro');
  }

  /** Define um tema específico ('claro' ou 'escuro') e persiste a escolha do usuário. */
  definir(tema: Tema): void {
    this.escolhaManual = true;
    this.modoAtual.set(tema);
    this.aplicar(tema);
    this.salvarTema(tema);
  }

  /** Define o modo ('claro', 'escuro' ou 'sistema'). */
  definirModo(modo: ModoTema): void {
    if (modo === 'sistema') {
      this.usarSistema();
    } else {
      this.definir(modo);
    }
  }

  /** Retorna a configuração de tema para acompanhar o sistema operacional. */
  usarSistema(): void {
    this.escolhaManual = false;
    this.modoAtual.set('sistema');
    try {
      if (typeof localStorage !== 'undefined') {
        localStorage.removeItem(CHAVE_TEMA_ARMAZENADO);
      }
    } catch {
      // Ignora erro de acesso a localStorage
    }
    const doSistema = this.lerTemaDoSistema() ?? 'claro';
    this.aplicar(doSistema);
  }

  private aplicar(tema: Tema): void {
    this.temaAtual.set(tema);
    if (typeof document === 'undefined') return;
    document.documentElement.classList.toggle('dark', tema === 'escuro');
  }

  private lerTemaInicial(): Tema {
    return this.lerTemaSalvo() ?? this.lerTemaDoSistema() ?? 'claro';
  }

  private lerTemaSalvo(): Tema | null {
    try {
      const salvo = typeof localStorage !== 'undefined' ? localStorage.getItem(CHAVE_TEMA_ARMAZENADO) : null;
      return salvo === 'claro' || salvo === 'escuro' ? salvo : null;
    } catch {
      return null;
    }
  }

  private lerTemaDoSistema(): Tema | null {
    const preferencia = this.consultarMidiaSistema();
    if (!preferencia) return null;
    return preferencia.matches ? 'escuro' : 'claro';
  }

  private consultarMidiaSistema(): MediaQueryList | null {
    try {
      if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return null;
      return window.matchMedia('(prefers-color-scheme: dark)');
    } catch {
      return null;
    }
  }

  private observarSistema(): void {
    const midia = this.consultarMidiaSistema();
    if (!midia) return;
    this.observadorSistema = midia;
    try {
      midia.addEventListener('change', this.aoMudarSistema);
    } catch {
      // Navegadores antigos sem addEventListener em MediaQueryList: ignora.
    }
  }

  private salvarTema(tema: Tema): void {
    try {
      if (typeof localStorage !== 'undefined') {
        if (!this.podePersistirPreferencia()) {
          localStorage.removeItem(CHAVE_TEMA_ARMAZENADO);
          return;
        }
        localStorage.setItem(CHAVE_TEMA_ARMAZENADO, tema);
      }
    } catch {
      // Armazenamento indisponível (modo privado etc.): o tema da sessão continua valendo.
    }
  }

  private podePersistirPreferencia(): boolean {
    try {
      const bruto = localStorage.getItem('fatec-lgpd-cookies');
      if (!bruto) return true;
      const parsed = JSON.parse(bruto) as { preferencias?: unknown };
      return parsed?.preferencias !== false;
    } catch {
      return true;
    }
  }
}
