import { Component } from '@angular/core';
import { RouterLink } from '@angular/router'; // 1. Importe aqui

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink], // 2. Adicione nas imports do componente
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class HomeComponent { }