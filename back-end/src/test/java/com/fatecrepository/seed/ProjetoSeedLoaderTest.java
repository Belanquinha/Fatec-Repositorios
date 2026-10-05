package com.fatecrepository.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fatecrepository.model.Instituicao;
import com.fatecrepository.model.Projeto;
import com.fatecrepository.model.ProjetoEstado;
import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import com.fatecrepository.repository.InstituicaoRepository;
import com.fatecrepository.repository.ProjetoRepository;
import com.fatecrepository.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Seed — ProjetoSeedLoader")
class ProjetoSeedLoaderTest {

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private InstituicaoRepository instituicaoRepository;

    @Mock
    private UserRepository userRepository;

    private ObjectMapper objectMapper;
    private ProjetoSeedLoader loader;

    @BeforeEach
    void setup(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tempDir) {
        objectMapper = new ObjectMapper();
        loader = new ProjetoSeedLoader(
                projetoRepository,
                instituicaoRepository,
                userRepository,
                new org.springframework.core.io.DefaultResourceLoader(),
                tempDir.toString()
        );
    }

    @Test
    @DisplayName("Lê o arquivo oficial seeds/projetos.json e valida estrutura e integrantes")
    void validaEstruturaDoArquivoOficial() throws Exception {
        InputStream stream = getClass().getResourceAsStream("/seeds/projetos.json");
        assertThat(stream).as("Arquivo seeds/projetos.json deve existir no classpath").isNotNull();

        List<ProjetoSeedDto> lista = objectMapper.readValue(stream, new TypeReference<List<ProjetoSeedDto>>() {});
        assertThat(lista).isNotEmpty();

        ProjetoSeedDto goldenMaker = lista.stream()
                .filter(p -> "Golden Maker".equalsIgnoreCase(p.getTitulo()))
                .findFirst()
                .orElse(null);

        assertThat(goldenMaker).isNotNull();
        assertThat(goldenMaker.getDescricaoCurta().length()).isLessThanOrEqualTo(144);
        assertThat(goldenMaker.getAnoPublicado()).isEqualTo(2025);
        assertThat(goldenMaker.getAutorEmail()).isEqualTo("aluno.teste@aluno.cps.sp.gov.br");
        assertThat(goldenMaker.getCodigoUnidadeInstituicao()).isEqualTo("003");
        assertThat(goldenMaker.getIntegrantes()).hasSize(5);
        assertThat(goldenMaker.getIntegrantes())
                .extracting(ProjetoSeedDto.IntegranteSeedDto::getNome)
                .containsExactlyInAnyOrder("Adryelle", "Gabriel", "Luiz", "Shania", "Thiago");
    }

    @Test
    @DisplayName("Upsert insere novo projeto se ele ainda não existe no banco")
    void upsertInsereProjetoInexistente() {
        InputStream stream = getClass().getResourceAsStream("/seeds/projetos.json");

        when(projetoRepository.existsByTitulo("Golden Maker")).thenReturn(false);

        Instituicao fatecSp = new Instituicao();
        fatecSp.setId(UUID.randomUUID());
        fatecSp.setCodigoUnidade("003");
        fatecSp.setNome("Fatec São Paulo");
        when(instituicaoRepository.findByCodigoUnidade("003")).thenReturn(Optional.of(fatecSp));

        User autor = new User();
        autor.setId(UUID.randomUUID());
        autor.setEmail("aluno.teste@aluno.cps.sp.gov.br");
        autor.setNome("Aluno Teste");
        autor.setRole(UserRole.ALUNO);
        when(userRepository.findByEmail("aluno.teste@aluno.cps.sp.gov.br")).thenReturn(Optional.of(autor));

        int inseridos = loader.carregar(stream, "upsert");

        assertThat(inseridos).isEqualTo(1);

        ArgumentCaptor<Projeto> captor = ArgumentCaptor.forClass(Projeto.class);
        verify(projetoRepository).save(captor.capture());

        Projeto salvo = captor.getValue();
        assertThat(salvo.getTitulo()).isEqualTo("Golden Maker");
        assertThat(salvo.getEstado()).isEqualTo(ProjetoEstado.APROVADO);
        assertThat(salvo.getInstituicao()).isEqualTo(fatecSp);
        assertThat(salvo.getAutor()).isEqualTo(autor);
        assertThat(salvo.getIntegrantes()).hasSize(5);
    }

    @Test
    @DisplayName("Upsert mantém projeto existente e não duplica no banco")
    void upsertMantemExistente() {
        InputStream stream = getClass().getResourceAsStream("/seeds/projetos.json");

        when(projetoRepository.existsByTitulo("Golden Maker")).thenReturn(true);

        int inseridos = loader.carregar(stream, "upsert");

        assertThat(inseridos).isZero();
        verify(projetoRepository, never()).save(any());
    }
}
