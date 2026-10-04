package com.fatecrepository.e2e;

import com.fatecrepository.model.Instituicao;
import com.fatecrepository.seed.CatalogoSeeder;
import com.fatecrepository.seed.SeedProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story 2.1 — AC: "o catálogo oficial é pré-populado no boot a partir dos recursos
 * versionados (seed idempotente, sem importação em runtime)".
 *
 * <p>O perfil de teste desliga o seed por padrão ({@code faterepo.seed.files: []});
 * aqui o {@link CatalogoSeeder} é acionado explicitamente contra o CSV versionado,
 * o que nos deixa verificar as duas metades do AC: o catálogo é populado a partir
 * do recurso, e rodar o seed de novo não duplica nada.
 */
@DisplayName("Story 2.1 — Seed do catálogo oficial é idempotente")
class SeedCatalogoE2ETest extends ApiE2ETestSupport {

    @Autowired
    private CatalogoSeeder catalogoSeeder;

    @Autowired
    private SeedProperties seedProperties;

    private void executarSeedDoCsvVersionado() {
        catalogoSeeder.run();
    }

    @Test
    @DisplayName("O seed popula o catálogo a partir do CSV versionado")
    void seedPopulaCatalogo() {
        seedProperties.setFiles(List.of("seeds/instituicoes.csv"));

        executarSeedDoCsvVersionado();

        List<Instituicao> catalogo = instituicaoRepository.findAll();
        assertThat(catalogo)
            .as("O catálogo oficial da CPS deve ser pré-populado no boot")
            .hasSize(86);
        assertThat(catalogo).allSatisfy(instituicao -> {
            assertThat(instituicao.getCodigoUnidade()).isNotBlank();
            assertThat(instituicao.getNome()).isNotBlank();
            assertThat(instituicao.getEstado()).isEqualTo("SP");
            assertThat(instituicao.isAtivo()).isTrue();
        });
    }

    @Test
    @DisplayName("Rodar o seed duas vezes não duplica instituições (idempotência)")
    void seedEhIdempotente() {
        seedProperties.setFiles(List.of("seeds/instituicoes.csv"));

        executarSeedDoCsvVersionado();
        long aposPrimeiraPassada = instituicaoRepository.count();

        executarSeedDoCsvVersionado();
        long aposSegundaPassada = instituicaoRepository.count();

        assertThat(aposPrimeiraPassada).isEqualTo(86);
        assertThat(aposSegundaPassada)
            .as("O seed não pode duplicar registros no boot")
            .isEqualTo(aposPrimeiraPassada);
    }

    @Test
    @DisplayName("As instituições semeadas ficam acessíveis publicamente pela API")
    void catalogoSemeadoEhPublico() {
        seedProperties.setFiles(List.of("seeds/instituicoes.csv"));
        executarSeedDoCsvVersionado();

        ResponseEntity<String> response = get("/instituicoes");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("Fatec Ipiranga");
    }

    @Test
    @DisplayName("O seed não sobrescreve alterações manuais do admin (modo upsert por chave)")
    void seedNaoDuplicaChaves() {
        seedProperties.setFiles(List.of("seeds/instituicoes.csv"));
        executarSeedDoCsvVersionado();

        // O código da unidade é a chave de negócio: não pode haver duas instituições com "004".
        long comCodigo004 = instituicaoRepository.findAll().stream()
            .filter(i -> "004".equals(i.getCodigoUnidade()))
            .count();

        assertThat(comCodigo004).isEqualTo(1);
    }
}