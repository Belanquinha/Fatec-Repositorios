package com.fatecrepository.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fatecrepository.config.ResendProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Entrega via API do Resend (https://resend.com), usando o {@link RestClient} do
 * Spring — sem dependência nova no {@code pom.xml}.
 *
 * <p>Best-effort por desenho: uma única tentativa, sem retry. Falha de envio é
 * exceção para o chamador, que apenas loga. O projeto já está commitado nesse
 * ponto, então falha de e-mail nunca desfaz submissão.
 *
 * <p>Não é {@code @Component}: quem decide entre esta implementação e o no-op de
 * "sem key" é {@code NotificacaoEmailConfig}, para nunca haver dois beans do tipo
 * {@code NotificacaoEmailSender} em disputa na injeção.
 */
@Slf4j
public class ResendEmailSender implements NotificacaoEmailSender {

    private static final String URL_API = "https://api.resend.com/emails";
    private static final DateTimeFormatter DATA_FORMATO =
        DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.of("pt", "BR"));

    private final ResendProperties props;
    private final RestClient restClient;

    public ResendEmailSender(ResendProperties props, RestClient restClient) {
        this.props = props;
        this.restClient = restClient;
    }

    @Override
    public boolean habilitado() {
        return true;
    }

    @Override
    public void enviar(ProjetoSubmetidoNotificacao notificacao) {
        if (notificacao == null || notificacao.emailProfessorResponsavel() == null
            || notificacao.emailProfessorResponsavel().isBlank()) {
            throw new IllegalArgumentException("Notificação sem destinatário");
        }

        String destinatario = resolverDestinatario(notificacao.emailProfessorResponsavel());
        EmailRequest corpo = montarRequisicao(destinatario, notificacao);

        try {
            var resposta = restClient.post()
                .uri(URL_API)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .toBodilessEntity();

            log.info("Notificação de projeto enviada: projetoId={}, destinatario={}, status={}",
                notificacao.projetoId(), destinatario, resposta.getStatusCode().value());
        } catch (RestClientException e) {
            // Loga status/corpo quando o RestClient os expõe; nunca loga a chave.
            log.error("Falha ao enviar notificação do projeto {} para {}: {}",
                notificacao.projetoId(), destinatario, e.getMessage());
            throw new NotificacaoEnvioException(
                "Provedor de e-mail recusou/falhou a notificação do projeto " + notificacao.projetoId(),
                extrairStatus(e), e);
        }
    }

    /**
     * Desenvolvimento: redireciona a entrega para a caixa do desenvolvedor, porque
     * a conta de teste do professor não tem caixa acessível. Em produção este
     * campo é bloqueado na subida do contexto ({@code ResendEmailConfig}).
     */
    private String resolverDestinatario(String emailResponsavel) {
        String override = props.getOverrideDestinatario();
        if (override != null && !override.isBlank()) {
            log.debug("Override de desenvolvimento ativo: entrega de {} redirectada para {}",
                emailResponsavel, override);
            return override.trim();
        }
        return emailResponsavel;
    }

    private EmailRequest montarRequisicao(String destinatario, ProjetoSubmetidoNotificacao n) {
        String assunto = "Novo projeto aguardando sua avaliação: " + n.titulo();
        // A URL também passa por escape: ela é configurável e vai para um atributo
        // href e para o texto do link — sem escape, um valor com '"' quebra o HTML.
        String urlBaseFrontEnd = escaparHtml(props.getUrlBaseFrontEnd());
        String html = """
            <div style="font-family: Arial, sans-serif; font-size: 14px; color: #222;">
              <p>Olá,</p>
              <p>Um projeto foi submetido e aguarda a sua avaliação como professor responsável.</p>
              <p>
                <strong>Título:</strong> %s<br>
                <strong>Instituição:</strong> %s<br>
                <strong>Data de submissão:</strong> %s
              </p>
              <p>Entre na plataforma para ver os detalhes e aprovar ou rejeitar:</p>
              <p><a href="%s">%s</a></p>
              <p>Este é um aviso automático. A aprovação só acontece dentro da plataforma.</p>
            </div>
            """.formatted(
            escaparHtml(n.titulo()),
            escaparHtml(n.nomeInstituicao() == null ? "" : n.nomeInstituicao()),
            n.dataSubmissao() == null ? "" : n.dataSubmissao().format(DATA_FORMATO),
            urlBaseFrontEnd,
            urlBaseFrontEnd);

        return new EmailRequest(props.getRemetente(), destinatario, assunto, html);
    }

    private static String escaparHtml(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** Recupera o status HTTP quando o RestClient trouxe um erro tipado. */
    private static Integer extrairStatus(RestClientException e) {
        if (e instanceof org.springframework.web.client.HttpStatusCodeException http) {
            return http.getStatusCode().value();
        }
        return null;
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record EmailRequest(
        @JsonProperty("from") String from,
        @JsonProperty("to") List<String> to,
        @JsonProperty("subject") String subject,
        @JsonProperty("html") String html
    ) {
        private EmailRequest(String from, String to, String subject, String html) {
            this(from, List.of(to), subject, html);
        }
    }
}
