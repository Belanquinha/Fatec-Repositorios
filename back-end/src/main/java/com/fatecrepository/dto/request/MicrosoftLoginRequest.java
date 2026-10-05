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

    /**
     * ID token do fluxo OIDC, opcional por enquanto. É o artefato que a auditoria OIDC manda
     * validar para provar autenticação, ao contrário do access token — que é credencial para o
     * Microsoft Graph, não para a nossa API.
     */
    private String idToken;

    private String nome;

    private String email;

    private String fotoUrl;
}
