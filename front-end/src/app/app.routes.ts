import { Routes } from '@angular/router';
import { CatalogoPublico } from './features/catalogo-publico/catalogo-publico';
import { UploadProjeto } from './features/upload-projeto/upload-projeto';
import { VerProjeto } from './features/ver-projeto/ver-projeto';
import { Perfil } from './features/perfil/perfil';
import { authGuard } from './core/auth/auth.guard';
import { submissaoGuard } from './core/auth/submissao.guard';

export const routes: Routes = [
    { path: '', component: CatalogoPublico, pathMatch: 'full' },
    { path: 'catalogo-publico', component: CatalogoPublico },
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