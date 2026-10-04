package com.fatecrepository.e2e;

import com.fatecrepository.model.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Upload de imagens (apoia a Story 3.1 — imagens complementares como blocos
 * nativos do Editor.js) e catálogo público (Epic 5 / FR12 / Story 5.1).
 */
@DisplayName("Upload de imagem e catálogo público")
class UploadECatalogoPublicoE2ETest extends ApiE2ETestSupport {

    private static final byte[] PNG_MINIMO = new byte[]{
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D
    };

    private ResponseEntity<String> enviarUpload(String nomeArquivo, String contentType, byte[] conteudo, String token) {
        MultiValueMap<String, Object> corpo = new LinkedMultiValueMap<>();
        corpo.add("file", new ByteArrayResource(conteudo) {
            @Override
            public String getFilename() {
                return nomeArquivo;
            }
        });

        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        return restTemplate.exchange(url("/uploads"), HttpMethod.POST,
            new HttpEntity<>(corpo, headers), String.class);
    }

    @Nested
    @DisplayName("POST /uploads — protegido e validado")
    class UploadDeImagem {

        @Test
        @DisplayName("Upload sem sessão devolve 401")
        void uploadSemToken() {
            assertNaoAutorizado(enviarUpload("capa.png", "image/png", PNG_MINIMO, null));
        }

        @Test
        @DisplayName("Upload com sessão devolve 201 e devolve a URL relativa")
        void uploadComToken() {
            String token = tokenPara("Aluno Upload", "aluno.upload@aluno.cps.sp.gov.br", UserRole.ALUNO);

            ResponseEntity<String> response = enviarUpload("capa.png", "image/png", PNG_MINIMO, token);

            assertThat(response.getStatusCode().value()).isEqualTo(201);
            assertThat(texto(response, "url")).startsWith("/uploads/").endsWith(".png");
            assertThat(texto(response, "nomeArquivo")).isNotBlank();
        }

        @Test
        @DisplayName("A URL devolvida é publicamente acessível (sem sessão)")
        void urlRetornadaEhPublica() {
            String token = tokenPara("Aluno Upload", "aluno.upload2@aluno.cps.sp.gov.br", UserRole.ALUNO);
            String url = texto(enviarUpload("capa.png", "image/png", PNG_MINIMO, token), "url");

            ResponseEntity<String> response = restTemplate.exchange(url(url), HttpMethod.GET,
                HttpEntity.EMPTY, String.class);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
        }

        @Test
        @DisplayName("Arquivo vazio devolve 400")
        void arquivoVazio() {
            String token = tokenPara("Aluno Upload", "aluno.upload3@aluno.cps.sp.gov.br", UserRole.ALUNO);

            ResponseEntity<String> response = enviarUpload("vazio.png", "image/png", new byte[0], token);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("Tipo não permitido (PDF) devolve 400")
        void tipoNaoPermitido() {
            String token = tokenPara("Aluno Upload", "aluno.upload4@aluno.cps.sp.gov.br", UserRole.ALUNO);

            ResponseEntity<String> response = enviarUpload("documento.pdf", "application/pdf",
                "%PDF-1.4".getBytes(), token);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(texto(response, "mensagem")).containsIgnoringCase("tipo");
        }

        @Test
        @DisplayName("Nomes de arquivo perigosos não escapam do diretório de uploads")
        void nomeDeArquivoNaoEscapaDoDiretorio() {
            String token = tokenPara("Aluno Upload", "aluno.upload5@aluno.cps.sp.gov.br", UserRole.ALUNO);

            ResponseEntity<String> response = enviarUpload("../../evil.png", "image/png", PNG_MINIMO, token);

            assertThat(response.getStatusCode().value()).isEqualTo(201);
            assertThat(texto(response, "url"))
                .as("O nome original jamais deve aparecer na URL")
                .doesNotContain("..")
                .doesNotContain("evil");
        }
    }

    @Nested
    @DisplayName("Epic 5 / Story 5.1 — visitante não autenticado pesquisa o catálogo")
    class VisitanteNaoAutenticado {

        @Test
        @DisplayName("Catálogo de instituições responde sem token")
        void catalogoInstituicoesSemToken() {
            criarInstituicao("001", "Fatec E2E Visitante");

            ResponseEntity<String> response = get("/instituicoes");

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).contains("Fatec E2E Visitante");
        }

        @Test
        @DisplayName("Lista de professores para autocomplete responde sem token")
        void listaProfessoresSemToken() {
            ResponseEntity<String> response = get("/professores");

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).startsWith("[");
        }

        @Test
        @DisplayName("Documentação da API é pública")
        void documentacaoEhPublica() {
            // swagger está desligado no perfil de teste; o essencial é que a rota
            // não exige sessão (se exigisse, o filtro devolveria 401 antes do handler).
            ResponseEntity<String> response = restTemplate.exchange(
                url("/v3/api-docs"), HttpMethod.GET, HttpEntity.EMPTY, String.class);

            assertThat(response.getStatusCode().value()).isNotEqualTo(401);
        }
    }

    @Nested
    @DisplayName("Lacuna de contrato — endpoint público de projetos")
    class LacunaDeContrato {

        @Test
        @DisplayName("GET /projetos/publicos (consumido pelo catálogo público) não está implementado")
        void listarPublicosNaoImplementado() {
            // O front-end (ProjetoService.listarProjetosPublicos) chama esta rota.
            // Hoje ela cai no catch-all anyRequest().permitAll() e explode em 500,
            // obrigando o front-end a cair no PROJETOS_MOCK — ou seja, a vitrine
            // pública nunca mostra dados reais. Este teste documenta a lacuna e
            // deve ser inverted/ removido quando o endpoint for criado.
            ResponseEntity<String> response = get("/projetos/publicos");

            assertThat(response.getStatusCode().value())
                .as("LACUNA CONHECIDA: /projetos/publicos deveria devolver 200 com a lista de projetos aprovados")
                .isEqualTo(500);
        }
    }
}