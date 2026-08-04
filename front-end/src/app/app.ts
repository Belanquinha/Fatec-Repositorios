import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { PaginaInicial } from './features/pagina-inicial/pagina-inicial';
import { MicrosoftLoginButton } from './shared/components/microsoft-login-button/microsoft-login-button';
import { Header } from './shared/components/header/header';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, MicrosoftLoginButton, Header],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('front-end');
}
