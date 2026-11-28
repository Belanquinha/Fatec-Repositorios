package com.fatecrepository.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JogoUploadResponse {
    private String arquivoJogoId;
    private String nomeOriginal;
    private long tamanhoBytes;
    private String sha256;
    private List<String> executaveis;
}
