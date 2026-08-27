import { Routes } from '@angular/router';
import { PaginaInicial } from './features/pagina-inicial/pagina-inicial';
import { LoginInstituicao } from './features/login-instituicao/login-instituicao';
import { MainAdmin } from './features/admin/main-admin/main-admin';
import { CadastroInstituicao } from './features/admin/cadastro-instituicao/cadastro-instituicao';


export const routes: Routes = [
    { path: '', component: PaginaInicial },
    { path: 'login-instituicao', component: LoginInstituicao },
    { path: 'admin-main', component: MainAdmin },
    { path: 'admin-cadastro-instituicao', component: CadastroInstituicao },
];
