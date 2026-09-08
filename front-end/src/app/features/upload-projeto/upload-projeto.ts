import { Component, NgZone } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-upload-projeto',
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './upload-projeto.html',
  styleUrl: './upload-projeto.css',
})
export class UploadProjeto {
  projeto = new FormControl('');

  etapaAtual = 1;
  totalEtapas = 4;

  capaPreview: string | null = null;
  qtdIntegrantes = 2;

  constructor(private zone: NgZone) {}

  onCapaSelecionada(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const arquivo = input.files[0];
      const reader = new FileReader();
      reader.onload = () => {
        this.zone.run(() => {
          this.capaPreview = reader.result as string;
        });
      };
      reader.readAsDataURL(arquivo);
    }
  }

  onQtdIntegrantesChange(event: Event) {
    const select = event.target as HTMLSelectElement;
    this.qtdIntegrantes = Number(select.value);
  }

  get integrantes(): number[] {
    return Array.from({ length: this.qtdIntegrantes }, (_, i) => i + 1);
  }

  PassarEtapa(elemento: HTMLElement) {
    if (this.etapaAtual < 4) {
      this.etapaAtual++;
      elemento.scrollIntoView({ behavior: 'smooth', block: 'start'})
    }
  }
  VoltarEtapa(elemento: HTMLElement) {
    if (this.etapaAtual > 1) {
      this.etapaAtual--;
      elemento.scrollIntoView({ behavior: 'smooth', block: 'start'})
    }
  }
}
