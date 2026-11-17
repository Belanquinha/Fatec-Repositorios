import {
  Component,
  DestroyRef,
  ElementRef,
  OnInit,
  AfterViewInit,
  AfterViewChecked,
  OnDestroy,
  ViewChild,
  inject,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import EditorJS, { OutputData } from '@editorjs/editorjs';
import Header from '@editorjs/header';
import List from '@editorjs/list';
import CodeTool from '@editorjs/code';
import Quote from '@editorjs/quote';
import ImageTool from '@editorjs/image';

import { ProjetoService } from '../../core/services/projeto.service';
import {
  InstituicaoOption,
  ProjetoCreatePayload,
} from '../../core/models/projeto.model';
import { environment } from '../../../environments/environment';

interface IntegranteLocal {
  id: number;
  nome: string;
  linkedin: string;
}

/**
 * Teto de imagem (capa + blocos do Editor.js), espelhando o back-end
 * (`multipart.max-file-size: 10MB` + `FileStorageService`). O front recusa
 * antes de enviar para que o usuário veja a mensagem na hora, em vez de um
 * 413 do nginx / 400 genérico depois do upload.
 */
const MAX_IMAGEM_BYTES = 10 * 1024 * 1024;
const TIPOS_IMAGEM_PERMITIDOS = [
  'image/png',
  'image/jpeg',
  'image/jpg',
  'image/webp',
];
/** Guarda do POST /projetos: abaixo do `client_max_body_size 12m` do nginx. */
const MAX_PAYLOAD_BYTES = 11 * 1024 * 1024;

function validarArquivoImagem(file: File): string | null {
  if (!TIPOS_IMAGEM_PERMITIDOS.includes((file.type || '').toLowerCase())) {
    return 'Tipo de arquivo não permitido. Use PNG, JPG, JPEG ou WEBP.';
  }
  if (file.size > MAX_IMAGEM_BYTES) {
    return `Imagem de ${(file.size / 1024 / 1024).toFixed(1)}MB excede o limite de 10MB. Comprima ou escolha um arquivo menor.`;
  }
  return null;
}

@Component({
  selector: 'app-upload-projeto',
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule, CommonModule],
  templateUrl: './upload-projeto.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './upload-projeto.css',
})
export class UploadProjeto implements OnInit, AfterViewInit, OnDestroy {
  private projetoService = inject(ProjetoService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  secaoAtual = 1;
  totalSecoes = 4;

  // Seção 1: Dados Gerais & Capa
  titulo = '';
  descricaoCurta = '';
  capaArquivo: File | null = null;
  capaPreview = signal<string | null>(null);
  imagemCapaUrl: string | null = null;

  // Seção 2: Instituição & Validação Acadêmica
  @ViewChild('inputInstituicao')
  private inputInstituicao?: ElementRef<HTMLInputElement>;
  private readonly limiteSugestoes = 50;
  instituicaoId = '';
  instituicaoSelecionadaObjeto = signal<InstituicaoOption | null>(null);
  instituicoes = signal<InstituicaoOption[]>([]);
  instituicoesFiltradas: InstituicaoOption[] = [];
  instituicoesEncontradas = 0;
  buscaInstituicao = '';
  mostrarDropdownInstituicao = signal(false);
  instituicaoDestaque = -1;
  carregandoInstituicoes = signal(false);
  erroCarregarInstituicoes = signal(false);

  /**
   * Campo de e-mail do professor (Story 3.2, decisão 2026-10-26): somente
   * e-mail, sem autocomplete por nome — o aluno digita o endereço direto e a
   * validação de formato acontece no envio. E-mails fora do tenant são
   * aceitos como "Professor Convidado" (registro informativo, sem notificação
   * no MVP), então não há estado de "cadastrado/novo" a rastrear aqui.
   */
  professorEmail = '';
  linkRepositorio = '';
  palavrasChave = '';

  // Seção 3: Editor.js
  @ViewChild('editorjsContainer')
  private editorjsContainer?: ElementRef<HTMLDivElement>;
  private editor: EditorJS | null = null;
  private editorInicializacaoPendente = false;
  private editorFila: Promise<void> = Promise.resolve();
  editorData: OutputData | null = null;

  // Seção 4: Lista Dinâmica de Integrantes
  integrantes: IntegranteLocal[] = [
    { id: 1, nome: '', linkedin: '' },
    { id: 2, nome: '', linkedin: '' },
  ];

  // Estado do envio e feedbacks
  enviando = signal(false);
  enviadoComSucesso = signal(false);
  erroMensagem = signal<string | null>(null);

  ngOnInit() {
    this.carregarInstituicoes();

    // takeUntilDestroyed: a navegação (`atualizarQueryParamInstituicao`, merge)
    // reusa este componente sem destruí-lo, então take(1) perderia trocas de
    // `instituicaoId` vindas da URL depois do primeiro emit.
    this.route.queryParams
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        if (params['instituicaoId']) {
          this.instituicaoId = params['instituicaoId'];
          this.sincronizarInstituicaoSelecionada();
        }
      });
  }

  ngAfterViewInit() {
    this.tentarIniciarEditor();
  }

  ngAfterViewChecked() {
    if (this.editorInicializacaoPendente) {
      this.tentarIniciarEditor();
    }
  }

  ngOnDestroy() {
    this.editorInicializacaoPendente = false;
    this.encadearEditor(async () => {
      await this.sairDaSecaoDoEditor();
      this.editorData = null;
    });
  }

  // --- Carga de dados remotos ---
  private carregarInstituicoes() {
    this.carregandoInstituicoes.set(true);
    this.erroCarregarInstituicoes.set(false);
    this.projetoService.listarInstituicoes().subscribe({
      next: (dados) => {
        this.instituicoes.set((dados || []).filter((i) => i.ativo));
        this.carregandoInstituicoes.set(false);
        this.sincronizarInstituicaoSelecionada();
      },
      error: (err) => {
        this.instituicoes.set([]);
        this.carregandoInstituicoes.set(false);
        this.erroCarregarInstituicoes.set(true);
        console.warn('Não foi possível carregar instituições do backend:', err);
      },
    });
  }

  sincronizarInstituicaoSelecionada() {
    if (this.instituicaoId && this.instituicoes().length > 0) {
      const encontrada = this.instituicoes().find(
        (i) => i.id === this.instituicaoId,
      );
      if (encontrada) {
        this.instituicaoSelecionadaObjeto.set(encontrada);
      }
    }
  }

  getLogoUrl(codigoUnidade?: string): string {
    return codigoUnidade ? `/logos-fatec/${codigoUnidade}.png` : '';
  }

  // --- Autocomplete da Instituição (FATEC) ---
  onInstituicaoBusca(termo: string) {
    this.buscaInstituicao = termo;
    this.mostrarDropdownInstituicao.set(true);
    this.aplicarFiltroInstituicoes();
  }

  abrirDropdownInstituicao() {
    if (this.carregandoInstituicoes()) {
      return;
    }
    this.mostrarDropdownInstituicao.set(true);
    this.aplicarFiltroInstituicoes();
  }

  ocultarDropdownInstituicaoComDelay() {
    setTimeout(() => {
      this.mostrarDropdownInstituicao.set(false);
      this.instituicaoDestaque = -1;
    }, 200);
  }

  private aplicarFiltroInstituicoes() {
    const termo = this.normalizarTexto(this.buscaInstituicao);
    const todas = this.instituicoes();

    const encontradas = !termo
      ? todas
      : todas.filter(
          (inst) =>
            this.normalizarTexto(inst.nome).includes(termo) ||
            this.normalizarTexto(inst.cidade || '').includes(termo) ||
            this.normalizarTexto(inst.codigoUnidade || '').includes(termo) ||
            this.normalizarTexto(inst.endereco || '').includes(termo),
        );

    this.instituicoesEncontradas = encontradas.length;
    this.instituicoesFiltradas = encontradas.slice(0, this.limiteSugestoes);
    this.instituicaoDestaque = this.instituicoesFiltradas.length > 0 ? 0 : -1;
  }

  selecionarInstituicao(inst: InstituicaoOption) {
    this.instituicaoId = inst.id;
    this.instituicaoSelecionadaObjeto.set(inst);
    this.buscaInstituicao = '';
    this.instituicoesFiltradas = [];
    this.instituicoesEncontradas = 0;
    this.mostrarDropdownInstituicao.set(false);
    this.instituicaoDestaque = -1;
    this.atualizarQueryParamInstituicao(inst.id);
  }

  trocarInstituicao() {
    this.instituicaoId = '';
    this.instituicaoSelecionadaObjeto.set(null);
    this.buscaInstituicao = '';
    this.instituicoesFiltradas = [];
    this.instituicoesEncontradas = 0;
    this.mostrarDropdownInstituicao.set(false);
    this.instituicaoDestaque = -1;
    this.atualizarQueryParamInstituicao(null);
    setTimeout(() => this.inputInstituicao?.nativeElement.focus());
  }
  onInstituicaoKeydown(event: KeyboardEvent) {
    const total = this.instituicoesFiltradas.length;
    if (!this.mostrarDropdownInstituicao() || total === 0) {
      return;
    }

    switch (event.key) {
      case 'ArrowDown':
        event.preventDefault();
        this.instituicaoDestaque = (this.instituicaoDestaque + 1) % total;
        break;
      case 'ArrowUp':
        event.preventDefault();
        this.instituicaoDestaque =
          (this.instituicaoDestaque - 1 + total) % total;
        break;
      case 'Enter': {
        event.preventDefault();
        const alvo =
          this.instituicoesFiltradas[this.instituicaoDestaque] ??
          this.instituicoesFiltradas[0];
        if (alvo) {
          this.selecionarInstituicao(alvo);
        }
        break;
      }
      case 'Escape':
        this.mostrarDropdownInstituicao.set(false);
        this.instituicaoDestaque = -1;
        break;
    }
  }

  // Atualiza a URL sem recarregar, preservando os demais query params
  private atualizarQueryParamInstituicao(id: string | null) {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { instituicaoId: id },
      queryParamsHandling: 'merge',
    });
  }

  private normalizarTexto(texto: string): string {
    return (texto || '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .trim();
  }

  // --- Inicialização do Editor.js ---
  // Criar, salvar e destruir o editor passa por uma fila serial: o template usa
  // @if na Seção 3, então o DOM do editor é destruído a cada troca de seção e a
  // instância precisa ser derrubada e recriada. Sem a fila, um "destroy"
  // atrasado poderia apagar um editor recém-criado.
  private encadearEditor(operacao: () => Promise<void>): Promise<void> {
    this.editorFila = this.editorFila.then(operacao).catch((err) => {
      console.error('Falha ao processar o Editor.js:', err);
    });
    return this.editorFila;
  }

  private solicitarInicializacaoEditor() {
    this.editorInicializacaoPendente = true;
    this.tentarIniciarEditor();
  }

  // Só inicializa quando o contêiner já existe na view (o @if pode ainda não ter
  // renderizado). Se não estiver pronto, a flag permanece e ngAfterViewChecked
  // tenta de novo no próximo ciclo de detecção.
  private tentarIniciarEditor() {
    if (
      !this.editorInicializacaoPendente ||
      this.editor ||
      !this.editorjsContainer
    ) {
      return;
    }
    this.editorInicializacaoPendente = false;
    this.encadearEditor(() => this.criarEditor());
  }

  private async criarEditor(): Promise<void> {
    const holder = this.editorjsContainer?.nativeElement;
    if (!holder || this.editor) {
      return;
    }

    try {
      const editor = new EditorJS({
        holder,
        placeholder:
          'Escreva a documentação do projeto ou insira imagens e blocos de código...',
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
                  const invalido = validarArquivoImagem(file);
                  if (invalido) {
                    throw new Error(invalido);
                  }
                  try {
                    const res = await firstValueFrom(
                      this.projetoService.uploadImagem(file),
                    );
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
                    // Sem fallback base64 (item 22): `uploadByFile` é o uploader
                    // de blocos de imagem do Editor.js (único chamador — ver
                    // grep) e qualquer `data:image/...` embutido é barrado no
                    // submit por `temBase64Embutido`, então gerar base64 aqui
                    // seria caminho morto que só adia o erro para um 413 do
                    // nginx. Falha vira mensagem acionável na hora.
                    console.warn(
                      'Upload de imagem falhou (sem fallback base64):',
                      err,
                    );
                    throw new Error(
                      'Não foi possível enviar a imagem ao servidor. Use uma imagem menor (até 10MB) e tente de novo.',
                    );
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

      this.editor = editor;
      await editor.isReady;
    } catch (err) {
      this.destruirEditor();
      console.error('Erro ao inicializar Editor.js:', err);
    }
  }

  private async salvarConteudoEditor(): Promise<void> {
    if (!this.editor) {
      return;
    }
    try {
      this.editorData = await this.editor.save();
    } catch (err) {
      console.error('Erro ao salvar dados do Editor.js:', err);
    }
  }

  // Persiste o conteúdo antes de derrubar a instância, para que ele volte
  // preenchido ao reabrir a Seção 3.
  private async sairDaSecaoDoEditor(): Promise<void> {
    await this.salvarConteudoEditor();
    this.destruirEditor();
  }

  private destruirEditor() {
    if (!this.editor) {
      return;
    }
    if (typeof this.editor.destroy === 'function') {
      try {
        this.editor.destroy();
      } catch {
        // Ignora se o DOM já foi removido
      }
    }
    this.editor = null;
  }

  // --- Capa do Projeto ---
  onCapaSelecionada(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const arquivo = input.files[0];
      const invalido = validarArquivoImagem(arquivo);
      if (invalido) {
        this.capaArquivo = null;
        this.erroMensagem.set(invalido);
        input.value = '';
        return;
      }
      this.capaArquivo = arquivo;
      this.erroMensagem.set(null);

      const reader = new FileReader();
      reader.onload = () => {
        // signal.set() agenda o tick no zoneless — o preview aparece na hora,
        // sem precisar clicar na tela. O antigo `NgZone.run()` era no-op aqui.
        this.capaPreview.set(reader.result as string);
      };
      reader.readAsDataURL(arquivo);
    }
  }

  removerCapa(event?: Event) {
    if (event) event.stopPropagation();
    this.capaPreview.set(null);
    this.capaArquivo = null;
    this.imagemCapaUrl = null;
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
    this.trocarSecao(secao);
  }

  proximaSecao() {
    this.trocarSecao(this.secaoAtual + 1);
  }

  secaoAnterior() {
    this.trocarSecao(this.secaoAtual - 1);
  }

  /**
   * Enter em qualquer campo dispara o `ngSubmit` nativo do form — e aqui ele é
   * **engolido de propósito**: não avança de seção e não envia o projeto. O
   * envio só acontece pelo botão da última seção, que chama
   * `enviarParaAvaliacao()` diretamente.
   *
   * Decisão (revisão 2026-10-27): avançar sem validar levava o aluno para a
   * próxima seção com os obrigatórios da atual vazios (percebido na Seção 1).
   * Alinha o form a EXPERIENCE.md, estado "Enter sem consulta — não submete".
   */
  aoSubmeterForm() {
    // Intencionalmente vazio. Não "otimizar" para avançar de seção: ver doc acima.
  }

  /**
   * Tela de sucesso (item 5): o aluno já enviou o projeto e quer acompanhar
   * o andamento — o caminho natural é a aba "Meus projetos" do Perfil.
   */
  irParaMeusProjetos() {
    this.router.navigate(['/perfil']);
  }

  private trocarSecao(secao: number) {
    if (secao < 1 || secao > this.totalSecoes || secao === this.secaoAtual) {
      return;
    }

    if (this.secaoAtual === 3) {
      this.encadearEditor(() => this.sairDaSecaoDoEditor());
    }

    this.secaoAtual = secao;

    if (secao === 3) {
      this.solicitarInicializacaoEditor();
    }
  }

  // --- Submissão Final Integrada ---
  async enviarParaAvaliacao() {
    this.erroMensagem.set(null);

    // 1. Validações preliminares
    if (!this.titulo.trim()) {
      this.erroMensagem.set('Por favor, informe o título do projeto.');
      this.irParaSecao(1);
      return;
    }

    if (!this.descricaoCurta.trim()) {
      this.erroMensagem.set(
        'Por favor, informe uma descrição curta para o projeto.',
      );
      this.irParaSecao(1);
      return;
    }

    if (this.descricaoCurta.trim().length > 144) {
      this.erroMensagem.set(
        'A descrição curta não pode exceder 144 caracteres.',
      );
      this.irParaSecao(1);
      return;
    }

    if (!this.capaArquivo && !this.capaPreview() && !this.imagemCapaUrl) {
      this.erroMensagem.set('Selecione uma imagem de capa para o projeto.');
      this.irParaSecao(1);
      return;
    }

    if (!this.instituicaoId) {
      this.erroMensagem.set('Selecione a unidade FATEC responsável.');
      this.irParaSecao(2);
      return;
    }

    if (!this.professorEmail.trim()) {
      this.erroMensagem.set(
        'Informe o e-mail do professor responsável pela validação acadêmica.',
      );
      this.irParaSecao(2);
      return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(this.professorEmail.trim())) {
      this.erroMensagem.set(
        'Informe um e-mail válido para o professor responsável.',
      );
      this.irParaSecao(2);
      return;
    }

    const integrantesValidos = this.integrantes.filter(
      (i) => i.nome.trim().length > 0,
    );
    if (integrantesValidos.length === 0) {
      this.erroMensagem.set(
        'Informe ao menos um integrante com o nome completo preenchido.',
      );
      this.irParaSecao(4);
      return;
    }

    // 2. Salvar conteúdo do Editor.js (aguarda qualquer teardown pendente)
    await this.encadearEditor(() => this.salvarConteudoEditor());

    this.enviando.set(true);

    try {
      // 3. Upload da capa se arquivo físico estiver pendente
      let capaFinalUrl = this.imagemCapaUrl;
      if (this.capaArquivo) {
        const invalido = validarArquivoImagem(this.capaArquivo);
        if (invalido) {
          this.enviando.set(false);
          this.erroMensagem.set(invalido);
          this.irParaSecao(1);
          return;
        }
        try {
          const uploadRes = await firstValueFrom(
            this.projetoService.uploadImagem(this.capaArquivo),
          );
          capaFinalUrl = uploadRes.url;
        } catch (uploadErr: any) {
          console.error('Falha no upload da capa:', uploadErr);
          // Nunca envia o preview base64 como imagemCapaUrl: ele infla o JSON
          // e o POST cai com 413. Só reaproveita URL que já veio do servidor.
          const jaEhUrl =
            (capaFinalUrl ?? '').startsWith('/') ||
            (capaFinalUrl ?? '').startsWith('http');
          if (!jaEhUrl) {
            this.enviando.set(false);
            const msgUpload =
              uploadErr?.error?.mensagem ||
              uploadErr?.error?.message ||
              'Não foi possível enviar a capa ao servidor. Use uma imagem até 10MB e tente de novo.';
            this.erroMensagem.set(msgUpload);
            this.irParaSecao(1);
            return;
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
        conteudoEditorJs: this.editorData
          ? JSON.stringify(this.editorData)
          : '',
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

      // 6. Guarda de tamanho: barra antes do 413 do nginx com mensagem acionável.
      // Mede em bytes UTF-8 (não em UTF-16 `.length`): acentos/emoji contam
      // mais de 1 byte no fio e o limite do nginx é em bytes.
      const tamanhoPayload = new TextEncoder().encode(
        JSON.stringify(payload),
      ).length;
      const temBase64Embutido =
        (payload.conteudoEditorJs ?? '').includes('data:image') ||
        (payload.imagemCapaUrl ?? '').startsWith('data:');
      if (temBase64Embutido || tamanhoPayload > MAX_PAYLOAD_BYTES) {
        this.enviando.set(false);
        this.erroMensagem.set(
          temBase64Embutido
            ? 'Há imagens embutidas no conteúdo (não enviadas ao servidor). Remova-as e reinsira por upload, com arquivos até 10MB.'
            : `Projeto muito grande (${(tamanhoPayload / 1024 / 1024).toFixed(1)}MB). Reduza as imagens (até 10MB cada) e tente de novo.`,
        );
        return;
      }

      // 7. Chamada de criação do projeto
      const projetoCriado = await firstValueFrom(
        this.projetoService.criarProjeto(payload),
      );
      console.log('Projeto submetido com sucesso:', projetoCriado);

      this.enviando.set(false);
      this.enviadoComSucesso.set(true);
    } catch (err: any) {
      console.error('Erro ao submeter projeto para avaliação:', err);
      this.enviando.set(false);
      if (err?.status === 413) {
        this.erroMensagem.set(
          'Projeto muito grande para o servidor (limite 10MB por imagem). Comprima as imagens e tente de novo.',
        );
        return;
      }
      const msgErro =
        err?.error?.mensagem ||
        err?.error?.message ||
        'Não foi possível enviar o projeto para avaliação. Verifique sua conexão e tente novamente.';
      this.erroMensagem.set(
        typeof msgErro === 'string' && msgErro.trim().startsWith('<')
          ? 'O servidor recusou o envio pelo tamanho. Comprima as imagens (até 10MB) e tente de novo.'
          : msgErro,
      );
    }
  }

  limparErro(): void {
    this.erroMensagem.set(null);
  }

  resetarFormulario() {
    this.editorInicializacaoPendente = false;
    this.encadearEditor(async () => {
      await this.sairDaSecaoDoEditor();
      this.editorData = null;
    });
    this.enviadoComSucesso.set(false);
    this.enviando.set(false);
    this.erroMensagem.set(null);
    this.secaoAtual = 1;
    this.titulo = '';
    this.descricaoCurta = '';
    this.capaArquivo = null;
    this.capaPreview.set(null);
    this.imagemCapaUrl = null;
    this.instituicaoId = '';
    this.instituicaoSelecionadaObjeto.set(null);
    this.buscaInstituicao = '';
    this.instituicoesFiltradas = [];
    this.instituicoesEncontradas = 0;
    this.mostrarDropdownInstituicao.set(false);
    this.instituicaoDestaque = -1;
    this.professorEmail = '';
    this.linkRepositorio = '';
    this.palavrasChave = '';
    this.integrantes = [
      { id: 1, nome: '', linkedin: '' },
      { id: 2, nome: '', linkedin: '' },
    ];
  }
}
