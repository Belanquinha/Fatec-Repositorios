package com.fatecrepository.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MicrosoftLoginRequest {

    @NotBlank(message = "Token Microsoft é obrigatório")
    private String accessToken;

    private String nome;

    private String email;

    private String fotoUrl;
}
