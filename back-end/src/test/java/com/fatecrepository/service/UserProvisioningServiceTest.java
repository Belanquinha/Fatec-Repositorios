package com.fatecrepository.service;

import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import com.fatecrepository.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A classificação de papel mora aqui, e é compartilhada pelo login Microsoft e pelo login de
 * desenvolvimento. Estes testes eram os de domínio dentro de {@code AuthServiceTest}; foram movidos
 * para cá quando a regra ganhou dono próprio.
 */
@ExtendWith(MockitoExtension.class)
class UserProvisioningServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserProvisioningService service;

    @Test
    @DisplayName("Deve classificar como ALUNO quando o domínio é o de aluno da CPS")
    void deveClassificarAlunoPorDominioDaCps() {
        assertEquals(UserRole.ALUNO, service.classificarPapel("gabriel@aluno.cps.sp.gov.br"));
    }

    @Test
    @DisplayName("Deve classificar como PROFESSOR quando o domínio é @cps.sp.gov.br")
    void deveClassificarProfessorPorDominioDaCps() {
        assertEquals(UserRole.PROFESSOR, service.classificarPapel("maria@cps.sp.gov.br"));
    }

    @Test
    @DisplayName("Deve classificar como ALUNO quando o domínio não é da CPS (fallback de AD-2)")
    void deveClassificarComoAlunoQuandoDominioNaoEhDaCps() {
        assertEquals(UserRole.ALUNO, service.classificarPapel("externo@exemplo.com"));
    }

    @Test
    @DisplayName("Deve classificar pelo domínio mesmo com o e-mail em maiúsculas e com espaços")
    void deveClassificarComEmailNaoNormalizado() {
        // A regra compara por `endsWith`, então um e-mail com caixa alta ou espaço não classify
        // se não for normalizado antes.
        assertEquals(UserRole.PROFESSOR, service.classificarPapel("  MARIA@CPS.SP.GOV.BR  "));
    }

    @Test
    @DisplayName("Deve criar o usuário com o papel derivado do domínio quando o e-mail ainda não existe")
    void deveCriarUsuarioComPapelDerivadoDoDominio() {
        when(userRepository.findByEmail("maria@cps.sp.gov.br")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocacao -> {
            User user = invocacao.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        User user = service.provisionar("maria@cps.sp.gov.br", "Maria da CPS", null);

        assertEquals(UserRole.PROFESSOR, user.getRole());
        assertEquals("maria@cps.sp.gov.br", user.getEmail());
    }

    @Test
    @DisplayName("Deve normalizar o e-mail antes de consultar, para não duplicar usuário por caixa")
    void deveNormalizarEmailAntesDeConsultar() {
        when(userRepository.findByEmail("maria@cps.sp.gov.br")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        service.provisionar("  Maria@CPS.SP.GOV.BR  ", "Maria", null);

        // A coluna de e-mail é única: buscar sem normalizar criaria um segundo usuário para o
        // mesmo endereço, em vez de encontrar o existente.
        verify(userRepository).findByEmail("maria@cps.sp.gov.br");
    }

    @Test
    @DisplayName("Deve reaproveitar o usuário existente sem alterar o papel já atribuído")
    void deveReaproveitarUsuarioExistente() {
        User existente = new User();
        existente.setId(UUID.randomUUID());
        existente.setNome("Maria da CPS");
        existente.setEmail("maria@cps.sp.gov.br");
        existente.setRole(UserRole.ADMIN);

        when(userRepository.findByEmail("maria@cps.sp.gov.br")).thenReturn(Optional.of(existente));

        User user = service.provisionar("maria@cps.sp.gov.br", "Outro Nome", null);

        // O seed do admin define o papel; o login não pode rebaixar nem promover ninguém.
        assertEquals(UserRole.ADMIN, user.getRole());
        assertEquals("Maria da CPS", user.getNome());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve aplicar a foto quando o usuário ainda não tem uma")
    void deveAplicarFotoAusente() {
        User existente = new User();
        existente.setId(UUID.randomUUID());
        existente.setEmail("gabriel@aluno.cps.sp.gov.br");
        existente.setRole(UserRole.ALUNO);

        when(userRepository.findByEmail("gabriel@aluno.cps.sp.gov.br")).thenReturn(Optional.of(existente));

        User user = service.provisionar("gabriel@aluno.cps.sp.gov.br", "Gabriel", "data:image/jpeg;base64,QUJD");

        assertEquals("data:image/jpeg;base64,QUJD", user.getFotoUrl());
        verify(userRepository).save(existente);
    }

    @Test
    @DisplayName("Deve preservar a foto já existente quando o login chega sem uma")
    void devePreservarFotoExistente() {
        User existente = new User();
        existente.setId(UUID.randomUUID());
        existente.setEmail("gabriel@aluno.cps.sp.gov.br");
        existente.setRole(UserRole.ALUNO);
        existente.setFotoUrl("data:image/jpeg;base64,WFla");

        when(userRepository.findByEmail("gabriel@aluno.cps.sp.gov.br")).thenReturn(Optional.of(existente));

        User user = service.provisionar("gabriel@aluno.cps.sp.gov.br", "Gabriel", null);

        // O login de desenvolvimento passa foto `null`. Sobrescrever com null apagaria a foto que o
        // Microsoft buscou, e o `save` extra só gastaria uma escrita.
        assertEquals("data:image/jpeg;base64,WFla", user.getFotoUrl());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Deve devolver e-mail nulo sem estourar quando o valor é nulo")
    void deveNormalizarEmailNuloSemEstourar() {
        // O chamador decide o que fazer com o e-mail ausente; aqui só não pode ser um NPE.
        assertNull(service.normalizarEmail(null));
    }

    @Test
    @DisplayName("Deve gravar o usuário novo com o nome informado")
    void deveGravarNomeInformado() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        User user = service.provisionar("joao@aluno.cps.sp.gov.br", "João da Silva", null);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("João da Silva", captor.getValue().getNome());
        assertEquals(user.getNome(), captor.getValue().getNome());
    }
}