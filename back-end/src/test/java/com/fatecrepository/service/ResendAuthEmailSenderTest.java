package com.fatecrepository.service;

import com.fatecrepository.config.ResendProperties;
import com.fatecrepository.exception.ServiceUnavailableException;
import com.fatecrepository.model.VerificationPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("ResendAuthEmailSender — envio de códigos de verificação")
class ResendAuthEmailSenderTest {

    private static final String URL_API = "https://api.resend.com/emails";

    private MockRestServiceServer servidor;
    private ResendProperties props;
    private ResendAuthEmailSender sender;

    @BeforeEach
    void setUp() {
        props = new ResendProperties();
        props.setApiKey("re_key_falso");
        props.setRemetente("naoresponda@cps.sp.gov.br");
        props.setOverrideDestinatario("");

        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        sender = new ResendAuthEmailSender(props, builder.build());
    }

    @Test
    @DisplayName("Envia código de cadastro via Resend com código no assunto e HTML")
    void enviaCodigoDeCadastro() {
        servidor.expect(requestTo(URL_API))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer re_key_falso"))
            .andExpect(jsonPath("$.from").value("naoresponda@cps.sp.gov.br"))
            .andExpect(jsonPath("$.to[0]").value("aluno@gmail.com"))
            .andExpect(jsonPath("$.subject").value(containsString("482910")))
            .andExpect(jsonPath("$.html").value(containsString("482910")))
            .andExpect(jsonPath("$.html").value(containsString("João Silva")))
            .andRespond(withSuccess());

        sender.enviarCodigoVerificacao(
            "aluno@gmail.com",
            "João Silva",
            "482910",
            VerificationPurpose.CADASTRO,
            15
        );

        servidor.verify();
    }

    @Test
    @DisplayName("Respeita overrideDestinatario em desenvolvimento")
    void respeitaOverrideDestinatario() {
        props.setOverrideDestinatario("caixa.dev@exemplo.com");

        servidor.expect(requestTo(URL_API))
            .andExpect(jsonPath("$.to[0]").value("caixa.dev@exemplo.com"))
            .andRespond(withSuccess());

        sender.enviarCodigoVerificacao(
            "docente@cps.sp.gov.br",
            "Docente",
            "123456",
            VerificationPurpose.RECUPERACAO_SENHA,
            15
        );

        servidor.verify();
    }

    @Test
    @DisplayName("Lança ServiceUnavailableException quando o Resend falha")
    void lancaServiceUnavailableQuandoResendFalha() {
        servidor.expect(requestTo(URL_API))
            .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> sender.enviarCodigoVerificacao(
            "aluno@gmail.com",
            "Aluno",
            "111222",
            VerificationPurpose.CADASTRO,
            15
        )).isInstanceOf(ServiceUnavailableException.class);

        servidor.verify();
    }
}
