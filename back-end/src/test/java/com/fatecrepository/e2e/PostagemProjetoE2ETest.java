package com.fatecrepository.e2e;

import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stories 3.1 e 3.2 — Postagem de projetos pelo aluno.
 *
 * <p>AC 3.1: "o projeto é salvo diretamente com estado AGUARDANDO_APROVACAO
 *            (não existe estado rascunho)"   → NFR5
 * AC 3.1: "os integrantes são salvos associados ao projeto (Projeto 1 ── N Integrante)
 *            com nome e linkLinkedin"
 * AC 3.2: "o atributo emailProfessorResponsavel é persistido obrigatoriamente"
 * AC 3.2: "tentar publicar sem emailProfessorResponsavel falha"
 *
 * <p>NFR5 é a regra mais EasyJS de errar: se alguém reintroduzir um estado RASCUNHO,
 * o aluno vê o projeto como salvo mas ele nunca chega à fila do professor. Por isso
 * o estado inicial é asseverado explicitamente.
 */
@DisplayName("Stories 3.1 / 3.2 — Postagem de projeto")
class PostagemProjetoE2ETest extends ApiE2ETestSupport {

    private String tokenAluno;
    private UUID instituicaoId;

    @BeforeEach
    void prepararCenario() {
        tokenAluno = tokenPara("Aluno E2E", "aluno.postagem@aluno.cps.sp.gov.br", UserRole.ALUNO);
        instituicaoId = criarInstituicao("001", "Fatec E2E Central").getId();
    }

    private Map<String, Object> payloadValido() {
        Map<String, Object> body = new HashMap<>();
        body.put("titulo", "Plataforma de Repositório Acadêmico");
        body.put("descricaoCurta", "Sistema web para disseminating projetos da Fatec");
        body.put("conteudoEditorJs", "{\"blocks\":[{\"type\":\"paragraph\",\"data\":{\"text\":\"Corpo\"}}]}");
        body.put("linkRepositorio", "https://github.com/fatec/repositorio");
        body.put("palavrasChave", List.of("Java", "Spring Boot", "Angular"));
        body.put("anoPublicado", 2026);
        body.put("instituicaoId", instituicaoId.toString());
        body.put("emailProfessorResponsavel", "professor@cps.sp.gov.br");
        body.put("integrantes", List.of(
            Map.of("nome", "Ana Integrante", "linkLinkedin", "https://linkedin.com/in/ana"),
            Map.of("nome", "Bruno Integrante", "linkLinkedin", "https://linkedin.com/in/bruno")
        ));
        return body;
    }

    @Nested
    @DisplayName("AC 3.1 / NFR5 — nasce direto em AGUARDANDO_APROVACAO")
    class EstadoInicial {

        @Test
        @DisplayName("Projeto criado nasce em AGUARDANDO_APROVACAO (não existe rascunho)")
        void nasceAguardandoAprovacao() {
            ResponseEntity<String> response = post("/projetos", payloadValido(), tokenAluno);

            assertThat(response.getStatusCode().value()).isEqualTo(201);
            assertThat(texto(response, "estado")).isEqualTo("AGUARDANDO_APROVACAO");
            assertThat(json(response).path("motivoRejeicao").isNull()).isTrue();
        }

        @Test
        @DisplayName("Não existe caminho para criar projeto já aprovado ou rejeitado")
        void naoPermiteEscolherEstado() {
            Map<String, Object> body = payloadValido();
            // O front-end tenta forçar o estado final.
            body.put("estado", "APROVADO");
            body.put("motivoRejeicao", "aprovado por engano");

            ResponseEntity<String> response = post("/projetos", body, tokenAluno);

            assertThat(response.getStatusCode().value()).isEqualTo(201);
            assertThat(texto(response, "estado"))
                .as("O estado é sempre decidido pelo servidor")
                .isEqualTo("AGUARDANDO_APROVACAO");
            assertThat(json(response).path("motivoRejeicao").isNull()).isTrue();
        }

        @Test
        @DisplayName("O estado inicial persiste no banco, não só na resposta HTTP")
        void estadoPersisteNoBanco() {
            String id = json(post("/projetos", payloadValido(), tokenAluno)).path("id").asText();

            var projeto = projetoRepository.findById(UUID.fromString(id)).orElseThrow();

            assertThat(projeto.getEstado().name()).isEqualTo("AGUARDANDO_APROVACAO");
            assertThat(projeto.getMotivoRejeicao()).isNull();
        }
    }

    @Nested
    @DisplayName("AC 3.1 — integrantes são salvos com nome e linkLinkedin")
    class Integrantes {

