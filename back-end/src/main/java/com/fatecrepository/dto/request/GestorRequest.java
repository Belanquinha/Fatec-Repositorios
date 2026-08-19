package com.fatecrepository.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "GestorRequest", description = "Dados do gestor da instituição")
public class GestorRequest {

    @NotBlank(message = "Nome do gestor é obrigatório")
    @Schema(description = "Nome completo do gestor", example = "José da Silva")
    private String nome;

    @NotBlank(message = "Email do gestor é obrigatório")
    @Email(message = "Email do gestor inválido")
    @Schema(description = "Email do gestor", example = "jose@extensao.com")
    private String email;

    @NotBlank(message = "Telefone do gestor é obrigatório")
    @Pattern(regexp = "\\d{10,11}", message = "Telefone do gestor deve ter 10 ou 11 dígitos")
    @Schema(description = "Telefone do gestor", example = "11999999999")
    private String telefone;

    @NotBlank(message = "Senha do gestor é obrigatória")
    @Size(min = 6, message = "Senha do gestor deve ter ao menos 6 caracteres")
    @Schema(description = "Senha de acesso do gestor", example = "123456")
    private String senha;
}
