import { Routes } from '@angular/router';
import { CatalogoPublico } from './features/catalogo-publico/catalogo-publico';
import { UploadProjeto } from './features/upload-projeto/upload-projeto';
import { VerProjeto } from './features/ver-projeto/ver-projeto';
import { Perfil } from './features/perfil/perfil';
import { Autenticacao } from './features/autenticacao/autenticacao';
import { Instituicoes } from './features/instituicoes/instituicoes';
import { PerfilInstituicao } from './features/perfil-instituicao/perfil-instituicao';
import { PerfilPublico } from './features/perfil-publico/perfil-publico';
import { authGuard } from './core/auth/auth.guard';
import { submissaoGuard } from './core/auth/submissao.guard';
import { visitanteGuard } from './core/auth/visitante.guard';

export const routes: Routes = [
    { path: '', component: CatalogoPublico, pathMatch: 'full' },
    { path: 'catalogo-publico', component: CatalogoPublico },
    { path: 'instituicoes', component: Instituicoes },
    { path: 'instituicoes/:id', component: PerfilInstituicao },
    { path: 'usuarios/:id', component: PerfilPublico },
    { path: 'entrar', component: Autenticacao, canActivate: [visitanteGuard] },
    { path: 'login', redirectTo: 'entrar', pathMatch: 'full' },
    { path: 'entra', redirectTo: 'entrar', pathMatch: 'full' },
    // Detalhe fica público: o catálogo (visitante) linka para projetos aprovados.
    // Projeto pendente/rejeitado só abre para o autor ou o professor responsável
    // — quem decide isso é o back-end (GET /projetos/{id} devolve 404 para quem
    // não pode ver).
    { path: 'ver-projeto/:id', component: VerProjeto },
    { path: 'ver-projeto', redirectTo: 'catalogo-publico', pathMatch: 'full' },
    { path: 'projeto-forms', component: UploadProjeto, canActivate: [authGuard, submissaoGuard] },
    { path: 'perfil', component: Perfil, canActivate: [authGuard] },
    { path: '**', redirectTo: '' },
];
