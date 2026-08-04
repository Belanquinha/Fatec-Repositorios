import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { MicrosoftLoginButton } from './shared/components/microsoft-login-button/microsoft-login-button';
import { Header } from './shared/components/header/header';
import { Footer } from './shared/components/footer/footer';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, MicrosoftLoginButton, Header, Footer],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('front-end');
}
