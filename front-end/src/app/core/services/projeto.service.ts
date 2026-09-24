import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  InstituicaoOption,
  ProfessorOption,
  ProjetoCreatePayload,
  ProjetoResponseModel,
  UploadResponseModel,
} from '../models/projeto.model';

export const PROJETOS_MOCK: ProjetoResponseModel[] = [
  {
    id: 'proj-1',
    titulo: 'Golden Maker',
    descricaoCurta: 'Este site oferece um espaço livre e criativo para produtores independentes mostrarem seu talento e conectarem projetos.',
    conteudoEditorJs: '',
    linkRepositorio: 'https://github.com/fatec/golden-maker',
    imagemCapaUrl: 'capa_card.png',
    palavrasChave: ['Web', 'Design', 'Mídia'],
    anoPublicado: 2026,
    estado: 'APROVADO',
    emailProfessorResponsavel: 'carlos.eduardo@cps.sp.gov.br',
    instituicaoId: 'inst-1',
    instituicaoNome: 'Fatec Ipiranga',
    autorId: 'user-1',
    autorNome: 'Mariana Souza',
    integrantes: [
      { nome: 'Mariana Souza', linkLinkedin: 'https://linkedin.com' },
      { nome: 'Lucas Ferreira', linkLinkedin: 'https://linkedin.com' }
    ],
    criadoEm: '2026-01-15T10:00:00Z',
    atualizadoEm: '2026-02-01T14:30:00Z'
  },
  {
    id: 'proj-2',
    titulo: 'AgroTech Monitor',
    descricaoCurta: 'Sistema IoT e telemetria para monitoramento em tempo real de microclima e umidade do solo em estufas agrícolas de alta precisão.',
    conteudoEditorJs: '',
    linkRepositorio: 'https://github.com/fatec/agrotech-monitor',
    imagemCapaUrl: 'capa_card.png',
    palavrasChave: ['IoT', 'Agronegócio', 'Sensores'],
    anoPublicado: 2026,
    estado: 'APROVADO',
    emailProfessorResponsavel: 'roberto.silva@cps.sp.gov.br',
    instituicaoId: 'inst-2',
    instituicaoNome: 'Fatec Pompeia',
    autorId: 'user-2',
    autorNome: 'Gabriel Santos',
    integrantes: [
      { nome: 'Gabriel Santos', linkLinkedin: 'https://linkedin.com' },
      { nome: 'Beatriz Lima', linkLinkedin: 'https://linkedin.com' }
    ],
    criadoEm: '2026-02-10T09:00:00Z',
    atualizadoEm: '2026-02-28T16:00:00Z'
  },
  {
    id: 'proj-3',
    titulo: 'Gestão Hospitalar Ágil',
    descricaoCurta: 'Plataforma web para triagem e otimização do fluxo de leitos hospitalares com dashboard preditivo de atendimento no SUS.',
    conteudoEditorJs: '',
    linkRepositorio: 'https://github.com/fatec/gestao-hospitalar',
    imagemCapaUrl: 'capa_card.png',
    palavrasChave: ['Saúde', 'Web', 'Gestão'],
    anoPublicado: 2025,
    estado: 'APROVADO',
    emailProfessorResponsavel: 'ana.maria@cps.sp.gov.br',
    instituicaoId: 'inst-3',
    instituicaoNome: 'Fatec Baixada Santista',
    autorId: 'user-3',
    autorNome: 'Camila Rocha',
    integrantes: [
      { nome: 'Camila Rocha', linkLinkedin: 'https://linkedin.com' }
    ],
    criadoEm: '2025-11-20T11:00:00Z',
    atualizadoEm: '2025-12-05T10:00:00Z'
  },
  {
    id: 'proj-4',
    titulo: 'EcoRota Logística Reversa',
    descricaoCurta: 'Aplicativo inteligente de roteirização para frotas de coleta seletiva urbana integrado a programa comunitário de créditos sustentáveis.',
    conteudoEditorJs: '',
    linkRepositorio: 'https://github.com/fatec/ecorota',
    imagemCapaUrl: 'capa_card.png',
    palavrasChave: ['Logística', 'Sustentabilidade', 'Mobile'],
    anoPublicado: 2026,
    estado: 'APROVADO',
    emailProfessorResponsavel: 'patricia.lima@cps.sp.gov.br',
    instituicaoId: 'inst-4',
    instituicaoNome: 'Fatec Carapicuíba',
    autorId: 'user-4',
    autorNome: 'Rafael Mendes',
    integrantes: [
      { nome: 'Rafael Mendes', linkLinkedin: 'https://linkedin.com' },
      { nome: 'Juliana Costa', linkLinkedin: 'https://linkedin.com' }
    ],
    criadoEm: '2026-03-01T08:30:00Z',
    atualizadoEm: '2026-03-15T15:20:00Z'
  },
  {
    id: 'proj-5',
    titulo: 'LuePad Repositório Acadêmico',
    descricaoCurta: 'Repositório acadêmico centralizado e catálogo aberto para preservação e disseminação de trabalhos de conclusão de curso do CPS.',
    conteudoEditorJs: '',
    linkRepositorio: 'https://github.com/Belanquinha/Fatec-Repositorios',
    imagemCapaUrl: 'capa_card.png',
    palavrasChave: ['Educação', 'Web', 'Angular'],
    anoPublicado: 2026,
    estado: 'APROVADO',
    emailProfessorResponsavel: 'carlos.eduardo@cps.sp.gov.br',
    instituicaoId: 'inst-5',
    instituicaoNome: 'Fatec São Paulo',
    autorId: 'user-5',
    autorNome: 'Time LuePad',
    integrantes: [
      { nome: 'Gabriel Belanque', linkLinkedin: 'https://linkedin.com' }
    ],
    criadoEm: '2026-03-10T14:00:00Z',
    atualizadoEm: '2026-03-20T17:00:00Z'
  },
  {
    id: 'proj-6',
    titulo: 'VisionAI Diagnóstico Médico',
    descricaoCurta: 'Classificador baseado em visão computacional e redes neurais profundas para triagem preliminar em imagens de raio-X pulmonar.',
    conteudoEditorJs: '',
    linkRepositorio: 'https://github.com/fatec/vision-ai',
    imagemCapaUrl: 'capa_card.png',
    palavrasChave: ['IA & Dados', 'Saúde', 'Inovação'],
    anoPublicado: 2025,
    estado: 'APROVADO',
    emailProfessorResponsavel: 'roberto.silva@cps.sp.gov.br',
    instituicaoId: 'inst-6',
    instituicaoNome: 'Fatec São José dos Campos',
    autorId: 'user-6',
    autorNome: 'Thiago Oliveira',
    integrantes: [
      { nome: 'Thiago Oliveira', linkLinkedin: 'https://linkedin.com' },
      { nome: 'Fernanda Martins', linkLinkedin: 'https://linkedin.com' }
    ],
    criadoEm: '2025-10-05T13:45:00Z',
    atualizadoEm: '2025-11-12T18:10:00Z'
  }
];

