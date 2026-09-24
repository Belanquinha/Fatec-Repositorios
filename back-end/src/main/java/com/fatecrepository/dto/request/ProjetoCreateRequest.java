package com.fatecrepository.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjetoCreateRequest {

    @NotBlank(message = "Título do projeto é obrigatório")
    private String titulo;

    @NotBlank(message = "Descrição curta é obrigatória")
    @Size(max = 144, message = "A descrição curta deve ter no máximo 144 caracteres")
    private String descricaoCurta;

    private String conteudoEditorJs;

    private String linkRepositorio;

    private String imagemCapaUrl;

    private List<String> palavrasChave;

    private Integer anoPublicado;

    @NotNull(message = "Instituição é obrigatória")
    private UUID instituicaoId;

    @NotBlank(message = "E-mail do professor responsável é obrigatório")
    @Email(message = "E-mail do professor responsável inválido")
    private String emailProfessorResponsavel;

    @NotEmpty(message = "O projeto deve ter ao menos um integrante")
    @Valid
    private List<IntegranteRequest> integrantes;
}
