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
  estado: 'SUBMETIDO' | 'AGUARDANDO_APROVACAO' | 'ALTERACOES_SOLICITADAS' | 'REENVIADO' | 'APROVADO' | 'REJEITADO';
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
}

export interface UploadResponseModel {
  url: string;
  nomeArquivo: string;
}
