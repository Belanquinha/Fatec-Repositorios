import { Routes } from '@angular/router';
import { PaginaInicial } from './features/pagina-inicial/pagina-inicial';
import { LoginInstituicao } from './features/login-instituicao/login-instituicao';
import { MainAdmin } from './features/admin/main-admin/main-admin';
import { CadastroInstituicao } from './features/admin/cadastro-instituicao/cadastro-instituicao';
import { VerProjeto } from './features/ver-projeto/ver-projeto';
import { UploadProjeto } from './features/upload-projeto/upload-projeto';
import { adminGuard } from './core/auth/admin.guard';


export const routes: Routes = [
    { path: '', component: PaginaInicial },
    { path: 'login-instituicao', component: LoginInstituicao },
    { path: 'admin-main', component: MainAdmin, canActivate: [adminGuard] },
    { path: 'admin-cadastro-instituicao', component: CadastroInstituicao, canActivate: [adminGuard] },
    { path: 'projeto-forms', component: UploadProjeto },
    { path: 'ver-projeto', component: VerProjeto },
];
