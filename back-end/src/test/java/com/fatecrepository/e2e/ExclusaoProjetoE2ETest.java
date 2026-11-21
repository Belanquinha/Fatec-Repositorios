package com.fatecrepository.e2e;

import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import com.fatecrepository.service.ProjetoPurgeService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SPEC-exclusao-projetos fase 1 — exclusão, anonimização, reversão e purge.
 *
 * <p>Os cenários cobrem AC1..AC9: hard delete de não-público, soft delete de
 * APROVADO com anonimização, visibilidade de REMOVIDO, reversão dentro e fora do
 * prazo, purge de linha e arquivos e a regra de proteção do arquivo de seed.
 */
@DisplayName("SPEC fase 1 — Exclusão e anonimização de projeto")
class ExclusaoProjetoE2ETest extends ApiE2ETestSupport {

    private static final String MOTIVO_PADRAO = "PEDIDO_AUTOR";
    /** Nomes de arquivo (sem /uploads/): a regra de proteção só apaga nomes UUID. */
    private static final String ARQ_CAPA = "22222222-2222-2222-2222-222222222222.png";
    private static final String ARQ_CAPA_THUMB = "22222222-2222-2222-2222-222222222222-thumb.png";
    private static final String ARQ_INLINE = "11111111-1111-1111-1111-111111111111.png";
    /** URLs enviadas no conteúdo/capa do projeto. */
    private static final String URL_CAPA = "/uploads/" + ARQ_CAPA;
    private static final String URL_INLINE = "/uploads/" + ARQ_INLINE;
    /** Arquivo solto do hard delete (não referenciado no conteúdo). */
    private static final String ARQ_AVULSO = "33333333-3333-3333-3333-333333333333.png";
    /** Seed (nome não-UUID): a regra de proteção o preserva. */
    private static final String ARQ_SEED = "golden-maker-capa.png";

    @Autowired
    private ProjetoPurgeService purgeService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private User aluno;
    private String tokenAluno;
    private User responsavel;
    private String tokenResponsavel;
    private User outroAluno;
    private String tokenOutroAluno;
    private com.fatecrepository.model.Instituicao instituicao;
    private UUID cursoId;

    @BeforeEach
    void prepararCenario() {
        aluno = criarUsuario("Aluno Exclusao", "aluno.exclusao@aluno.cps.sp.gov.br", UserRole.ALUNO);
        tokenAluno = tokenDe(aluno);
        responsavel = criarUsuario("Prof Exclusao", "prof.exclusao@cps.sp.gov.br", UserRole.PROFESSOR);
        tokenResponsavel = tokenDe(responsavel);
        outroAluno = criarUsuario("Aluno Outro Exclusao", "aluno.outro.exclusao@aluno.cps.sp.gov.br", UserRole.ALUNO);
        tokenOutroAluno = tokenDe(outroAluno);
        instituicao = criarInstituicao("920", "Fatec Exclusao");
        cursoId = criarCursoVinculado(instituicao, "Análise e Desenvolvimento de Sistemas").getId();
    }

    private Map<String, Object> payloadProjeto() {
        Map<String, Object> body = new HashMap<>();
        body.put("titulo", "Projeto para Exclusao");
        body.put("descricaoCurta", "Projeto usado na suíte de exclusão");
        body.put("conteudoEditorJs", "{\"blocks\":[{\"type\":\"image\",\"data\":{\"file\":{\"url\":\"" + URL_INLINE + "\"}}}]}");
        body.put("linkRepositorio", "https://github.com/fatec/projeto-exclusao");
        body.put("imagemCapaUrl", URL_CAPA);
        body.put("palavrasChave", List.of("TCC", "Repositório"));
        body.put("instituicaoId", instituicao.getId().toString());
        body.put("cursoId", cursoId.toString());
        body.put("emailProfessorResponsavel", responsavel.getEmail());
        body.put("integrantes", List.of(
            Map.of("nome", "Ana Integrante", "linkLinkedin", "https://linkedin.com/in/ana"),
            Map.of("nome", "Bruno Integrante")
        ));
        return body;
    }

