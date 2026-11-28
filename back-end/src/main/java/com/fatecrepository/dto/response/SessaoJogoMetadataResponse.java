package com.fatecrepository.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessaoJogoMetadataResponse {
    private UUID projetoId;
    private String titulo;
    private String nomeOriginal;
    private long tamanhoBytes;
    private String sha256;
    private String executavelPrincipal;
}
