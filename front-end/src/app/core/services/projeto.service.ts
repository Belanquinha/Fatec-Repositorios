import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpEvent } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CursoOption,
  DevolverProjetoPayload,
  ExcluirProjetoPayload,
  ExclusaoContaModel,
  ExclusaoProjetoModel,
  InstituicaoOption,
  JogoUploadResponseModel,
  PerfilInstituicaoModel,
  PerfilPublicoUsuarioModel,
  ProfessorDetalhe,
  ProfessorOption,
  ProjetoCreatePayload,
  ProjetoRemovidoModel,
  ProjetoResponseModel,
  SessaoJogoResponseModel,
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

  obterPerfilInstituicao(id: string): Observable<PerfilInstituicaoModel> {
    return this.http.get<PerfilInstituicaoModel>(
      `${this.apiUrl}/instituicoes/${encodeURIComponent(id)}/perfil`
    );
  }

  obterPerfilUsuario(id: string): Observable<PerfilPublicoUsuarioModel> {
    return this.http.get<PerfilPublicoUsuarioModel>(
      `${this.apiUrl}/usuarios/${encodeURIComponent(id)}/perfil`
    );
  }

  listarCursosDaInstituicao(instituicaoId: string): Observable<CursoOption[]> {
    return this.http.get<CursoOption[]>(
      `${this.apiUrl}/instituicoes/${encodeURIComponent(instituicaoId)}/cursos`
    );
  }

  listarTodosCursos(): Observable<CursoOption[]> {
    return this.http.get<CursoOption[]>(`${this.apiUrl}/cursos`);
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

  uploadPacoteJogo(arquivo: File): Observable<HttpEvent<JogoUploadResponseModel>> {
    const formData = new FormData();
    formData.append('file', arquivo);
    return this.http.post<JogoUploadResponseModel>(`${this.apiUrl}/jogos/upload`, formData, {
      reportProgress: true,
      observe: 'events',
    });
  }

  criarSessaoJogo(projetoId: string): Observable<SessaoJogoResponseModel> {
    return this.http.post<SessaoJogoResponseModel>(
      `${this.apiUrl}/projetos/${encodeURIComponent(projetoId)}/sessao-jogo`,
      {}
    );
  }

  criarProjeto(payload: ProjetoCreatePayload): Observable<ProjetoResponseModel> {
    return this.http.post<ProjetoResponseModel>(`${this.apiUrl}/projetos`, payload);
  }

  obterProjetoPorId(id: string): Observable<ProjetoResponseModel> {
    return this.http.get<ProjetoResponseModel>(`${this.apiUrl}/projetos/${encodeURIComponent(id)}`);
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
   * Devolução pelo professor responsável (fase 2a, §4.3): o projeto sai da fila
   * para ALTERACOES_SOLICITADAS com a justificativa que o autor vai ler.
   */
  devolverProjeto(
    id: string,
    payload: DevolverProjetoPayload
  ): Observable<ProjetoResponseModel> {
    return this.http.patch<ProjetoResponseModel>(
      `${this.apiUrl}/projetos/${encodeURIComponent(id)}/devolver`,
      payload
    );
  }

  /** Edição e reenvio pelo autor (fase 2a, §4.3): ALTERACOES_SOLICITADAS -> REENVIADO. */
  atualizarProjeto(
    id: string,
    payload: ProjetoCreatePayload
  ): Observable<ProjetoResponseModel> {
    return this.http.put<ProjetoResponseModel>(
      `${this.apiUrl}/projetos/${encodeURIComponent(id)}`,
      payload
    );
  }

  /** Exclusão da própria conta (fase 2b, §4.6). */
  excluirConta(): Observable<ExclusaoContaModel> {
    return this.http.delete<ExclusaoContaModel>(`${this.apiUrl}/usuarios/me`);
  }

  /**
   * Exclusão pelo autor (SPEC-exclusao-projetos fase 1).
   * AGUARDANDO_APROVACAO/REJEITADO → hard delete; APROVADO → soft delete (REMOVIDO),
   * com reversão possível por 14 dias.
   */
  excluirProjeto(id: string, payload: ExcluirProjetoPayload): Observable<ExclusaoProjetoModel> {
    return this.http.delete<ExclusaoProjetoModel>(
      `${this.apiUrl}/projetos/${encodeURIComponent(id)}`,
      { body: payload }
    );
  }

  reverterProjetoRemovido(id: string): Observable<ProjetoResponseModel> {
    return this.http.post<ProjetoResponseModel>(
      `${this.apiUrl}/projetos/${encodeURIComponent(id)}/reverter`,
      {}
    );
  }

  /** Removidos do usuário logado — sem PII, com a contagem regressiva. */
  listarProjetosRemovidos(): Observable<ProjetoRemovidoModel[]> {
    return this.http.get<ProjetoRemovidoModel[]>(`${this.apiUrl}/projetos/removidos`);
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