    private String submeterPendente() {
        return submeterPendente(null, null);
    }

    /**
     * Submete com capa/inline exclusivos do teste. O disco é compartilhado, então o
     * arquivo tem que ser referenciado pelo conteúdo para o delete achá-lo — e ser
     * único para não colidir com o de outro teste da suíte.
     */
    private String submeterPendente(String nomeCapa, String nomeInline) {
        Map<String, Object> body = payloadProjeto();
        if (nomeCapa != null) {
            body.put("imagemCapaUrl", "/uploads/" + nomeCapa);
        }
        if (nomeInline != null) {
            body.put("conteudoEditorJs", "{\"blocks\":[{\"type\":\"image\",\"data\":{\"file\":{\"url\":\"/uploads/"
                + nomeInline + "\"}}}]}");
        }
        ResponseEntity<String> response = post("/projetos", body, tokenAluno);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        return json(response).path("id").asText();
    }

    private String submeterEAprovar() {
        String id = submeterPendente();
        assertThat(patch("/projetos/" + id + "/aprovar", Map.of(), tokenResponsavel).getStatusCode().value())
            .isEqualTo(200);
        return id;
    }

    private String submeterERejeitar() {
        String id = submeterPendente();
        assertThat(patch("/projetos/" + id + "/rejeitar",
            Map.of("motivo", "Fora do escopo do curso"), tokenResponsavel).getStatusCode().value())
            .isEqualTo(200);
        return id;
    }

    private void criarArquivoDeUpload(String nomeArquivo) throws Exception {
        Path uploadDir = Path.of(System.getProperty("java.io.tmpdir"), "fatecrepository-e2e-uploads");
        Files.createDirectories(uploadDir);
        Files.writeString(uploadDir.resolve(nomeArquivo), "conteudo-de-teste");
    }

    private Path arquivoDeUpload(String nomeArquivo) {
        return Path.of(System.getProperty("java.io.tmpdir"), "fatecrepository-e2e-uploads", nomeArquivo);
    }

    @Nested
    @DisplayName("Hard delete de não-público pelo autor")
    class HardDelete {

        @Test
        @DisplayName("AC1: AGUARDANDO_APROVACAO é apagado com arquivos e some de /meus e /publicos")
        void apagaPendente() throws Exception {
            String id = submeterPendente(ARQ_AVULSO, ARQ_INLINE);
            criarArquivoDeUpload(ARQ_AVULSO);
            criarArquivoDeUpload(ARQ_INLINE);
            assertThat(arquivoDeUpload(ARQ_AVULSO)).exists();

            ResponseEntity<String> response = delete("/projetos/" + id,
                Map.of("motivoRemocao", MOTIVO_PADRAO), tokenAluno);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(texto(response, "estado")).isEqualTo("EXCLUIDO");
            assertThat(get("/projetos/" + id, tokenAluno).getStatusCode().value()).isEqualTo(404);
            assertThat(jsonList(get("/projetos/meus", tokenAluno))).isEmpty();
            assertThat(get("/projetos/publicos").getBody()).doesNotContain(id);
            assertThat(arquivoDeUpload(ARQ_AVULSO))
                .doesNotExist();
            assertThat(arquivoDeUpload(ARQ_INLINE))
                .doesNotExist();
        }

        @Test
        @DisplayName("AC2: REJEITADO é apagado pelo autor")
        void apagaRejeitado() {
            String id = submeterERejeitar();

            ResponseEntity<String> response = delete("/projetos/" + id,
                Map.of("motivoRemocao", MOTIVO_PADRAO), tokenAluno);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(texto(response, "estado")).isEqualTo("EXCLUIDO");
            assertThat(get("/projetos/" + id, tokenAluno).getStatusCode().value()).isEqualTo(404);
            assertThat(jsonList(get("/projetos/meus", tokenAluno))).isEmpty();
        }
    }

    @Nested
    @DisplayName("Soft delete de APROVADO e anonimização")
    class SoftDelete {