        @Test
        @DisplayName("Todos os integrantes enviados são persistidos e associados ao projeto")
        void integrantesPersistidos() {
            String id = json(post("/projetos", payloadValido(), tokenAluno)).path("id").asText();

            ResponseEntity<String> consulta = get("/projetos/" + id);
            JsonNode integrantes = json(consulta).path("integrantes");

            assertThat(integrantes).hasSize(2);
            assertThat(integrantes.get(0).path("nome").asText()).isEqualTo("Ana Integrante");
            assertThat(integrantes.get(0).path("linkLinkedin").asText()).isEqualTo("https://linkedin.com/in/ana");
            assertThat(integrantes.get(1).path("nome").asText()).isEqualTo("Bruno Integrante");
        }

        @Test
        @DisplayName("Integrante sem LinkedIn é aceito (campo opcional)")
        void integranteSemLinkedin() {
            Map<String, Object> body = payloadValido();
            body.put("integrantes", List.of(Map.of("nome", "Carla Sem LinkedIn")));

            String id = json(post("/projetos", body, tokenAluno)).path("id").asText();

            JsonNode integrantes = json(get("/projetos/" + id)).path("integrantes");
            assertThat(integrantes).hasSize(1);
            assertThat(integrantes.get(0).path("nome").asText()).isEqualTo("Carla Sem LinkedIn");
            assertThat(integrantes.get(0).path("linkLinkedin").isNull()).isTrue();
        }

        @Test
        @DisplayName("Lista vazia de integrantes devolve 400")
        void listaVaziaDeIntegrantes() {
            Map<String, Object> body = payloadValido();
            body.put("integrantes", List.of());

            ResponseEntity<String> response = post("/projetos", body, tokenAluno);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(json(response).path("erros").has("integrantes")).isTrue();
        }

