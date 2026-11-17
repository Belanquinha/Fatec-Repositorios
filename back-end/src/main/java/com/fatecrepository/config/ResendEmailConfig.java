package com.fatecrepository.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Trava de produção do override de destinatário.
 *
 * <p>O override existe para desenvolvimento (a conta de teste do professor não tem
 * caixa de entrada acessível). Deixá-lo ligado em produção faria TODAS as
 * submissões irem para a caixa do desenvolvedor — o professor real nunca saberia
 * do projeto. Como o silêncio esconde esse erro de configuração, a subida do
 * contexto aborta.
 *
 * <p>A regra é fail-closed: o override só é aceito quando um profile explicitamente
 * de desenvolvimento ({@code dev*} ou {@code local*}) está ativo. Sem profile ativo,
 * com {@code prod} ou com qualquer outro nome, o contexto aborta — não se admite
 * que "ausência de profile" signifique "pode redirecionar". Para usar o override,
 * declare o profile no ambiente (no compose, {@code SPRING_PROFILES_ACTIVE}).
 *
 * <p>Quando a integração está habilitada, o remetente também é validado: {@code from}
 * vazio seria recusado pelo provedor em todas as notificações.
 */
@Configuration
@RequiredArgsConstructor
public class ResendEmailConfig implements InitializingBean {

    private final ResendProperties props;
    private final Environment environment;

    @Override
    public void afterPropertiesSet() {
        validarOverride();
        validarRemetente();
    }

    private void validarOverride() {
        String override = props.getOverrideDestinatario();
        if (override == null || override.isBlank()) {
            return;
        }
        for (String perfil : environment.getActiveProfiles()) {
            if (perfilDesenvolvimento(perfil)) {
                return;
            }
        }
        throw new IllegalStateException(
            "app.resend.override-destinatario esta preenchido, mas nenhum profile de "
                + "desenvolvimento (dev/local) esta ativo. Este override redirecionaria "
                + "todas as notificacoes de submissao para uma caixa de desenvolvimento; "
                + "ative um profile dev/local ou remova o override antes de subir.");
    }

    private static boolean perfilDesenvolvimento(String perfil) {
        if (perfil == null) {
            return false;
        }
        String normalizado = perfil.toLowerCase();
        return normalizado.startsWith("dev") || normalizado.startsWith("local");
    }

    private void validarRemetente() {
        if (!props.habilitado()) {
            return;
        }
        if (props.getRemetente() == null || props.getRemetente().isBlank()) {
            throw new IllegalStateException(
                "app.resend.remetente esta vazio com a integracao habilitada: nenhuma "
                    + "notificacao seria entregue (o provedor recusa 'from' vazio).");
        }
    }
}
