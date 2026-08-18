import { Routes } from '@angular/router';
import { PaginaInicial } from './features/pagina-inicial/pagina-inicial';
import { LoginInstituicao } from './features/login-instituicao/login-instituicao';


export const routes: Routes = [
    { path: '', component: PaginaInicial },
    { path: 'login-instituicao', component: LoginInstituicao },
];
