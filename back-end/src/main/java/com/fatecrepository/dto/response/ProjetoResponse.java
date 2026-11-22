package com.fatecrepository.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjetoResponse {
    private UUID id;
    private String titulo;
    private String descricaoCurta;
    private String conteudoEditorJs;
    private String linkRepositorio;
    private String imagemCapaUrl;
    private List<String> palavrasChave;
    private Integer anoPublicado;
    private String estado;
    private String motivoRejeicao;
    private String emailProfessorResponsavel;
    /** ID da conta do professor responsável (quando cadastrado), permitindo linkar o perfil público sem expor o e-mail. */
    private UUID professorResponsavelId;
    /** Nome da conta do professor responsável (visão pública/LGPD — nunca expor e-mail na vitrine). */
    private String nomeProfessorResponsavel;
    private UUID instituicaoId;
    private String instituicaoNome;
    private UUID cursoId;
    private String cursoNome;
    private UUID autorId;
    private String autorNome;
    private List<IntegranteResponse> integrantes;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
    // --- Fase 2: devolução/edição ---
    /** Justificativa do professor; visível ao autor em ALTERACOES_SOLICITADAS. */
    private String motivoDevolucao;
    private LocalDateTime devolvidoEm;
    private LocalDateTime reenviadoEm;
}