        @Test
        @DisplayName("AC4: APROVADO vira REMOVIDO, some do público e o detalhe fica 404")
        void removeAprovado() {
            String id = submeterEAprovar();
            assertThat(get("/projetos/" + id).getStatusCode().value()).isEqualTo(200);

            ResponseEntity<String> response = delete("/projetos/" + id,
                Map.of("motivoRemocao", MOTIVO_PADRAO), tokenAluno);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(texto(response, "estado")).isEqualTo("REMOVIDO");
            assertThat(json(response).has("removidoEm")).isTrue();
            assertThat(json(response).has("dadosOriginais")).isFalse();
            assertThat(get("/projetos/" + id).getStatusCode().value()).isEqualTo(404);
            assertThat(get("/projetos/" + id, tokenAluno).getStatusCode().value()).isEqualTo(404);
            assertThat(jsonList(get("/projetos/publicos")))
                .noneSatisfy(p -> assertThat(p.get("id")).isEqualTo(id));
            assertThat(jsonList(get("/projetos/meus", tokenAluno))).isEmpty();
        }

        @Test
        @DisplayName("AC4: PII é anonimizada imediatamente e nunca aparece em /removidos")
        void anonimizaPii() throws Exception {
            String id = submeterEAprovar();
            criarArquivoDeUpload(ARQ_CAPA);

            assertThat(delete("/projetos/" + id, Map.of("motivoRemocao", MOTIVO_PADRAO), tokenAluno)
                .getStatusCode().value()).isEqualTo(200);

            List<Map<String, Object>> removidos = jsonList(get("/projetos/removidos", tokenAluno));
            assertThat(removidos).hasSize(1);
            assertThat(removidos.get(0))
                .containsEntry("id", id)
                .containsEntry("estadoAnterior", "APROVADO")
                .containsEntry("motivoRemocaoGenerico", MOTIVO_PADRAO);
            assertThat(removidos.get(0).keySet()).doesNotContain(
                "emailProfessorResponsavel", "conteudoEditorJs", "imagemCapaUrl", "dadosOriginais", "linkRepositorio");
            assertThat(json(removidosResponse(tokenAluno)).toString())
                .doesNotContain(responsavel.getEmail())
                .doesNotContain("Ana Integrante")
                .doesNotContain("https://linkedin.com/in/ana");
        }

        @Test
        @DisplayName("Soft delete mantém arquivos para a reversão")
        void mantemArquivos() throws Exception {
            String id = submeterEAprovar();
            criarArquivoDeUpload(ARQ_CAPA);

            delete("/projetos/" + id, Map.of("motivoRemocao", MOTIVO_PADRAO), tokenAluno);

            assertThat(arquivoDeUpload(ARQ_CAPA))
                .exists();
        }
    }

    @Nested
    @DisplayName("Autorização do DELETE")
    class AutorizacaoDelete {

        @Test
        @DisplayName("AC3: outro aluno recebe 403")
        void outroAutor403() {
            String id = submeterEAprovar();

            assertAcessoNegado(delete("/projetos/" + id, Map.of("motivoRemocao", MOTIVO_PADRAO), tokenOutroAluno));
        }

        @Test
        @DisplayName("GET /projetos/removidos sem token recebe 401 (não pode virar público)")
        void removidosSemToken401() {
            submeterEAprovar();

            assertNaoAutorizado(get("/projetos/removidos"));
        }

        @Test
        @DisplayName("AC3: professor responsável não é autor e recebe 403")
        void professor403() {
            String id = submeterEAprovar();

            assertAcessoNegado(delete("/projetos/" + id, Map.of("motivoRemocao", MOTIVO_PADRAO), tokenResponsavel));
        }

        @Test
        @DisplayName("AC3: sem token recebe 401")
        void semToken401() {
            String id = submeterEAprovar();

            assertNaoAutorizado(delete("/projetos/" + id, Map.of("motivoRemocao", MOTIVO_PADRAO), null));
        }

