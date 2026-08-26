import { Routes } from '@angular/router';
import { HomeComponent } from './home/home';
import { CadastroInstComponent } from './cadastro-inst/cadastro-inst';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'cadastrar', component: CadastroInstComponent },
  { path: '**', redirectTo: '' }
];