package com.fatecrepository.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "InstituicaoRequest", description = "Dados para cadastro de uma instituição do catálogo oficial")
public class InstituicaoRequest {

    @NotBlank(message = "Código da unidade é obrigatório")
    @Schema(description = "Código oficial da unidade (chave de negócio)", example = "004")
    private String codigoUnidade;

    @NotBlank(message = "Nome é obrigatório")
    @Schema(description = "Nome da instituição", example = "Fatec Ipiranga")
    private String nome;

    @Schema(description = "Endereço da instituição", example = "Rua Frei João, 59 - Vila Nair")
    private String endereco;

    @Schema(description = "Cidade da instituição", example = "São Paulo")
    private String cidade;

    @Schema(description = "Estado da instituição", example = "SP")
    private String estado;

    @Schema(description = "Região administrativa", example = "Capital")
    private String regiaoAdministrativa;

    @Schema(description = "CNPJ da instituição", example = "62.823.257/0016-87")
    private String cnpj;

    @Schema(description = "Telefones da instituição", example = "(19) 3406-3297 / 3406-5776")
    private String telefone;

    @Schema(description = "Site oficial", example = "http://www.fatec.edu.br")
    private String site;

    @Schema(description = "Link do logotipo", example = "https://bkpsitecpsnew.blob.core.windows.net/...")
    private String linkLogo;

    @Schema(description = "Se a instituição está ativa no catálogo", example = "true")
    private Boolean ativo;
}