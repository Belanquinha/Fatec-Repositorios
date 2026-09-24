import { Component, NgZone, OnInit, AfterViewInit, OnDestroy, inject } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import EditorJS, { OutputData } from '@editorjs/editorjs';
import Header from '@editorjs/header';
import List from '@editorjs/list';
import CodeTool from '@editorjs/code';
import Quote from '@editorjs/quote';
import ImageTool from '@editorjs/image';

import { ProjetoService } from '../../core/services/projeto.service';
import {
  InstituicaoOption,
  ProfessorOption,
  ProjetoCreatePayload,
} from '../../core/models/projeto.model';
import { SelecionarInstituicao } from '../selecionar-instituicao/selecionar-instituicao';
import { environment } from '../../../environments/environment';

interface IntegranteLocal {
  id: number;
  nome: string;
  linkedin: string;
}

@Component({
  selector: 'app-upload-projeto',
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule, CommonModule, SelecionarInstituicao],
  templateUrl: './upload-projeto.html',
  styleUrl: './upload-projeto.css',
})
export class UploadProjeto implements OnInit, AfterViewInit, OnDestroy {
  private zone = inject(NgZone);
  private projetoService = inject(ProjetoService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  secaoAtual = 1;
  totalSecoes = 4;

  // Seção 1: Dados Gerais & Capa
  titulo = '';
  descricaoCurta = '';
  capaArquivo: File | null = null;
  capaPreview: string | null = null;
  imagemCapaUrl: string | null = null;

  // Seção 2: Instituição & Validação Acadêmica
  instituicaoId = '';
  instituicaoSelecionadaObjeto: InstituicaoOption | null = null;
  instituicoes: InstituicaoOption[] = [];
  carregandoInstituicoes = false;
  mostrarModalTrocaInstituicao = false;

  professorEmail = '';
  professorStatus: 'cadastrado' | 'novo' | 'vazio' = 'vazio';
  mostrarDropdownProfessor = false;
  linkRepositorio = '';
  palavrasChave = '';

  professoresBase: ProfessorOption[] = [
    { id: '1', nome: 'Prof. Dr. Carlos Eduardo', email: 'carlos.eduardo@cps.sp.gov.br' },
    { id: '2', nome: 'Profa. Dra. Ana Maria Souza', email: 'ana.maria@cps.sp.gov.br' },
    { id: '3', nome: 'Prof. Me. Roberto Silva', email: 'roberto.silva@cps.sp.gov.br' },
    { id: '4', nome: 'Profa. Me. Patricia Lima', email: 'patricia.lima@cps.sp.gov.br' },
  ];
  professoresFiltrados: ProfessorOption[] = [];

  // Seção 3: Editor.js
  private editor: EditorJS | null = null;
  editorData: OutputData | null = null;

  // Seção 4: Lista Dinâmica de Integrantes
  integrantes: IntegranteLocal[] = [
    { id: 1, nome: '', linkedin: '' },
    { id: 2, nome: '', linkedin: '' },
  ];

  // Estado do envio e feedbacks
  enviando = false;
  enviadoComSucesso = false;
  erroMensagem: string | null = null;

  ngOnInit() {
    this.carregarProfessores();
    this.carregarInstituicoes();

    this.route.queryParams.subscribe((params) => {
      if (params['instituicaoId']) {
        this.instituicaoId = params['instituicaoId'];
        this.sincronizarInstituicaoSelecionada();
      }
    });
  }

  ngAfterViewInit() {
    if (this.secaoAtual === 3) {
      this.inicializarEditor();
    }
  }

  ngOnDestroy() {
    this.destruirEditor();
  }

  // --- Carga de dados remotos ---
  private carregarInstituicoes() {
    this.carregandoInstituicoes = true;
    this.projetoService.listarInstituicoes().subscribe({
      next: (dados) => {
        this.instituicoes = (dados || []).filter((i) => i.ativo);
        this.carregandoInstituicoes = false;
        this.sincronizarInstituicaoSelecionada();
      },
      error: (err) => {
        console.warn('Não foi possível carregar instituições do backend:', err);
        this.carregandoInstituicoes = false;
      },
    });
  }

  sincronizarInstituicaoSelecionada() {
    if (this.instituicaoId && this.instituicoes.length > 0) {
      const encontrada = this.instituicoes.find((i) => i.id === this.instituicaoId);
      if (encontrada) {
        this.instituicaoSelecionadaObjeto = encontrada;
      }
    }
  }

  abrirModalTrocaInstituicao() {
    this.mostrarModalTrocaInstituicao = true;
  }

  fecharModalTrocaInstituicao() {
    this.mostrarModalTrocaInstituicao = false;
  }

  onInstituicaoTrocada(nova: InstituicaoOption) {
    this.instituicaoId = nova.id;
    this.instituicaoSelecionadaObjeto = nova;
    this.fecharModalTrocaInstituicao();
    // Atualiza a URL sem recarregar
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { instituicaoId: nova.id },
      queryParamsHandling: 'merge',
    });
  }

  getLogoUrl(codigoUnidade?: string): string {
    return codigoUnidade ? `/logos-fatec/${codigoUnidade}.png` : '';
  }

  private carregarProfessores() {
    this.projetoService.listarProfessores().subscribe({
      next: (profs) => {
        if (profs && profs.length > 0) {
          this.professoresBase = profs;
        }
      },
      error: (err) => {
        console.warn('Não foi possível buscar professores remotos, mantendo base inicial:', err);
      },
    });
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
                  uploadByFile: async (file: File) => {
                    try {
                      const res = await firstValueFrom(this.projetoService.uploadImagem(file));
                      const fullUrl = res.url.startsWith('http')
                        ? res.url
                        : `${environment.apiUrl}${res.url}`;
                      return {
                        success: 1,
                        file: {
                          url: fullUrl,
                        },
                      };
                    } catch (err) {
                      console.warn('Upload remoto de imagem falhou, usando fallback Base64:', err);
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
                    }
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
      this.capaArquivo = arquivo;
      this.erroMensagem = null;

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
    this.capaArquivo = null;
    this.imagemCapaUrl = null;
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

  selecionarProfessor(prof: ProfessorOption) {
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

  // --- Submissão Final Integrada ---
  async enviarParaAvaliacao() {
    this.erroMensagem = null;

    // 1. Validações preliminares
    if (!this.titulo.trim()) {
      this.erroMensagem = 'Por favor, informe o título do projeto.';
      this.irParaSecao(1);
      return;
    }

    if (!this.descricaoCurta.trim()) {
      this.erroMensagem = 'Por favor, informe uma descrição curta para o projeto.';
      this.irParaSecao(1);
      return;
    }

    if (this.descricaoCurta.trim().length > 144) {
      this.erroMensagem = 'A descrição curta não pode exceder 144 caracteres.';
      this.irParaSecao(1);
      return;
    }

    if (!this.capaArquivo && !this.capaPreview && !this.imagemCapaUrl) {
      this.erroMensagem = 'Selecione uma imagem de capa para o projeto.';
      this.irParaSecao(1);
      return;
    }

    if (!this.instituicaoId) {
      this.erroMensagem = 'Selecione a unidade FATEC responsável.';
      this.irParaSecao(2);
      return;
    }

    if (!this.professorEmail.trim()) {
      this.erroMensagem = 'Informe o e-mail do professor responsável pela validação acadêmica.';
      this.irParaSecao(2);
      return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(this.professorEmail.trim())) {
      this.erroMensagem = 'Informe um e-mail válido para o professor responsável.';
      this.irParaSecao(2);
      return;
    }

    const integrantesValidos = this.integrantes.filter((i) => i.nome.trim().length > 0);
    if (integrantesValidos.length === 0) {
      this.erroMensagem = 'Informe ao menos um integrante com o nome completo preenchido.';
      this.irParaSecao(4);
      return;
    }

    // 2. Salvar conteúdo do Editor.js
    if (this.editor) {
      try {
        this.editorData = await this.editor.save();
      } catch (err) {
        console.error('Erro ao salvar dados do Editor.js:', err);
      }
    }

    this.enviando = true;

    try {
      // 3. Upload da capa se arquivo físico estiver pendente
      let capaFinalUrl = this.imagemCapaUrl;
      if (this.capaArquivo) {
        try {
          const uploadRes = await firstValueFrom(this.projetoService.uploadImagem(this.capaArquivo));
          capaFinalUrl = uploadRes.url;
        } catch (uploadErr: any) {
          console.error('Falha no upload da capa:', uploadErr);
          // Se falhar upload de imagem no backend por estar offline ou não autorizado, prossegue se tiver preview
          if (!capaFinalUrl && this.capaPreview) {
            capaFinalUrl = this.capaPreview;
          }
        }
      }

      // 4. Tratamento das palavras-chave
      const tags = this.palavrasChave
        ? this.palavrasChave
            .split(',')
            .map((t) => t.trim())
            .filter((t) => t.length > 0)
        : [];

      // 5. Montagem do Payload
      const payload: ProjetoCreatePayload = {
        titulo: this.titulo.trim(),
        descricaoCurta: this.descricaoCurta.trim(),
        conteudoEditorJs: this.editorData ? JSON.stringify(this.editorData) : '',
        linkRepositorio: this.linkRepositorio.trim() || undefined,
        imagemCapaUrl: capaFinalUrl || undefined,
        palavrasChave: tags,
        anoPublicado: new Date().getFullYear(),
        instituicaoId: this.instituicaoId,
        emailProfessorResponsavel: this.professorEmail.trim().toLowerCase(),
        integrantes: integrantesValidos.map((i) => ({
          nome: i.nome.trim(),
          linkLinkedin: i.linkedin.trim() || undefined,
        })),
      };

      // 6. Chamada de criação do projeto
      const projetoCriado = await firstValueFrom(this.projetoService.criarProjeto(payload));
      console.log('Projeto submetido com sucesso:', projetoCriado);

      this.enviando = false;
      this.enviadoComSucesso = true;
    } catch (err: any) {
      console.error('Erro ao submeter projeto para avaliação:', err);
      this.enviando = false;
      const msgErro =
        err?.error?.mensagem ||
        err?.error?.message ||
        'Não foi possível enviar o projeto para avaliação. Verifique sua conexão e tente novamente.';
      this.erroMensagem = msgErro;
    }
  }

  resetarFormulario() {
    this.destruirEditor();
    this.enviadoComSucesso = false;
    this.enviando = false;
    this.erroMensagem = null;
    this.secaoAtual = 1;
    this.titulo = '';
    this.descricaoCurta = '';
    this.capaArquivo = null;
    this.capaPreview = null;
    this.imagemCapaUrl = null;
    this.instituicaoId = '';
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
