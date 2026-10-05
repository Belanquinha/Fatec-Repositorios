import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { UsuarioLogado } from '../../core/auth/models/usuario-logado';
import { ProjetoService } from '../../core/services/projeto.service';

/**
 * Tela de Perfil (DESIGN 4.6) — v1: identidade + contexto do papel, leitura.
 *
 * A identidade (nome, e-mail, foto, papel) é somente leitura e vem da sessão
 * guardada no navegador, que por sua vez veio do back-end no login. O contexto
 * do papel é carregado da API: o aluno vê o contador dos próprios projetos
 * (`GET /projetos/meus`); o professor vê o estado de vínculos — que hoje é
 * sempre vazio, porque a relação professor↔instituição ainda não existe no
 * back-end. Quando as rotas `area-professor` e `area-professor/fila` existirem,
 * os CTAs da seção do professor passam a navegar para elas (ver TODO no HTML).
 */
@Component({
  selector: 'app-perfil',
  standalone: true,
  imports: [CommonModule, RouterModule],
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

    // Só o aluno tem contexto vindo da API nesta v1: o professor ainda não tem
    // endpoint de vínculos, então a seção dele é o estado vazio do DESIGN 4.6.
    if (logado.role === 'ALUNO') {
      this.carregarContextoAluno();
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
}
