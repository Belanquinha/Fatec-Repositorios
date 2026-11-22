package com.fatecrepository.e2e;

import com.fatecrepository.model.EmailVerificationCode;
import com.fatecrepository.model.Instituicao;
import com.fatecrepository.model.ProfessorInstituicao;
import com.fatecrepository.model.User;
import com.fatecrepository.model.VerificationPurpose;
import com.fatecrepository.repository.ProfessorInstituicaoRepository;
import com.fatecrepository.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SPEC-exclusao-projetos fase 2b — exclusão da própria conta (AC17).
 *
 * <p>Cenário: um professor que (a) tem projeto aprovado, (b) tem projeto pendente
 * próprio e (c) é responsável por um projeto de outro autor. Ao excluir a conta,
 * o aprovado permanece com autoria anônima, o pendente próprio é apagado e o
 * projeto em que era responsável volta ao autor sem e-mail de responsável.
 */
@DisplayName("SPEC fase 2b — Exclusão da própria conta")
class ExclusaoContaE2ETest extends ApiE2ETestSupport {

    @Autowired
    private ProfessorInstituicaoRepository vinculoRepository;

    private User professor;
    private String tokenProfessor;
    private User outroProfessor;
    private String tokenOutroProfessor;
    private User aluno;
    private String tokenAluno;
    private Instituicao instituicao;
    private UUID cursoId;

    @BeforeEach
    void prepararCenario() {
        professor = criarUsuario("Prof Conta", "prof.conta@cps.sp.gov.br", com.fatecrepository.model.UserRole.PROFESSOR);
        tokenProfessor = tokenDe(professor);
        outroProfessor = criarUsuario("Prof Avaliador", "prof.avaliador@cps.sp.gov.br", com.fatecrepository.model.UserRole.PROFESSOR);
        tokenOutroProfessor = tokenDe(outroProfessor);
        aluno = criarUsuario("Aluno Conta", "aluno.conta@aluno.cps.sp.gov.br", com.fatecrepository.model.UserRole.ALUNO);
        tokenAluno = tokenDe(aluno);
        instituicao = criarInstituicao("922", "Fatec Conta");
        cursoId = criarCursoVinculado(instituicao, "Análise e Desenvolvimento de Sistemas").getId();
    }

    private Map<String, Object> payloadProjeto(String titulo, String responsavelEmail) {
        Map<String, Object> body = new HashMap<>();
        body.put("titulo", titulo);
        body.put("descricaoCurta", "Projeto usado na suíte de exclusão de conta");
        body.put("conteudoEditorJs", "{\"blocks\":[]}");
        body.put("linkRepositorio", "https://github.com/fatec/projeto-conta");
        body.put("palavrasChave", List.of("TCC"));
        body.put("instituicaoId", instituicao.getId().toString());
        body.put("cursoId", cursoId.toString());
        body.put("emailProfessorResponsavel", responsavelEmail);
        body.put("integrantes", List.of(Map.of("nome", "Ana Integrante")));
        return body;
    }

    private String submeter(String token, String titulo, String responsavelEmail) {
        ResponseEntity<String> response = post("/projetos", payloadProjeto(titulo, responsavelEmail), token);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        return json(response).path("id").asText();
    }

    private void criarVinculoProfessor() {
        ProfessorInstituicao vinculo = new ProfessorInstituicao();
        vinculo.setUsuario(professor);
        vinculo.setInstituicao(instituicao);
        vinculoRepository.saveAndFlush(vinculo);
    }

    private void criarCodigoVerificacao() {
        EmailVerificationCode codigo = new EmailVerificationCode();
        codigo.setEmail(professor.getEmail());
        codigo.setFinalidade(VerificationPurpose.RECUPERACAO_SENHA);
        codigo.setCodigoHash("hash-de-teste");
        codigo.setExpiraEm(LocalDateTime.now().plusMinutes(15));
        verificationCodeRepository.saveAndFlush(codigo);
    }

    @Test
    @DisplayName("AC17: conta anonimizada, obra mantida, não-aprovados excluídos e fila devolvida")
    void excluirConta() {
        // (a) aprovado do próprio professor — deve permanecer.
        String idAprovado = submeter(tokenProfessor, "Projeto Aprovado do Prof", outroProfessor.getEmail());
        assertThat(patch("/projetos/" + idAprovado + "/aprovar", Map.of(), tokenOutroProfessor)
            .getStatusCode().value()).isEqualTo(200);

        // (b) pendente do próprio professor — deve ser excluído definitivamente.
        String idPendenteProprio = submeter(tokenProfessor, "Projeto Pendente do Prof", outroProfessor.getEmail());

        // (c) pendente de outro autor em que o professor é responsável — volta ao autor.
        String idResponsavel = submeter(tokenAluno, "Projeto do Aluno", professor.getEmail());

        criarVinculoProfessor();
        criarCodigoVerificacao();

        ResponseEntity<String> response = delete("/usuarios/me", tokenProfessor);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(json(response).has("mensagem")).isTrue();

        // Conta anonimizada no banco.
        User removido = userRepository.findById(professor.getId()).orElseThrow();
        assertThat(removido.getNome()).isEqualTo("[conta removida]");
        assertThat(removido.getEmail()).isNull();
        assertThat(removido.getSenhaHash()).isNull();
        assertThat(removido.getFotoUrl()).isNull();

        // Obra aprovada preservada, agora com autoria anônima.
        assertThat(get("/projetos/" + idAprovado).getStatusCode().value()).isEqualTo(200);
        assertThat(jsonList(get("/projetos/publicos")))
            .anySatisfy(p -> {
                assertThat(p.get("id")).isEqualTo(idAprovado);
                assertThat(p.get("autorNome")).isEqualTo("[conta removida]");
            });

        // Não-aprovado do usuário desapareceu.
        assertThat(get("/projetos/" + idPendenteProprio).getStatusCode().value()).isEqualTo(404);
        assertThat(projetoRepository.existsById(UUID.fromString(idPendenteProprio))).isFalse();

        // Pendente em que era responsável: devolvido ao aluno, e-mail limpo.
        ResponseEntity<String> detalhe = get("/projetos/" + idResponsavel, tokenAluno);
        assertThat(detalhe.getStatusCode().value()).isEqualTo(200);
        assertThat(texto(detalhe, "estado")).isEqualTo("ALTERACOES_SOLICITADAS");
        assertThat(texto(detalhe, "motivoDevolucao")).isEqualTo("responsável removido");
        assertThat(json(detalhe).path("emailProfessorResponsavel").isNull()).isTrue();

        // Vínculos e códigos de verificação removidos.
        assertThat(vinculoRepository.buscarComInstituicao(professor.getId())).isEmpty();
        assertThat(verificationCodeRepository
            .findTopByEmailAndFinalidadeOrderByCriadoEmDesc(
                professor.getEmail(), VerificationPurpose.RECUPERACAO_SENHA))
            .isEmpty();

        // Token antigo invalidado.
        assertNaoAutorizado(get("/projetos/meus", tokenProfessor));
    }

    @Test
    @DisplayName("DELETE /usuarios/me sem token recebe 401")
    void semToken401() {
        assertNaoAutorizado(delete("/usuarios/me", null));
    }
}
