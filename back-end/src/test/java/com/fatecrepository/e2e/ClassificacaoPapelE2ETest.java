package com.fatecrepository.e2e;

import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import com.fatecrepository.service.UserProvisioningService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Contrato ponta a ponta da classificação de papel por domínio (AD-2 / Story 1.2).
 *
 * <p>Os demais E2E emitem o JWT com o papel já forjado ({@code tokenPara(nome, email, role)}),
 * o que prova o enforcement do filtro mas nunca a regra que deriva o papel do domínio.
 * Esta suíte usa o {@link UserProvisioningService} real para provisionar o usuário a
 * partir do e-mail e assevera o papel que sai no fluxo + o comportamento das rotas
 * gated por papel. Uma regressão na ordem dos sufixos ou no fallback quebra aqui.
 */
@DisplayName("Story 1.2 — Classificação de papel por domínio, de ponta a ponta")
class ClassificacaoPapelE2ETest extends ApiE2ETestSupport {

    @Autowired
    private UserProvisioningService provisioningService;

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "aluno.contrato@aluno.cps.sp.gov.br, ALUNO",
        "prof.contrato@cps.sp.gov.br, PROFESSOR",
        "convidado@universidade-externa.edu.br, ALUNO"
    })
    @DisplayName("provisionar() deriva o papel do domínio do e-mail")
    void papelDerivadoDoDominio(String email, UserRole esperado) {
        User user = provisioningService.provisionar(email, "Usuário Contrato", null);

        assertThat(user.getRole()).isEqualTo(esperado);
    }

    @Test
    @DisplayName("Ordem dos sufixos: @aluno.cps.sp.gov.br é ALUNO mesmo terminando em @cps.sp.gov.br")
    void ordemDosSufixos() {
        // "@aluno.cps.sp.gov.br" também termina com "@cps.sp.gov.br": se o check de
        // professor rodar primeiro, o aluno vira professor e aprova projeto alheio.
        User user = provisioningService.provisionar("aluno.sufixo@aluno.cps.sp.gov.br", "Aluno Sufixo", null);

        assertThat(user.getRole()).isEqualTo(UserRole.ALUNO);
    }

    @Nested
    @DisplayName("Papel classificado governa o acesso às rotas")
    class PapelClassificadoGovernaAcesso {

        @Test
        @DisplayName("E-mail de aluno provisionado recebe 403 na fila de pendentes")
        void alunoProvisionadoRecebe403NaFila() {
            User aluno = provisioningService.provisionar(
                "aluno.fila@aluno.cps.sp.gov.br", "Aluno Fila", null);

            assertAcessoNegado(get("/projetos/pendentes", tokenDe(aluno)));
        }

        @Test
        @DisplayName("E-mail de professor provisionado atravessa a barreira de papel da fila (200)")
        void professorProvisionadoAtravessaBarreiraDePapel() {
            User professor = provisioningService.provisionar(
                "prof.fila@cps.sp.gov.br", "Prof Fila", null);

            ResponseEntity<String> response = get("/projetos/pendentes", tokenDe(professor));

            // Sem vínculos a fila vem vazia, mas o status prova o papel: aluno levaria 403.
            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(jsonList(response)).isEmpty();
        }

        @Test
        @DisplayName("Domínio desconhecido cai no fallback ALUNO (sem privilégio de aprovação)")
        void dominioDesconhecidoCaiEmAluno() {
            User externo = provisioningService.provisionar(
                "externo@universidade-externa.edu.br", "Externo", null);

            assertThat(externo.getRole()).isEqualTo(UserRole.ALUNO);
            assertAcessoNegado(post("/instituicoes",
                Map.of("codigoUnidade", "999", "nome", "Fatec Contrato"), tokenDe(externo)));
        }
    }
}
