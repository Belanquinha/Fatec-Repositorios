package com.fatecrepository.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "InstituicaoRequest", description = "Dados para cadastro de uma instituição e do seu gestor")
public class InstituicaoRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Schema(description = "Nome da instituição", example = "Fatec Piranga")
    private String nome;

    @NotBlank(message = "CNPJ é obrigatório")
    @Pattern(regexp = "\\d{14}", message = "CNPJ deve ter 14 dígitos")
    @Schema(description = "CNPJ da instituição", example = "00000000000100")
    private String cnpj;

    @Schema(description = "Endereço da instituição", example = "Rua Frei João, 59 - Vila Nair")
    private String endereco;

    @NotBlank(message = "Cidade é obrigatória")
    @Schema(description = "Cidade da instituição", example = "São Paulo")
    private String cidade;

    @NotBlank(message = "Estado é obrigatório")
    @Schema(description = "Estado da instituição", example = "SP")
    private String estado;

    @NotNull(message = "Gestor é obrigatório")
    @Valid
    @Schema(description = "Dados do gestor responsável pela instituição")
    private GestorRequest gestor;
}
