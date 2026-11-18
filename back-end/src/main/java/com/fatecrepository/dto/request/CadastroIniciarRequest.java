package com.fatecrepository.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CadastroIniciarRequest {

    @NotBlank(message = "Informe seu nome completo")
    @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres")
    private String nome;

    @NotBlank(message = "Informe o e-mail")
    @Email(message = "Formato de e-mail inválido")
    @Size(max = 254, message = "E-mail muito longo")
    private String email;

    @NotBlank(message = "Crie uma senha")
    @Size(min = 8, max = 100, message = "A senha deve ter entre 8 e 100 caracteres")
    private String senha;
}
