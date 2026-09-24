import { Routes } from '@angular/router';
import { CatalogoPublico } from './features/catalogo-publico/catalogo-publico';
import { UploadProjeto } from './features/upload-projeto/upload-projeto';
import { SelecionarInstituicao } from './features/selecionar-instituicao/selecionar-instituicao';

export const routes: Routes = [
    { path: '', component: CatalogoPublico, pathMatch: 'full' },
    { path: 'catalogo-publico', component: CatalogoPublico },
    { path: 'selecionar-instituicao', component: SelecionarInstituicao },
    { path: 'projeto-forms', component: UploadProjeto },
    { path: '**', redirectTo: '' },
];