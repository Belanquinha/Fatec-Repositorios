package com.fatecrepository.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fatecrepository.config.ResendProperties;
import com.fatecrepository.exception.ServiceUnavailableException;
import com.fatecrepository.model.VerificationPurpose;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Entrega síncrona dos códigos de verificação via API do Resend.
 *
 * <p>Diferente da notificação best-effort de projetos, a verificação de e-mail é parte
 * essencial da operação de cadastro/recuperação: se o provedor falhar, lança
 * {@link ServiceUnavailableException} para que o usuário receba feedback claro e possa
 * tentar novamente.
 */
@Slf4j
public class ResendAuthEmailSender implements AuthEmailSender {

    private static final String URL_API = "https://api.resend.com/emails";

    private final ResendProperties props;
    private final RestClient restClient;

    public ResendAuthEmailSender(ResendProperties props, RestClient restClient) {
        this.props = props;
        this.restClient = restClient;
    }

    @Override
    public void enviarCodigoVerificacao(
        String emailDestino,
        String nomeDestinatario,
        String codigo,
        VerificationPurpose finalidade,
        int validadeMinutos
    ) {
        if (emailDestino == null || emailDestino.isBlank()) {
            throw new IllegalArgumentException("E-mail de destino obrigatório");
        }

        String destinatarioReal = resolverDestinatario(emailDestino);
        EmailPayload payload = montarPayload(destinatarioReal, nomeDestinatario, codigo, finalidade, validadeMinutos);

        try {
            var resposta = restClient.post()
                .uri(URL_API)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();

            log.info("Código de verificação ({}) enviado via Resend para {} (status={})",
                finalidade, destinatarioReal, resposta.getStatusCode().value());
        } catch (RestClientException e) {
            log.error("Falha ao enviar código de verificação ({}) via Resend para {}: {}",
                finalidade, destinatarioReal, e.getMessage());
            throw new ServiceUnavailableException(
                "Não foi possível enviar o código de verificação por e-mail no momento. "
                    + "Verifique o endereço informado ou tente novamente em instantes.",
                e
            );
        }
    }

    private String resolverDestinatario(String emailOriginal) {
        String override = props.getOverrideDestinatario();
        if (override != null && !override.isBlank()) {
            log.debug("Override de desenvolvimento ativo: código de {} redirecionado para {}",
                emailOriginal, override);
            return override.trim();
        }
        return emailOriginal;
    }

    private EmailPayload montarPayload(
        String destinatario,
        String nome,
        String codigo,
        VerificationPurpose finalidade,
        int validadeMinutos
    ) {
        boolean cadastro = finalidade == VerificationPurpose.CADASTRO;
        String assunto = cadastro
            ? "Seu código de confirmação — Fatec Repositório: " + codigo
            : "Código para redefinição de senha — Fatec Repositório: " + codigo;

        String saudacao = (nome != null && !nome.isBlank())
            ? "Olá, <strong>" + escaparHtml(nome.trim()) + "</strong>,"
            : "Olá,";

        String descricao = cadastro
            ? "Recebemos sua solicitação de criação de conta no <strong>Fatec Repositório</strong>. "
                + "Utilize o código abaixo para confirmar a existência deste endereço de e-mail:"
            : "Recebemos uma solicitação para redefinir a senha da sua conta no <strong>Fatec Repositório</strong>. "
                + "Utilize o código abaixo para continuar:";

        String avisoIgnorar = cadastro
            ? "Se você não iniciou este cadastro, basta ignorar esta mensagem — nenhuma conta será criada."
            : "Se você não solicitou a redefinição de senha, ignore esta mensagem — sua senha atual permanece protegida.";

        String html = """
            <div style="font-family: Arial, Helvetica, sans-serif; max-width: 520px; margin: 0 auto; padding: 24px; border: 1px solid #DEE0E3; color: #202124; background-color: #FEFEFE;">
              <div style="border-bottom: 3px solid #DCA703; padding-bottom: 12px; margin-bottom: 20px;">
                <span style="font-size: 16px; font-weight: 700; letter-spacing: 0.02em; color: #202124;">FATEC REPOSITÓRIO</span>
              </div>
              <p style="font-size: 14px; line-height: 1.5; margin: 0 0 12px;">%s</p>
              <p style="font-size: 14px; line-height: 1.5; margin: 0 0 20px;">%s</p>
              <div style="background-color: #F4F6F9; border-left: 4px solid #DCA703; padding: 18px 20px; text-align: center; margin: 0 0 20px;">
                <span style="font-family: 'Courier New', Courier, monospace; font-size: 30px; font-weight: 700; letter-spacing: 8px; color: #202124;">%s</span>
              </div>
              <p style="font-size: 13px; color: #4D4D50; margin: 0 0 12px;">
                Este código expira em <strong>%d minutos</strong>. Nunca compartilhe este código com terceiros.
              </p>
              <p style="font-size: 12px; color: #6A6A6C; margin: 20px 0 0; border-top: 1px solid #DEE0E3; padding-top: 12px;">
                %s
              </p>
            </div>
            """.formatted(
            saudacao,
            descricao,
            escaparHtml(codigo),
            validadeMinutos,
            avisoIgnorar
        );

        return new EmailPayload(props.getRemetente(), List.of(destinatario), assunto, html);
    }

    private static String escaparHtml(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record EmailPayload(
        @JsonProperty("from") String from,
        @JsonProperty("to") List<String> to,
        @JsonProperty("subject") String subject,
        @JsonProperty("html") String html
    ) {}
}
