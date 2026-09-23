import { Routes } from '@angular/router';
import { UploadProjeto } from './features/upload-projeto/upload-projeto';

export const routes: Routes = [
    { path: '', redirectTo: 'projeto-forms', pathMatch: 'full' },
    { path: 'projeto-forms', component: UploadProjeto },
    { path: '**', redirectTo: 'projeto-forms' },
];