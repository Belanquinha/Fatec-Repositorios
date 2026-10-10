import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  InstituicaoOption,
  ProfessorDetalhe,
  ProfessorOption,
  ProjetoCreatePayload,
  ProjetoResponseModel,
  UploadResponseModel,
} from '../models/projeto.model';

@Injectable({
  providedIn: 'root',
})
export class ProjetoService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  listarProjetosPublicos(): Observable<ProjetoResponseModel[]> {
    return this.http.get<ProjetoResponseModel[]>(`${this.apiUrl}/projetos/publicos`);
  }

  listarInstituicoes(): Observable<InstituicaoOption[]> {
    return this.http.get<InstituicaoOption[]>(`${this.apiUrl}/instituicoes`);
  }

  /**
   * LGPD (Story 3.2): autocomplete por nome, autenticado, com ?q (mín. 2 chars
   * no servidor, máx. 10 resultados). A lista expõe só id+nome — nunca e-mail
   * em massa. Termo curto retorna [] sem chamar o backend.
   */
  buscarProfessores(termo: string): Observable<ProfessorOption[]> {
    const q = (termo || '').trim();
    if (q.length < 2) {
      return of([]);
    }
    return this.http.get<ProfessorOption[]>(`${this.apiUrl}/professores`, {
      params: { q },
    });
  }

  /** E-mail de UM professor já selecionado (para emailProfessorResponsavel). */
  obterProfessorPorId(id: string): Observable<ProfessorDetalhe> {
    return this.http.get<ProfessorDetalhe>(
      `${this.apiUrl}/professores/${encodeURIComponent(id)}`
    );
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

  /**
   * Pendentes para o professor logado (tela /perfil).
   * O back-end filtra por: professor responsável indicado na submissão +
   * vínculo com a instituição do projeto. Sem vínculo ou sem indicação → [].
   */
  listarPendentesAprovacao(): Observable<ProjetoResponseModel[]> {
    return this.http.get<ProjetoResponseModel[]>(`${this.apiUrl}/projetos/pendentes`);
  }

  aprovarProjeto(id: string): Observable<ProjetoResponseModel> {
    return this.http.patch<ProjetoResponseModel>(
      `${this.apiUrl}/projetos/${encodeURIComponent(id)}/aprovar`,
      {}
    );
  }

  rejeitarProjeto(id: string, motivo: string): Observable<ProjetoResponseModel> {
    return this.http.patch<ProjetoResponseModel>(
      `${this.apiUrl}/projetos/${encodeURIComponent(id)}/rejeitar`,
      { motivo }
    );
  }

  /**
   * Vínculos do professor (Frame B · aba "Instituições (0–4)" do /perfil).
   * GET retorna 0–4 instituições ordenadas por nome; PUT substitui o conjunto
   * completo (idempotente, [] limpa). Erros 400/404/409 vêm no envelope
   * {"codigo","mensagem"} e são exibidos pelo chamador em painel neutro.
   */
  obterMeusVinculos(): Observable<InstituicaoOption[]> {
    return this.http.get<InstituicaoOption[]>(`${this.apiUrl}/professores/me/instituicoes`);
  }

  salvarMeusVinculos(instituicaoIds: string[]): Observable<InstituicaoOption[]> {
    return this.http.put<InstituicaoOption[]>(`${this.apiUrl}/professores/me/instituicoes`, {
      instituicaoIds,
    });
  }
}
