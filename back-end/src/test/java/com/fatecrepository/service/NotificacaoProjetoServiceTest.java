package com.fatecrepository.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Regras da primeira versão da notificação: uma tentativa, sem retry, e falha de
 * e-mail não propaga (quem chamou já commitou o projeto).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificacaoProjetoService — best-effort pós-commit")
class NotificacaoProjetoServiceTest {

    @Mock
    private NotificacaoEmailSender sender;

    private NotificacaoProjetoService service;

    @BeforeEach
    void setUp() {
        service = new NotificacaoProjetoService(sender);
    }

    private ProjetoSubmetidoNotificacao notificacao() {
        return new ProjetoSubmetidoNotificacao(
            UUID.randomUUID(), "Projeto X", "Fatec Central",
            "professor.teste@cps.sp.gov.br", LocalDate.of(2026, 11, 16));
    }

    @Test
    @DisplayName("Envia exatamente uma vez quando habilitado")
    void enviaUmaUnicaTentativa() {
        org.mockito.Mockito.lenient().when(sender.habilitado()).thenReturn(true);

        service.notificarProjetoSubmetido(notificacao());

        verify(sender, times(1)).enviar(any(ProjetoSubmetidoNotificacao.class));
        // "exatamente uma" é a asserção de que não há retry: uma segunda chamada
        // aqui falharia o teste. Não existe laço de repetição no serviço.
    }

    @Test
    @DisplayName("Falha do provedor é engolida (não propaga para quem submeteu)")
    void falhaNaoPropaga() {
        org.mockito.Mockito.lenient().when(sender.habilitado()).thenReturn(true);
        doThrow(new NotificacaoEnvioException("403 do provedor", 403, null))
            .when(sender).enviar(any(ProjetoSubmetidoNotificacao.class));

        // Não deve lançar: o projeto já está commitado neste ponto.
        service.notificarProjetoSubmetido(notificacao());

        verify(sender, times(1)).enviar(any());
    }

    @Test
    @DisplayName("Sem key (integração desligada) nem chega a chamar o provedor")
    void desligadoNaoChamaProvedor() {
        org.mockito.Mockito.lenient().when(sender.habilitado()).thenReturn(false);

        service.notificarProjetoSubmetido(notificacao());

        verify(sender, never()).enviar(any());
    }

    @Test
    @DisplayName("Notificação nula é ignorada sem NPE")
    void notificacaoNulaEhIgnorada() {
        service.notificarProjetoSubmetido(null);

        verify(sender, never()).enviar(any());
    }

    @Test
    @DisplayName("A notificação carrega o e-mail do professor responsável gravado no projeto")
    void destinatarioVeioDoProjeto() {
        org.mockito.Mockito.lenient().when(sender.habilitado()).thenReturn(true);
        ProjetoSubmetidoNotificacao n = notificacao();

        service.notificarProjetoSubmetido(n);

        ArgumentCaptor<ProjetoSubmetidoNotificacao> captor =
            ArgumentCaptor.forClass(ProjetoSubmetidoNotificacao.class);
        verify(sender).enviar(captor.capture());
        assertThat(captor.getValue().emailProfessorResponsavel())
            .isEqualTo("professor.teste@cps.sp.gov.br");
        assertThat(captor.getValue().titulo()).isEqualTo("Projeto X");
        assertThat(captor.getValue().nomeInstituicao()).isEqualTo("Fatec Central");
        assertThat(captor.getValue().dataSubmissao()).isEqualTo(LocalDate.of(2026, 11, 16));
    }
}
