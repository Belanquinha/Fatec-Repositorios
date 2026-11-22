export interface IntegranteModel {
  id?: string;
  nome: string;
  linkLinkedin?: string;
}

export interface InstituicaoOption {
  id: string;
  codigoUnidade: string;
  nome: string;
  cidade?: string;
  estado?: string;
  regiaoAdministrativa?: string;
  endereco?: string;
  site?: string;
  ativo: boolean;
}

export interface CursoOption {
  id: string;
  nome: string;
}

export interface ProfessorOption {
  id: string;
  nome: string;
  // LGPD: a lista (/professores?q) expõe só id+nome; o e-mail vem só do
  // detalhe do professor selecionado ou da digitação livre (convidado).
  email?: string;
}

export interface ProfessorDetalhe {
  id: string;
  nome: string;
  email: string;
}

export interface ProjetoCreatePayload {
  titulo: string;
  descricaoCurta: string;
  conteudoEditorJs?: string;
  linkRepositorio?: string;
  imagemCapaUrl?: string;
  palavrasChave?: string[];
  anoPublicado?: number;
  instituicaoId: string;
  cursoId: string;
  emailProfessorResponsavel: string;
  integrantes: {
    nome: string;
    linkLinkedin?: string;
  }[];
}

export interface ProjetoResponseModel {
  id: string;
  titulo: string;
  descricaoCurta: string;
  conteudoEditorJs: string;
  linkRepositorio: string;
  imagemCapaUrl: string;
  palavrasChave: string[];
  anoPublicado: number;
  estado: 'SUBMETIDO' | 'AGUARDANDO_APROVACAO' | 'ALTERACOES_SOLICITADAS' | 'REENVIADO' | 'APROVADO' | 'REJEITADO' | 'REMOVIDO';
  motivoRejeicao?: string;
  /**
   * E-mail do professor responsável. Na vitrine (projeto APROVADO) o back-end
   * entrega null por LGPD; a visão interna (autor/professor responsável lendo
   * pendente ou rejeitado) mantém o valor operacional.
   */
  emailProfessorResponsavel: string;
  /** ID da conta do professor responsável (quando existente), para linkar o perfil público sem expor e-mail. */
  professorResponsavelId?: string | null;
  /** Nome da conta do professor responsável — o que a vitrine exibe (nunca o e-mail). */
  nomeProfessorResponsavel?: string | null;
  instituicaoId: string;
  instituicaoNome: string;
  cursoId?: string;
  cursoNome?: string;
  autorId: string;
  autorNome: string;
  integrantes: IntegranteModel[];
    criadoEm: string;
    atualizadoEm: string;
    /** Fase 2 (SPEC-exclusao-projetos): justificativa do professor na devolução. */
    motivoDevolucao?: string | null;
    devolvidoEm?: string | null;
    reenviadoEm?: string | null;
  }

export interface UploadResponseModel {
  url: string;
  nomeArquivo: string;
}

/** Corpo do DELETE /projetos/{id}: só categoria genérica, nunca texto livre com PII. */
export interface ExcluirProjetoPayload {
  motivoRemocao: MotivoRemocao;
}

export type MotivoRemocao = 'PEDIDO_AUTOR' | 'DADOS_PESSOAIS' | 'ERRO_AUTORIA' | 'OUTRO';

/** Resposta do DELETE: confirma a operação sem devolver PII. */
export interface ExclusaoProjetoModel {
  id: string;
  titulo: string;
  estado: string;
  removidoEm?: string;
}

/** Corpo do PATCH /projetos/{id}/devolver: texto livre obrigatório. */
export interface DevolverProjetoPayload {
  motivo: string;
}

/** Resposta do DELETE /usuarios/me. */
export interface ExclusaoContaModel {
  mensagem: string;
}

/** Removidos do usuário logado — sem PII, com a contagem regressiva. */
export interface ProjetoRemovidoModel {
  id: string;
  titulo: string;
  estadoAnterior?: string;
  removidoEm?: string;
  motivoRemocaoGenerico?: string;
  diasParaExpirar: number;
}

export interface PerfilInstituicaoModel {
  id: string;
  codigoUnidade: string;
  nome: string;
  endereco?: string;
  cidade?: string;
  estado?: string;
  regiaoAdministrativa?: string;
  telefone?: string;
  site?: string;
  linkLogo?: string;
  ativo: boolean;
  cursos: CursoOption[];
  projetos: ProjetoResponseModel[];
}

export interface PerfilPublicoUsuarioModel {
  id: string;
  nome: string;
  role: 'ALUNO' | 'PROFESSOR' | 'ADMIN';
  fotoUrl?: string;
  instituicoes: InstituicaoOption[];
  projetos: ProjetoResponseModel[];
}