        @Test
        @DisplayName("Integrante sem nome devolve 400")
        void integranteSemNome() {
            Map<String, Object> body = payloadValido();
            body.put("integrantes", List.of(Map.of("linkLinkedin", "https://linkedin.com/in/sem-nome")));

            ResponseEntity<String> response = post("/projetos", body, tokenAluno);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("AC 3.2 — emailProfessorResponsavel é obrigatório")
    class EmailProfessorResponsavelObrigatorio {

        @Test
        @DisplayName("Publicar sem emailProfessorResponsavel falha com 400")
        void semEmailProfessorResponsavel() {
            Map<String, Object> body = payloadValido();
            body.remove("emailProfessorResponsavel");

            ResponseEntity<String> response = post("/projetos", body, tokenAluno);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(json(response).path("erros").has("emailProfessorResponsavel")).isTrue();
        }

        @Test
        @DisplayName("emailProfessorResponsavel em branco falha com 400")
        void emailEmBranco() {
            Map<String, Object> body = payloadValido();
            body.put("emailProfessorResponsavel", "   ");

            assertThat(post("/projetos", body, tokenAluno).getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("emailProfessorResponsavel inválido falha com 400")
        void emailInvalido() {
            Map<String, Object> body = payloadValido();
            body.put("emailProfessorResponsavel", "isto-nao-e-email");

            assertThat(post("/projetos", body, tokenAluno).getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("E-mail persistido é normalizado para minúsculas (AD-6)")
        void emailNormalizadoParaMinusculas() {
            Map<String, Object> body = payloadValido();
            body.put("emailProfessorResponsavel", "Professor.Convidado@CPS.SP.GOV.BR");

            String id = json(post("/projetos", body, tokenAluno)).path("id").asText();

            assertThat(json(get("/projetos/" + id)).path("emailProfessorResponsavel").asText())
                .isEqualTo("professor.convidado@cps.sp.gov.br");
        }

        @Test
        @DisplayName("E-mail preenchido com espaços é recusado na fronteira (@Email), não normalizado")
        void emailComEspacosNosExtremos() {
            Map<String, Object> body = payloadValido();
            body.put("emailProfessorResponsavel", "  professor@cps.sp.gov.br  ");

            ResponseEntity<String> response = post("/projetos", body, tokenAluno);

            // O @Email do Bean Validation atua antes do service: espaço nas bordas
            // é rejeitado em vez de silenciosamente aparado.
            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(json(response).path("erros").has("emailProfessorResponsavel")).isTrue();
        }

        @Test
        @DisplayName("E-mail de professor fora do tenant é aceito como 'Professor Convidado'")
        void professorConvidadoForaDoTenant() {
            Map<String, Object> body = payloadValido();
            body.put("emailProfessorResponsavel", "convidado@universidade-externa.edu.br");

            ResponseEntity<String> response = post("/projetos", body, tokenAluno);

            assertThat(response.getStatusCode().value()).isEqualTo(201);
            assertThat(texto(response, "emailProfessorResponsavel")).isEqualTo("convidado@universidade-externa.edu.br");
        }
    }

    @Nested
    @DisplayName("Atributos obrigatórios e institutions")
    class AtributosEInstituicoes {

        @Test
        @DisplayName("Instituição ausente devolve 400")
        void instituicaoAusente() {
            Map<String, Object> body = payloadValido();
            body.remove("instituicaoId");

            assertThat(post("/projetos", body, tokenAluno).getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("Instituição inexistente devolve 404")
        void instituicaoInexistente() {
            Map<String, Object> body = payloadValido();
            body.put("instituicaoId", UUID.randomUUID().toString());

            ResponseEntity<String> response = post("/projetos", body, tokenAluno);

            assertThat(response.getStatusCode().value()).isEqualTo(404);
            assertThat(texto(response, "mensagem")).contains("Instituição");
        }

        @Test
        @DisplayName("Instituição inativa no catálogo devolve 400")
        void instituicaoInativa() {
            var inativa = criarInstituicao("002", "Fatec E2E Inativa", false);
            Map<String, Object> body = payloadValido();
            body.put("instituicaoId", inativa.getId().toString());

            ResponseEntity<String> response = post("/projetos", body, tokenAluno);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(texto(response, "mensagem")).contains("inativa");
        }

        @Test
        @DisplayName("Título ausente devolve 400")
        void tituloAusente() {
            Map<String, Object> body = payloadValido();
            body.remove("titulo");

            assertThat(post("/projetos", body, tokenAluno).getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("Descrição curta acima de 144 caracteres devolve 400")
        void descricaoCurtaAcimaDoLimite() {
            Map<String, Object> body = payloadValido();
            body.put("descricaoCurta", "x".repeat(145));

            ResponseEntity<String> response = post("/projetos", body, tokenAluno);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(json(response).path("erros").has("descricaoCurta")).isTrue();
        }

        @Test
        @DisplayName("Ano de publicação omitido assume o ano corrente")
        void anoOmitidoAssumeCorrente() {
            Map<String, Object> body = payloadValido();
            body.remove("anoPublicado");

            assertThat(json(post("/projetos", body, tokenAluno)).path("anoPublicado").asInt())
                .isEqualTo(java.time.Year.now().getValue());
        }
    }

    @Nested
    @DisplayName("Story 3.1 — o autor e a instituição vêm do contexto da sessão")
    class AutorEGestao {

        @Test
        @DisplayName("O projeto é atribuído ao usuário autenticado, não ao id enviado")
        void autorVeioDaSessao() {
            User aluno = criarUsuario("Helena Autora", "helena.autora@aluno.cps.sp.gov.br", UserRole.ALUNO);
            String token = tokenDe(aluno);

            Map<String, Object> body = payloadValido();
            body.put("autorId", UUID.randomUUID().toString()); // tentativa de injeteção

            String id = json(post("/projetos", body, token)).path("id").asText();

            var projeto = projetoRepository.findById(UUID.fromString(id)).orElseThrow();
            assertThat(projeto.getAutor().getId()).isEqualTo(aluno.getId());
        }

        @Test
        @DisplayName("Projeto aparece em 'meus projetos' do autor e não dos demais")
        void projetoApareceSomenteDoAutor() {
            User autora = criarUsuario("Iris Autora", "iris.autora@aluno.cps.sp.gov.br", UserRole.ALUNO);
            User outro = criarUsuario("João Outro", "joao.outro@aluno.cps.sp.gov.br", UserRole.ALUNO);

            String id = json(post("/projetos", payloadValido(), tokenDe(autora))).path("id").asText();

            assertThat(jsonList(get("/projetos/meus", tokenDe(autora))))
                .extracting(p -> String.valueOf(p.get("id")))
                .contains(id);
            assertThat(jsonList(get("/projetos/meus", tokenDe(outro)))).isEmpty();
        }

        @Test
        @DisplayName("Projetos do autor vêm do mais recente para o mais antigo")
        void meusProjetosOrdenadosPorData() {
            String token = tokenPara("Karla Ordem", "karla.ordem@aluno.cps.sp.gov.br", UserRole.ALUNO);
            post("/projetos", payloadValido(), token);

            Map<String, Object> segundo = payloadValido();
            segundo.put("titulo", "Segundo Projeto");
            post("/projetos", segundo, token);

            var lista = jsonList(get("/projetos/meus", token));
            assertThat(lista).hasSize(2);
            assertThat(lista.get(0).get("titulo")).isEqualTo("Segundo Projeto");
        }
    }
}