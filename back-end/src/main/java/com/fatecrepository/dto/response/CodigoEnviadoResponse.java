package com.fatecrepository.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodigoEnviadoResponse {

    private String mensagem;
    private String email;
    private int expiraEmSegundos;
    private int reenvioEmSegundos;
}
