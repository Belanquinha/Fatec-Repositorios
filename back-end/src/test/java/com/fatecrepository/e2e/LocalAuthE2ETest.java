package com.fatecrepository.e2e;

import com.fatecrepository.model.VerificationPurpose;
import com.fatecrepository.service.AuthEmailSender;
import com.fatecrepository.service.ConsoleDevAuthEmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Autenticação local E2E — cadastro com código, domínio de papel, login e recuperação de senha")
class LocalAuthE2ETest extends ApiE2ETestSupport {

    @Autowired
    private AuthEmailSender authEmailSender;

    private ConsoleDevAuthEmailSender consoleSender() {
        assertThat(authEmailSender).isInstanceOf(ConsoleDevAuthEmailSender.class);
        return (ConsoleDevAuthEmailSender) authEmailSender;
    }

    @BeforeEach
    void limparCodigosCapturados() {
        if (authEmailSender instanceof ConsoleDevAuthEmailSender devSender) {
            devSender.limpar();
        }
    }

    @Test
    @DisplayName("Fluxo completo: domínio externo vira ALUNO após confirmar código e faz login com senha")
    void cadastroDominioExternoViraAlunoELoga() {
        ResponseEntity<String> inicio = post("/auth/cadastro/iniciar", Map.of(
            "nome", "Mariana Externa",
            "email", "mariana@gmail.com",
            "senha", "senhaSegura123"
        ), null);
        assertThat(inicio.getStatusCode().value()).isEqualTo(200);
        assertThat(userRepository.findByEmail("mariana@gmail.com")).isEmpty();

        String codigo = consoleSender().obterUltimoCodigo("mariana@gmail.com", VerificationPurpose.CADASTRO);
        assertThat(codigo).matches("^\\d{6}$");

        ResponseEntity<String> confirmacao = post("/auth/cadastro/confirmar", Map.of(
            "email", "mariana@gmail.com",
            "codigo", codigo
        ), null);
        assertThat(confirmacao.getStatusCode().value()).isEqualTo(200);
        assertThat(texto(confirmacao, "role")).isEqualTo("ALUNO");
        assertThat(texto(confirmacao, "accessToken")).isNotBlank();

        ResponseEntity<String> login = post("/auth/login", Map.of(
            "email", "mariana@gmail.com",
            "senha", "senhaSegura123"
        ), null);
        assertThat(login.getStatusCode().value()).isEqualTo(200);
        assertThat(texto(login, "role")).isEqualTo("ALUNO");
        assertThat(texto(login, "email")).isEqualTo("mariana@gmail.com");
    }

    @Test
    @DisplayName("Fluxo completo: domínio @cps.sp.gov.br vira PROFESSOR após confirmar código")
    void cadastroDominioCpsViraProfessor() {
        ResponseEntity<String> inicio = post("/auth/cadastro/iniciar", Map.of(
            "nome", "Prof. Roberto Silva",
            "email", "roberto.silva@cps.sp.gov.br",
            "senha", "senhaDocente123"
        ), null);
        assertThat(inicio.getStatusCode().value()).isEqualTo(200);

        // Antes de confirmar, o professor NÃO aparece na tabela de usuários
        assertThat(userRepository.findByEmail("roberto.silva@cps.sp.gov.br")).isEmpty();

        String codigo = consoleSender().obterUltimoCodigo(
            "roberto.silva@cps.sp.gov.br",
            VerificationPurpose.CADASTRO
        );

        ResponseEntity<String> confirmacao = post("/auth/cadastro/confirmar", Map.of(
            "email", "roberto.silva@cps.sp.gov.br",
            "codigo", codigo
        ), null);
        assertThat(confirmacao.getStatusCode().value()).isEqualTo(200);
        assertThat(texto(confirmacao, "role")).isEqualTo("PROFESSOR");

        // Token emitido tem permissão real de PROFESSOR no back-end
        String tokenProf = texto(confirmacao, "accessToken");
        ResponseEntity<String> vinculos = get("/professores/me/instituicoes", tokenProf);
        assertThat(vinculos.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("Bloqueia força bruta após 5 tentativas erradas no código de cadastro")
    void bloqueiaAposCincoTentativasIncorretas() {
        post("/auth/cadastro/iniciar", Map.of(
            "nome", "Teste Brute Force",
            "email", "bruteforce@outlook.com",
            "senha", "senhaSegura123"
        ), null);

        String codigoReal = consoleSender().obterUltimoCodigo(
            "bruteforce@outlook.com",
            VerificationPurpose.CADASTRO
        );
        String codigoErrado = "000000".equals(codigoReal) ? "999999" : "000000";

        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> erro = post("/auth/cadastro/confirmar", Map.of(
                "email", "bruteforce@outlook.com",
                "codigo", codigoErrado
            ), null);
            assertThat(erro.getStatusCode().value()).isEqualTo(400);
        }

        // Na 6ª tentativa, mesmo enviando o código real, o registro já foi invalidado
        ResponseEntity<String> tentativaComCodigoReal = post("/auth/cadastro/confirmar", Map.of(
            "email", "bruteforce@outlook.com",
            "codigo", codigoReal
        ), null);
        assertThat(tentativaComCodigoReal.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("Fluxo completo de recuperação de senha atualiza credencial de login")
    void recuperacaoDeSenhaAtualizaCredencial() {
        post("/auth/cadastro/iniciar", Map.of(
            "nome", "Lucas Aluno",
            "email", "lucas@aluno.cps.sp.gov.br",
            "senha", "senhaAntiga123"
        ), null);
        String codCadastro = consoleSender().obterUltimoCodigo(
            "lucas@aluno.cps.sp.gov.br",
            VerificationPurpose.CADASTRO
        );
        post("/auth/cadastro/confirmar", Map.of(
            "email", "lucas@aluno.cps.sp.gov.br",
            "codigo", codCadastro
        ), null);

        ResponseEntity<String> esqueci = post("/auth/senha/esquecer", Map.of(
            "email", "lucas@aluno.cps.sp.gov.br"
        ), null);
        assertThat(esqueci.getStatusCode().value()).isEqualTo(200);

        String codRecuperacao = consoleSender().obterUltimoCodigo(
            "lucas@aluno.cps.sp.gov.br",
            VerificationPurpose.RECUPERACAO_SENHA
        );
        assertThat(codRecuperacao).matches("^\\d{6}$");

        ResponseEntity<String> redefinir = post("/auth/senha/redefinir", Map.of(
            "email", "lucas@aluno.cps.sp.gov.br",
            "codigo", codRecuperacao,
            "novaSenha", "senhaNova456"
        ), null);
        assertThat(redefinir.getStatusCode().value()).isEqualTo(200);

        // Senha antiga não funciona mais
        ResponseEntity<String> loginAntigo = post("/auth/login", Map.of(
            "email", "lucas@aluno.cps.sp.gov.br",
            "senha", "senhaAntiga123"
        ), null);
        assertThat(loginAntigo.getStatusCode().value()).isEqualTo(401);

        // Nova senha autentica normalmente
        ResponseEntity<String> loginNovo = post("/auth/login", Map.of(
            "email", "lucas@aluno.cps.sp.gov.br",
            "senha", "senhaNova456"
        ), null);
        assertThat(loginNovo.getStatusCode().value()).isEqualTo(200);
        assertThat(texto(loginNovo, "role")).isEqualTo("ALUNO");
    }
}
