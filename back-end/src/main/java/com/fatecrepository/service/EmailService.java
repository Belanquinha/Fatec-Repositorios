package com.fatecrepository.service;

import com.fatecrepository.model.Projeto;
import com.fatecrepository.model.User;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.List;

/**
 * Envio de e-mails assíncrono/não crítico do back-end.
 *
 * <p>Envia para o professor responsável logo após a submissão de um projeto.
 * A mensagem é "melhor-esforço": se o SMTP falhar, o erro é logado e a
 * submissão do aluno <b>nunca</b> é bloqueada. Isso evita que um problema
 * pontual de e-mail impeça o registro do projeto.</p>
 *
 * <p>Envio feito no back-end (não no front-end): o naveador não envia e-mail
 * para destinatários arbitrários por segurança (CORS + exposição de
 * credenciais), e o professor já está gravado no modelo {@link Projeto} via
 * {@code emailProfessorResponsavel}, então não precisamos de mais um cadastro.</p>
 */
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;

    public EmailService(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    /**
     * Notifica o professor responsável logo após o aluno submeter o projeto.
     *
     * @param projeto   projeto recém-criado (já persistido em {@code salvo}).
     * @param autor     autor (aluno) do projeto.
     */
    public void enviarNovoProjeto(Projeto projeto, User autor) {
        String professorEmail = projeto.getEmailProfessorResponsavel();
        if (professorEmail == null || professorEmail.isBlank()) {
            log.warn("Projeto {} sem e-mail do professor responsável; pulando notificação", projeto.getId());
            return;
        }

        // Prefere um endereço "oficial" do projeto; se não foi declarado, usa
        // a conta SMTP ativa. Qualquer "from" que não seja um endereço com domínio
        // é substituído pelo e-mail do professor, evitando messagens marcadas como
        // spam ou rejeitadas pelo relay.
        String from = System.getenv("EMAIL_FROM");
        if (from == null || from.isBlank()) {
            from = System.getenv("MAIL_USERNAME");
        }
        if (from == null || !from.contains("@") || from.isBlank()) {
            from = professorEmail;
        }

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(professorEmail);
            helper.setSubject("[Fatec Repositório] Novo projeto submetido para avaliação");
            helper.setText(buildHtmlBody(projeto, autor), true); // true = HTML
            javaMailSender.send(message);
            log.info("E-mail de notificação enviado para {} sobre o projeto {}", professorEmail, projeto.getId());
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail de notificação para o professor {} sobre o projeto {} — a submissão continuou normalmente",
                    professorEmail, projeto.getId(), e);
        }
    }

    /** Junta palavras-chave em uma string, ignorando nulos ou vazios por segurança. */
    private static String joinPalavrasChave(List<String> palavras) {
        if (palavras == null || palavras.isEmpty()) {
            return "—";
        }
        return palavras.stream()
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .collect(Collectors.joining(", "));
    }

    private String buildHtmlBody(Projeto projeto, User autor) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><body style=\"font-family: 'Segoe UI', Arial, sans-serif; color:#1f2937; background:#f3f4f6; margin:0; padding:24px;\">")
                .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px; margin:0 auto; background:#ffffff; border-radius:10px; overflow:hidden; box-shadow:0 1px 4px rgba(0,0,0,.12);\">")
                .append("<tr style=\"background:#1d4ed8; color:#ffffff;\"><td style=\"padding:18px 22px; font-size:18px; font-weight:600;\">📦 Fatec Repositório</td></tr>")
                .append("<tr><td style=\"padding:22px;\">")
                .append("<h2 style=\"margin:0 0 8px; color:#111827;\">Novo projeto submetido</h2>")
                .append("<p><b>Título:</b> ").append(escapeHtml(projeto.getTitulo())).append("</p>")
                .append("<p><b>Descrição:</b> ").append(escapeHtml(projeto.getDescricaoCurta())).append("</p>")
                .append("<p><b>Autor:</b> ").append(escapeHtml(autor.getNome())).append(" (")
                .append(escapeHtml(autor.getEmail())).append(")</p>")
                .append("<p><b>Instituição:</b> ")
                .append(projeto.getInstituicao() != null ? escapeHtml(projeto.getInstituicao().getNome()) : "—")
                .append("</p>")
                .append("<p><b>Palavras-chave:</b> ")
                .append(joinPalavrasChave(projeto.getPalavrasChave()))
                .append("</p>")
                .append("<p><b>Link do repositório:</b> ")
                .append(projeto.getLinkRepositorio() == null || projeto.getLinkRepositorio().isBlank()
                        ? "—"
                        : "<a href=\"").append(escapeHtml(projeto.getLinkRepositorio())).append("\" style=\"color:#2563eb; text-decoration:underline;\">link</a>")
                .append("</p>")
                .append("<p><b>Estado:</b> <span style=\"color:#b91c1c;\">").append(projeto.getEstado() != null ? projeto.getEstado().name() : "—").append("</span></p>")
                .append("<p style=\"color:#6b7280; font-size:12px; margin-top:18px; border-top:1px solid #e5e7eb; padding-top:12px;\">"
                        + "Enviado automaticamente pelo Fatec Repositório &middot; "
                        + "Este e-mail foi gerado após a submissão do projeto.")
                .append("</td></tr>")
                .append("</table>")
                .append("</body></html>");
        return sb.toString();
    }

    /** Escapa texto bruto para uso dentro de conteúdo HTML. */
    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