        @Test
        @DisplayName("AC3: projeto inexistente recebe 404")
        void inexistente404() {
            assertThat(delete("/projetos/" + UUID.randomUUID(),
                Map.of("motivoRemocao", MOTIVO_PADRAO), tokenAluno).getStatusCode().value()).isEqualTo(404);
        }

        @Test
        @DisplayName("DELETE duas vezes devolve 404")
        void deleteDuplicado404() {
            String id = submeterEAprovar();
            delete("/projetos/" + id, Map.of("motivoRemocao", MOTIVO_PADRAO), tokenAluno);

            assertThat(delete("/projetos/" + id, Map.of("motivoRemocao", MOTIVO_PADRAO), tokenAluno)
                .getStatusCode().value()).isEqualTo(404);
        }
    }

    @Nested
    @DisplayName("Reversão pelo autor")
    class Reversao {

        @Test
        @DisplayName("AC5: reverte dentro do prazo e volta a APROVADO e ao /publicos")
        void reverteDentroDoPrazo() {
            String id = submeterEAprovar();
            delete("/projetos/" + id, Map.of("motivoRemocao", MOTIVO_PADRAO), tokenAluno);

            ResponseEntity<String> response = post("/projetos/" + id + "/reverter", Map.of(), tokenAluno);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(texto(response, "estado")).isEqualTo("APROVADO");
            assertThat(get("/projetos/" + id).getStatusCode().value()).isEqualTo(200);
            assertThat(jsonList(get("/projetos/publicos"))).anySatisfy(p -> assertThat(p.get("id")).isEqualTo(id));
            assertThat(jsonList(get("/projetos/removidos", tokenAluno))).isEmpty();
        }

        @Test
        @DisplayName("AC5: reversão restaura integrantes, e-mail e arquivos")
        void reversaoRestauraPii() {
            String id = submeterEAprovar();
            delete("/projetos/" + id, Map.of("motivoRemocao", MOTIVO_PADRAO), tokenAluno);

            ResponseEntity<String> response = post("/projetos/" + id + "/reverter", Map.of(), tokenAluno);

            assertThat(json(response).path("emailProfessorResponsavel").asText())
                .isEqualTo(responsavel.getEmail());
            assertThat(json(response).path("integrantes")).hasSize(2);
            assertThat(json(response).path("integrantes").get(0).path("nome").asText())
                .isEqualTo("Ana Integrante");
            assertThat(json(response).path("imagemCapaUrl").asText())
                .isEqualTo(URL_CAPA);
            assertThat(json(response).path("linkRepositorio").asText())
                .isEqualTo("https://github.com/fatec/projeto-exclusao");
        }

        @Test
        @DisplayName("AC6: removidoEm com mais de 14 dias devolve 409")
        void reversaoForaDoPrazo409() {
            String id = submeterEAprovar();
            delete("/projetos/" + id, Map.of("motivoRemocao", MOTIVO_PADRAO), tokenAluno);
            forcarRemovidoEm(id, java.time.LocalDateTime.now().minusDays(15));

            ResponseEntity<String> response = post("/projetos/" + id + "/reverter", Map.of(), tokenAluno);

            assertThat(response.getStatusCode().value()).isEqualTo(409);
            assertThat(response.getBody()).contains("Prazo de reversão expirado");
        }

        @Test
        @DisplayName("Reverter projeto que não está REMOVIDO devolve 404")
        void reverterNaoRemovido404() {
            String id = submeterEAprovar();

            assertThat(post("/projetos/" + id + "/reverter", Map.of(), tokenAluno).getStatusCode().value())
                .isEqualTo(404);
        }

        @Test
        @DisplayName("Outro usuário não reverte projeto removido (403)")
        void outroUsuarioReverter403() {
            String id = submeterEAprovar();
            delete("/projetos/" + id, Map.of("motivoRemocao", MOTIVO_PADRAO), tokenAluno);

            assertAcessoNegado(post("/projetos/" + id + "/reverter", Map.of(), tokenOutroAluno));
        }
    }

