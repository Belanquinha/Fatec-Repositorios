package com.fatecrepository.service;

import com.fatecrepository.config.ResendProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * O client HTTP real é usado, mas preso a um {@link MockRestServiceServer}: nenhuma
 * chamada sai para a rede. Isso exercita o contrato de verdade (endpoint, Bearer,
 * payload JSON) — a entrega real fica para o teste manual do plano.
 */
@DisplayName("ResendEmailSender — contrato HTTP e falha")
class ResendEmailSenderTest {

    private static final String URL_API = "https://api.resend.com/emails";

    private MockRestServiceServer servidor;
    private ResendProperties props;
    private ResendEmailSender sender;

    @BeforeEach
    void setUp() {
        props = new ResendProperties();
        props.setApiKey("re_key_falso");
        props.setRemetente("naoresponda@cps.sp.gov.br");
        props.setUrlBaseFrontEnd("http://localhost:4200");
        props.setOverrideDestinatario("");

        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        sender = new ResendEmailSender(props, builder.build());
    }

    private ProjetoSubmetidoNotificacao notificacao() {
        return new ProjetoSubmetidoNotificacao(
            UUID.randomUUID(), "Projeto X", "Fatec Central",
            "professor.teste@cps.sp.gov.br", LocalDate.of(2026, 11, 16));
    }

    @Test
    @DisplayName("Envio usa endpoint do Resend, Bearer da key e destinatário do professor")
    void chamaResendComDestinatarioDoProjeto() {
        servidor.expect(requestTo(URL_API))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("Authorization", "Bearer re_key_falso"))
            .andExpect(jsonPath("$.from").value("naoresponda@cps.sp.gov.br"))
            .andExpect(jsonPath("$.to[0]").value("professor.teste@cps.sp.gov.br"))
            .andExpect(jsonPath("$.subject").value("Novo projeto aguardando sua avaliação: Projeto X"))
            .andRespond(withSuccess());

        sender.enviar(notificacao());

        servidor.verify();
    }

    @Test
    @DisplayName("Corpo traz título, instituição e data; sem deep link de aprovação")
    void corpoContemMetadadosMinimos() {
        servidor.expect(requestTo(URL_API))
            .andExpect(jsonPath("$.html").value(containsString("Fatec Central")))
            .andExpect(jsonPath("$.html").value(containsString("16/11/2026")))
            .andExpect(jsonPath("$.html").value(containsString("Projeto X")))
            // Aprovar é ação autenticada na plataforma: nada de link com token.
            .andExpect(jsonPath("$.html").value(not(containsString("/aprovar?"))))
            .andRespond(withSuccess());

        sender.enviar(notificacao());

        servidor.verify();
    }

    @Test
    @DisplayName("Override de desenvolvimento troca o destinatário (usado no teste manual)")
    void overrideTrocaDestinatario() {
        props.setOverrideDestinatario("dev@meu-email.com");
        servidor.expect(requestTo(URL_API))
            .andExpect(jsonPath("$.to[0]").value("dev@meu-email.com"))
            .andRespond(withSuccess());

        sender.enviar(notificacao());

        servidor.verify();
    }

    @Test
    @DisplayName("Erro HTTP do provedor vira NotificacaoEnvioException com status")
    void erroHttpViraExcecaoTipada() {
        servidor.expect(requestTo(URL_API)).andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> sender.enviar(notificacao()))
            .isInstanceOf(NotificacaoEnvioException.class)
            .satisfies(e -> assertThat(((NotificacaoEnvioException) e).getStatusHttp()).isEqualTo(403));
    }

    @Test
    @DisplayName("Falha de rede (sem resposta HTTP) também vira NotificacaoEnvioException")
    void erroDeRedeVirAExcecaoTipada() {
        servidor.expect(requestTo(URL_API)).andRespond(request -> {
            throw new java.io.IOException("connection reset");
        });

        assertThatThrownBy(() -> sender.enviar(notificacao()))
            .isInstanceOf(NotificacaoEnvioException.class)
            .satisfies(e -> assertThat(((NotificacaoEnvioException) e).getStatusHttp()).isNull());
    }

    @Test
    @DisplayName("Notificação sem destinatário é recusada antes de falar com o provedor")
    void semDestinatarioEhRecusado() {
        assertThatThrownBy(() -> sender.enviar(
            new ProjetoSubmetidoNotificacao(UUID.randomUUID(), "t", "i", "  ", LocalDate.now())))
            .isInstanceOf(IllegalArgumentException.class);

        // Nenhuma expectativa registrada = nenhuma requisição feita.
        servidor.verify();
    }
}
