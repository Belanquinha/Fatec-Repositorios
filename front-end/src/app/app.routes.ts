import { Routes } from '@angular/router';
import { CatalogoPublico } from './features/catalogo-publico/catalogo-publico';
import { UploadProjeto } from './features/upload-projeto/upload-projeto';
import { SelecionarInstituicao } from './features/selecionar-instituicao/selecionar-instituicao';
import { VerProjeto } from './features/ver-projeto/ver-projeto';

export const routes: Routes = [
    { path: '', component: CatalogoPublico, pathMatch: 'full' },
    { path: 'catalogo-publico', component: CatalogoPublico },
    { path: 'ver-projeto/:id', component: VerProjeto },
    { path: 'ver-projeto', redirectTo: 'catalogo-publico', pathMatch: 'full' },
    { path: 'projeto-forms', component: UploadProjeto },
    { path: '**', redirectTo: '' },
];