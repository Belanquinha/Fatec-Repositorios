package com.fatecrepository.e2e;

import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.UserRole;
import com.fatecrepository.service.NotificacaoEmailSender;
import com.fatecrepository.service.NotificacaoEnvioException;
import com.fatecrepository.service.ProjetoSubmetidoNotificacao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

/**
 * Notificação de submissão, de ponta a ponta pelo HTTP real.
 *
 * <p>O RestClient do Resend é substituído por um spy do sender, então nenhuma
 * chamada sai para a rede nem depende de credencial — mas o gatilho pós-commit,
 * o payload montado pelo service e a persistência do projeto são exercitados de
 * verdade contra o PostgreSQL do perfil {@code test}.
 *
 * <p>O perfil de teste não tem {@code RESEND_API_KEY}, então o spy precisa
 * simular a integração ligada: sem isso o serviço nem chamaria o provedor (que é
 * exatamente o comportamento esperado em dev sem key — coberto nos testes
 * unitários). O alvo aqui é o gatilho e suas consequências.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Notificação de submissão ao professor responsável (E2E)")
class NotificacaoProjetoE2ETest extends ApiE2ETestSupport {

    private static final int ESPERA_MS = 5000;

    @MockitoSpyBean
    private NotificacaoEmailSender notificacaoEmailSender;

    @MockitoSpyBean
    private ResponseMapper responseMapper;

    private String tokenAluno;
    private UUID instituicaoId;
    private UUID cursoId;

    @BeforeEach
    void prepararCenario() {
        // Perfil de teste não configura key (bean no-op); liga só para este teste.
        doReturn(true).when(notificacaoEmailSender).habilitado();

        tokenAluno = tokenPara("Aluno Notificacao", "aluno.notificacao@aluno.cps.sp.gov.br", UserRole.ALUNO);
        var instituicao = criarInstituicao("400", "Fatec Notificacao");
        instituicaoId = instituicao.getId();
        cursoId = criarCursoVinculado(instituicao, "Análise e Desenvolvimento de Sistemas").getId();
    }

    private Map<String, Object> payloadValido() {
        Map<String, Object> body = new HashMap<>();
        body.put("titulo", "Projeto que dispara notificacao");
        body.put("descricaoCurta", "Verifica se o professor responsavel e avisado");
        body.put("conteudoEditorJs", "{\"blocks\":[]}");
        body.put("instituicaoId", instituicaoId.toString());
        body.put("cursoId", cursoId.toString());
        body.put("emailProfessorResponsavel", "professor.teste@cps.sp.gov.br");
        body.put("integrantes", List.of(Map.of("nome", "Ana Integrante")));
        return body;
    }

    @Test
    @DisplayName("Submeter projeto dispara exatamente uma notificação para o e-mail gravado")
    void submissaoDisparaUmaNotificacao() {
        ResponseEntity<String> response = post("/projetos", payloadValido(), tokenAluno);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        String id = json(response).path("id").asText();

        ArgumentCaptor<ProjetoSubmetidoNotificacao> captor =
            ArgumentCaptor.forClass(ProjetoSubmetidoNotificacao.class);
        // O envio é assíncrono (pós-commit); timeout cobre o agendamento pela pool.
        verificarExatamenteUma(captor);

        ProjetoSubmetidoNotificacao dados = captor.getValue();
        assertThat(dados.projetoId().toString()).isEqualTo(id);
        assertThat(dados.titulo()).isEqualTo("Projeto que dispara notificacao");
        assertThat(dados.emailProfessorResponsavel()).isEqualTo("professor.teste@cps.sp.gov.br");
        assertThat(dados.nomeInstituicao()).isEqualTo("Fatec Notificacao");

        // Independente do e-mail, o projeto está persistido como pendente.
        assertThat(projetoRepository.findById(dados.projetoId())).isPresent();
    }

    @Test
    @DisplayName("Falha do provedor não desfaz a submissão (segue AGUARDANDO_APROVACAO)")
    void falhaDeEnvioNaoDesfazSubmissao() {
        doThrow(new NotificacaoEnvioException("403 do provedor", 403, null))
            .when(notificacaoEmailSender).enviar(any(ProjetoSubmetidoNotificacao.class));

        ResponseEntity<String> response = post("/projetos", payloadValido(), tokenAluno);

        assertThat(response.getStatusCode().value())
            .as("A confirmação do aluno é independente do resultado do e-mail")
            .isEqualTo(201);
        assertThat(texto(response, "estado")).isEqualTo("AGUARDANDO_APROVACAO");

        // Continua na fila do professor, mesmo sem alerta entregue.
        var projeto = projetoRepository.findById(UUID.fromString(json(response).path("id").asText()))
            .orElseThrow();
        assertThat(projeto.getEstado().name()).isEqualTo("AGUARDANDO_APROVACAO");
        assertThat(projeto.getEmailProfessorResponsavel()).isEqualTo("professor.teste@cps.sp.gov.br");

        // A tentativa aconteceu uma vez; não há retentativa automática.
        org.mockito.Mockito.verify(notificacaoEmailSender, after(ESPERA_MS).times(1))
            .enviar(any(ProjetoSubmetidoNotificacao.class));
    }

    @Test
    @DisplayName("Rollback da transação não dispara notificação (afterCommit não roda)")
    void rollbackNaoDisparaNotificacao() {
        // Falha depois do save e do registro do afterCommit: a transação faz
        // rollback e a notificação não pode acontecer — é a garantia central.
        doThrow(new IllegalStateException("falha após o save para forçar rollback"))
            .when(responseMapper).toProjetoResponse(any());

        ResponseEntity<String> response = post("/projetos", payloadValido(), tokenAluno);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(projetoRepository.count())
            .as("Rollback não pode deixar o projeto pendente")
            .isZero();
        org.mockito.Mockito.verify(notificacaoEmailSender, after(ESPERA_MS).never())
            .enviar(any(ProjetoSubmetidoNotificacao.class));
    }

    private void verificarExatamenteUma(ArgumentCaptor<ProjetoSubmetidoNotificacao> captor) {
        try {
            // `after` (não `timeout`): espera a janela inteira e só então conta as
            // invocações — assim uma segunda notificação no período é detectada.
            org.mockito.Mockito.verify(notificacaoEmailSender, after(ESPERA_MS).times(1))
                .enviar(captor.capture());
        } catch (AssertionError e) {
            throw new AssertionError(
                "Esperado exatamente 1 notificação; capturadas=" + captor.getAllValues().size(), e);
        }
    }
}
