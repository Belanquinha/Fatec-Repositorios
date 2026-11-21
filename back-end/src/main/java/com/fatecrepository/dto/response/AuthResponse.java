package com.fatecrepository.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    /**
     * ID da conta. Sem ele o front-end não sabe se um card é do usuário logado —
     * a ação de excluir o próprio projeto ficaria oculta (o `podeExcluir()` compara
     * `autorId` com `usuario.id`, e um `id` ausente nunca é igual a nenhum UUID).
     */
    private UUID id;
    private String accessToken;
    private String tokenType;
    private long expiresInSeconds;
    private String nome;
    private String email;
    private String fotoUrl;
    private String role;

    public AuthResponse(String accessToken, String tokenType, long expiresInSeconds,
                        String nome, String email, String fotoUrl, String role) {
        this(null, accessToken, tokenType, expiresInSeconds, nome, email, fotoUrl, role);
    }
}
