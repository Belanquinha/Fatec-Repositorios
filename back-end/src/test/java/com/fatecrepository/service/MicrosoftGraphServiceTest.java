package com.fatecrepository.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * O Graph devolve um envelope {@code @odata.context} em toda resposta. Os testes de
 * {@link AuthServiceTest} usam mocks do {@link MicrosoftGraphService} e portanto não exercitam o
 * deserializador; estes testes cobrem o parse do payload real.
 */
class MicrosoftGraphServiceTest {

    private static final String PAYLOAD_GRAPH_REAL =
            "{\"@odata.context\":\"https://graph.microsoft.com/v1.0/$metadata#users/$entity\","
            + "\"businessPhones\":[],\"displayName\":\"Gabriel Souza\",\"givenName\":\"Gabriel\","
            + "\"jobTitle\":null,\"mail\":\"gabriel@aluno.sp.sp.gov.br\","
            + "\"mobilePhone\":null,\"officeLocation\":null,\"preferredLanguage\":\"pt-BR\","
            + "\"surname\":\"Souza\",\"userPrincipalName\":\"gabriel@aluno.sp.sp.gov.br\",\"id\":\"abc123\"}";

    private static final String PAYLOAD_SEM_MAIL =
            "{\"@odata.context\":\"https://graph.microsoft.com/v1.0/$metadata#users/$entity\","
            + "\"displayName\":\"Maria Oliveira\",\"mail\":null,"
            + "\"userPrincipalName\":\"maria@cps.sp.gov.br\",\"id\":\"def456\"}";

    private final MicrosoftGraphService.GraphUser parse(String payload) throws Exception {
        return new ObjectMapper().readValue(payload, MicrosoftGraphService.GraphUser.class);
    }

    @Test
    @DisplayName("Deve desserializar o payload real do Graph, ignorando @odata.context e os demais campos excedentes")
    void deveIgnorarCamposExcedentesDoGraph() throws Exception {
        MicrosoftGraphService.GraphUser user = parse(PAYLOAD_GRAPH_REAL);

        assertNotNull(user, "o parse não pode falhar por causa de campos que o Graph não mapeia");
        assertEquals("Gabriel Souza", user.getDisplayName());
        assertEquals("gabriel@aluno.sp.sp.gov.br", user.getMail());
        assertEquals("gabriel@aluno.sp.sp.gov.br", user.getUserPrincipalName());
        assertEquals("abc123", user.getId());
    }

    @Test
    @DisplayName("Deve mapear userPrincipalName quando a conta não expõe mail")
    void deveMapearUserPrincipalNameSemMail() throws Exception {
        MicrosoftGraphService.GraphUser user = parse(PAYLOAD_SEM_MAIL);

        assertEquals("maria@cps.sp.gov.br", user.getUserPrincipalName());
        org.junit.jupiter.api.Assertions.assertNull(user.getMail());
    }
}