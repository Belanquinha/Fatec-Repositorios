import { Component, NgZone, AfterViewInit, OnDestroy } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

import EditorJS, { OutputData } from '@editorjs/editorjs';
import Header from '@editorjs/header';
import List from '@editorjs/list';
import CodeTool from '@editorjs/code';
import Quote from '@editorjs/quote';
import ImageTool from '@editorjs/image';

interface Integrante {
  id: number;
  nome: string;
  linkedin: string;
}

interface Professor {
  nome: string;
  email: string;
}

@Component({
  selector: 'app-upload-projeto',
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule, CommonModule],
  templateUrl: './upload-projeto.html',
  styleUrl: './upload-projeto.css',
})
export class UploadProjeto implements AfterViewInit, OnDestroy {
  secaoAtual = 1;
  totalSecoes = 4;

  // Seção 1: Dados Gerais & Capa
  titulo = '';
  descricaoCurta = '';
  capaPreview: string | null = null;

  // Seção 2: Instituição & Validação Acadêmica
  instituicao = '';
  professorEmail = '';
  professorStatus: 'cadastrado' | 'novo' | 'vazio' = 'vazio';
  mostrarDropdownProfessor = false;
  linkRepositorio = '';
  palavrasChave = '';

  // Lista mockada de professores para o Autocomplete
  professoresBase: Professor[] = [
    { nome: 'Prof. Dr. Carlos Eduardo', email: 'carlos.eduardo@cps.sp.gov.br' },
    { nome: 'Profa. Dra. Ana Maria Souza', email: 'ana.maria@cps.sp.gov.br' },
    { nome: 'Prof. Me. Roberto Silva', email: 'roberto.silva@cps.sp.gov.br' },
    { nome: 'Profa. Me. Patricia Lima', email: 'patricia.lima@cps.sp.gov.br' },
  ];
  professoresFiltrados: Professor[] = [];

  // Seção 3: Editor.js
  private editor: EditorJS | null = null;
  editorData: OutputData | null = null;

  // Seção 4: Lista Dinâmica de Integrantes
  integrantes: Integrante[] = [
    { id: 1, nome: '', linkedin: '' },
    { id: 2, nome: '', linkedin: '' },
  ];

  // Estado do envio
  enviadoComSucesso = false;

  constructor(private zone: NgZone) {}

  ngAfterViewInit() {
    if (this.secaoAtual === 3) {
      this.inicializarEditor();
    }
  }

  ngOnDestroy() {
    this.destruirEditor();
  }

  // --- Inicialização do Editor.js ---
  private inicializarEditor() {
    setTimeout(() => {
      const container = document.getElementById('editorjs-container');
      if (!container || this.editor) return;

      try {
        this.editor = new EditorJS({
          holder: 'editorjs-container',
          placeholder: 'Escreva a documentação do projeto ou insira imagens e blocos de código...',
          tools: {
            header: {
              class: Header,
              config: {
                placeholder: 'Digite um cabeçalho...',
                levels: [2, 3, 4],
                defaultLevel: 2,
              },
            },
            list: {
              class: List,
              inlineToolbar: true,
            },
            code: {
              class: CodeTool,
              config: {
                placeholder: 'Cole o código do projeto aqui...',
              },
            },
            quote: {
              class: Quote,
              inlineToolbar: true,
              config: {
                quotePlaceholder: 'Digite uma citação ou destaque...',
                captionPlaceholder: 'Autor da citação',
              },
            },
            image: {
              class: ImageTool,
              config: {
                uploader: {
                  uploadByFile: (file: File) => {
                    return new Promise((resolve) => {
                      const reader = new FileReader();
                      reader.onload = () => {
                        this.zone.run(() => {
                          resolve({
                            success: 1,
                            file: {
                              url: reader.result as string,
                            },
                          });
                        });
                      };
                      reader.readAsDataURL(file);
                    });
                  },
                },
              },
            },
          },
          data: this.editorData || undefined,
          onChange: async () => {
            if (this.editor) {
              this.editorData = await this.editor.save();
            }
          },
        });
      } catch (err) {
        console.error('Erro ao inicializar Editor.js:', err);
      }
    }, 100);
  }

  private destruirEditor() {
    if (this.editor && typeof this.editor.destroy === 'function') {
      try {
        this.editor.destroy();
      } catch (e) {
        // Ignora se o DOM já foi removido
      }
      this.editor = null;
    }
  }

  // --- Capa do Projeto ---
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

  removerCapa(event?: Event) {
    if (event) event.stopPropagation();
    this.capaPreview = null;
  }

  // --- Autocomplete Professor Responsável ---
  onProfessorEmailInput() {
    const busca = this.professorEmail.trim().toLowerCase();
    if (!busca) {
      this.professoresFiltrados = [];
      this.mostrarDropdownProfessor = false;
      this.professorStatus = 'vazio';
      return;
    }

    this.professoresFiltrados = this.professoresBase.filter(
      (p) =>
        p.nome.toLowerCase().includes(busca) ||
        p.email.toLowerCase().includes(busca)
    );
    this.mostrarDropdownProfessor = this.professoresFiltrados.length > 0;

    const encontrado = this.professoresBase.some(
      (p) => p.email.toLowerCase() === busca
    );
    this.professorStatus = encontrado ? 'cadastrado' : 'novo';
  }

  selecionarProfessor(prof: Professor) {
    this.professorEmail = prof.email;
    this.professorStatus = 'cadastrado';
    this.mostrarDropdownProfessor = false;
  }

  ocultarDropdownComDelay() {
    setTimeout(() => {
      this.mostrarDropdownProfessor = false;
    }, 200);
  }

  // --- Integrantes Dinâmicos ---
  adicionarIntegrante() {
    const novoId = this.integrantes.length + 1;
    this.integrantes.push({
      id: novoId,
      nome: '',
      linkedin: '',
    });
  }

  removerIntegrante(index: number) {
    if (this.integrantes.length > 1) {
      this.integrantes.splice(index, 1);
    }
  }

  // --- Navegação entre Seções ---
  irParaSecao(secao: number) {
    if (secao >= 1 && secao <= this.totalSecoes) {
      this.secaoAtual = secao;
      if (secao === 3) {
        this.inicializarEditor();
      }
    }
  }

  proximaSecao() {
    if (this.secaoAtual < this.totalSecoes) {
      this.secaoAtual++;
      if (this.secaoAtual === 3) {
        this.inicializarEditor();
      }
    }
  }

  secaoAnterior() {
    if (this.secaoAtual > 1) {
      this.secaoAtual--;
      if (this.secaoAtual === 3) {
        this.inicializarEditor();
      }
    }
  }

  // --- Submissão Final ---
  async enviarParaAvaliacao() {
    if (this.editor) {
      try {
        this.editorData = await this.editor.save();
        console.log('Dados do Editor.js capturados:', this.editorData);
      } catch (err) {
        console.error('Erro ao salvar dados do Editor.js:', err);
      }
    }
    this.enviadoComSucesso = true;
  }

  resetarFormulario() {
    this.destruirEditor();
    this.enviadoComSucesso = false;
    this.secaoAtual = 1;
    this.titulo = '';
    this.descricaoCurta = '';
    this.capaPreview = null;
    this.instituicao = '';
    this.professorEmail = '';
    this.professorStatus = 'vazio';
    this.linkRepositorio = '';
    this.palavrasChave = '';
    this.editorData = null;
    this.integrantes = [
      { id: 1, nome: '', linkedin: '' },
      { id: 2, nome: '', linkedin: '' },
    ];
  }
}