    @Nested
    @DisplayName("Purge de removidos expirados")
    class Purge {

        @Test
        @DisplayName("AC7: purge apaga linha e arquivos com mais de 14 dias")
        void purgeRemoveExpirado() throws Exception {
            String id = submeterEAprovar();
            delete("/projetos/" + id, Map.of("motivoRemocao", MOTIVO_PADRAO), tokenAluno);
            forcarRemovidoEm(id, java.time.LocalDateTime.now().minusDays(15));
            criarArquivoDeUpload(ARQ_CAPA);
            criarArquivoDeUpload(ARQ_INLINE);
            assertThat(arquivoDeUpload(ARQ_CAPA)).exists();
            assertThat(arquivoDeUpload(ARQ_INLINE)).exists();

            int removidos = purgeService.purgarExpirados();

            assertThat(removidos).isEqualTo(1);
            assertThat(get("/projetos/" + id, tokenAluno).getStatusCode().value()).isEqualTo(404);
            assertThat(jsonList(get("/projetos/removidos", tokenAluno))).isEmpty();
            assertThat(arquivoDeUpload(ARQ_CAPA))
                .doesNotExist();
            assertThat(arquivoDeUpload(ARQ_INLINE))
                .doesNotExist();
        }

        @Test
        @DisplayName("AC7: purge não remove removido com menos de 14 dias")
        void purgeMantemDentroDoPrazo() {
            String id = submeterEAprovar();
            delete("/projetos/" + id, Map.of("motivoRemocao", MOTIVO_PADRAO), tokenAluno);

            int removidos = purgeService.purgarExpirados();

            assertThat(removidos).isZero();
            assertThat(jsonList(get("/projetos/removidos", tokenAluno))).hasSize(1);
        }

        @Test
        @DisplayName("AC8: arquivo de seed com nome não-UUID nunca é apagado")
        void naoApagaArquivoDeSeed() throws Exception {
            String id = submeterEAprovar();
            delete("/projetos/" + id, Map.of("motivoRemocao", MOTIVO_PADRAO), tokenAluno);
            forcarRemovidoEm(id, java.time.LocalDateTime.now().minusDays(15));
            criarArquivoDeUpload(ARQ_SEED);

            purgeService.purgarExpirados();

            assertThat(arquivoDeUpload(ARQ_SEED)).exists();
        }

        @Test
        @DisplayName("AC9: purge remove capa, thumb e imagem inline")
        void purgeRemoveCapaThumbInline() throws Exception {
            String id = submeterEAprovar();
            delete("/projetos/" + id, Map.of("motivoRemocao", MOTIVO_PADRAO), tokenAluno);
            forcarRemovidoEm(id, java.time.LocalDateTime.now().minusDays(15));
            criarArquivoDeUpload(ARQ_CAPA);
            criarArquivoDeUpload(ARQ_CAPA_THUMB);
            criarArquivoDeUpload(ARQ_INLINE);
            assertThat(arquivoDeUpload(ARQ_CAPA)).exists();
            assertThat(arquivoDeUpload(ARQ_CAPA_THUMB)).exists();
            assertThat(arquivoDeUpload(ARQ_INLINE)).exists();

            purgeService.purgarExpirados();

            assertThat(arquivoDeUpload(ARQ_CAPA))
                .doesNotExist();
            assertThat(arquivoDeUpload(ARQ_CAPA_THUMB))
                .doesNotExist();
            assertThat(arquivoDeUpload(ARQ_INLINE))
                .doesNotExist();
        }
    }

    private ResponseEntity<String> removidosResponse(String token) {
        return get("/projetos/removidos", token);
    }

    private void forcarRemovidoEm(String id, LocalDateTime removidoEm) {
        transactionTemplate.executeWithoutResult(status -> {
            var projeto = entityManager.find(com.fatecrepository.model.Projeto.class, UUID.fromString(id));
            assertThat(projeto).isNotNull();
            projeto.setRemovidoEm(removidoEm);
            entityManager.flush();
            entityManager.clear();
        });
    }
}
