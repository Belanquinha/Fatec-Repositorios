import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  InstituicaoOption,
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
