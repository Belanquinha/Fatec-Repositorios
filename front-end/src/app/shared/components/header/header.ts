import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-header',
  imports: [CommonModule, RouterModule],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header {

  estadoDoMenuAberto = false;
  isHovered = false;

  mudarMenu(): void {
    this.estadoDoMenuAberto = !this.estadoDoMenuAberto;
  }
}