@Injectable({
  providedIn: 'root',
})
export class ProjetoService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  listarProjetosPublicos(): Observable<ProjetoResponseModel[]> {
    return this.http.get<ProjetoResponseModel[]>(`${this.apiUrl}/projetos/publicos`).pipe(
      catchError(() => of(PROJETOS_MOCK))
    );
  }

  listarInstituicoes(): Observable<InstituicaoOption[]> {
    return this.http.get<InstituicaoOption[]>(`${this.apiUrl}/instituicoes`).pipe(
      catchError(() => of([
        { id: 'inst-1', codigoUnidade: '001', nome: 'Fatec Ipiranga', cidade: 'São Paulo', ativo: true },
        { id: 'inst-2', codigoUnidade: '002', nome: 'Fatec Pompeia', cidade: 'Pompeia', ativo: true },
        { id: 'inst-3', codigoUnidade: '003', nome: 'Fatec Baixada Santista', cidade: 'Santos', ativo: true },
        { id: 'inst-4', codigoUnidade: '004', nome: 'Fatec Carapicuíba', cidade: 'Carapicuíba', ativo: true },
        { id: 'inst-5', codigoUnidade: '005', nome: 'Fatec São Paulo', cidade: 'São Paulo', ativo: true },
        { id: 'inst-6', codigoUnidade: '006', nome: 'Fatec São José dos Campos', cidade: 'São José dos Campos', ativo: true }
      ]))
    );
  }

  listarProfessores(): Observable<ProfessorOption[]> {
    return this.http.get<ProfessorOption[]>(`${this.apiUrl}/professores`);
  }

  uploadImagem(arquivo: File): Observable<UploadResponseModel> {
    const formData = new FormData();
    formData.append('file', arquivo);
    return this.http.post<UploadResponseModel>(`${this.apiUrl}/uploads`, formData);
  }

  criarProjeto(payload: ProjetoCreatePayload): Observable<ProjetoResponseModel> {
    return this.http.post<ProjetoResponseModel>(`${this.apiUrl}/projetos`, payload);
  }

  obterProjetoPorId(id: string): Observable<ProjetoResponseModel> {
    return this.http.get<ProjetoResponseModel>(`${this.apiUrl}/projetos/${id}`);
  }

  obterMeusProjetos(): Observable<ProjetoResponseModel[]> {
    return this.http.get<ProjetoResponseModel[]>(`${this.apiUrl}/projetos/meus`);
  }
}
