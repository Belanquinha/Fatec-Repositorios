package com.fatecrepository.dto.request;

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
public class CadastroConfirmarRequest {

    @NotBlank(message = "Informe o e-mail")
    @Email(message = "Formato de e-mail inválido")
    @Size(max = 254, message = "E-mail muito longo")
    private String email;

    @NotBlank(message = "Informe o código de verificação")
    @Pattern(regexp = "^\\d{6}$", message = "O código deve conter exatamente 6 dígitos numéricos")
    private String codigo;
}
