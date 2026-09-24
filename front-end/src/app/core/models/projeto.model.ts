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

export interface ProfessorOption {
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
  estado: 'AGUARDANDO_APROVACAO' | 'APROVADO' | 'REJEITADO';
  motivoRejeicao?: string;
  emailProfessorResponsavel: string;
  instituicaoId: string;
  instituicaoNome: string;
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
