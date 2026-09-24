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
    private UUID instituicaoId;
    private String instituicaoNome;
    private UUID autorId;
    private String autorNome;
    private List<IntegranteResponse> integrantes;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}
