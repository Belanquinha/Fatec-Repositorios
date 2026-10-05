package com.fatecrepository.seed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjetoSeedDto {

    private String titulo;
    private String descricaoCurta;
    private String conteudoEditorJs;
    private String linkRepositorio;
    private String imagemCapaUrl;
    private String imagemSeedClasspath;
    private List<String> palavrasChave;
    private Integer anoPublicado;
    private String estado;
    private String emailProfessorResponsavel;
    private String codigoUnidadeInstituicao;
    private String autorEmail;
    private String autorNome;
    private List<IntegranteSeedDto> integrantes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IntegranteSeedDto {
        private String nome;
        private String linkLinkedin;
    }
}
