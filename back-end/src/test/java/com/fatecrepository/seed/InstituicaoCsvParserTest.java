package com.fatecrepository.seed;

import com.fatecrepository.model.Instituicao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class InstituicaoCsvParserTest {

    private final InstituicaoCsvParser parser = new InstituicaoCsvParser();

    @Test
    @DisplayName("parse de CSV válido com BOM e aspas produz entidades normalizadas")
    void parseValido() throws Exception {
        String csv = "\uFEFFcodigoUnidade;nome;endereco;cidade;estado;regiaoAdministrativa;cnpj;telefone;site;linkLogo\n"
            + "001;Fatec Teste;Rua X, 10;São Paulo;SP;Capital;12.345.678/0001-90;(11) 1111-1111;http://site;http://logo\n"
            + "002;Fatec Teste 2;\"Rua Y, 20 - CEP: 01000-000\";Campinas;SP;Campinas;;(18) 3522-4181 / 3502-4500;;\n";

        var resultado = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(resultado.totalInvalidas()).isZero();
        assertThat(resultado.instituicoes()).hasSize(2);

        var primeira = resultado.instituicoes().get(0);
        assertThat(primeira.getCodigoUnidade()).isEqualTo("001");
        assertThat(primeira.getNome()).isEqualTo("Fatec Teste");
        assertThat(primeira.getEndereco()).isEqualTo("Rua X, 10");
        assertThat(primeira.getCidade()).isEqualTo("São Paulo");
        assertThat(primeira.getEstado()).isEqualTo("SP");
        assertThat(primeira.getRegiaoAdministrativa()).isEqualTo("Capital");
        assertThat(primeira.getCnpj()).isEqualTo("12.345.678/0001-90");
        assertThat(primeira.getTelefone()).isEqualTo("(11) 1111-1111");
        assertThat(primeira.getSite()).isEqualTo("http://site");
        assertThat(primeira.getLinkLogo()).isEqualTo("http://logo");
        assertThat(primeira.isAtivo()).isTrue();
        assertThat(primeira.getCriadoEm()).isNotNull();
        assertThat(primeira.getAtualizadoEm()).isNotNull();

        var segunda = resultado.instituicoes().get(1);
        assertThat(segunda.getEndereco()).isEqualTo("Rua Y, 20 - CEP: 01000-000");
        assertThat(segunda.getCnpj()).isNull();
        assertThat(segunda.getTelefone()).isEqualTo("(18) 3522-4181 / 3502-4500");
        assertThat(segunda.getSite()).isNull();
    }

    @Test
    @DisplayName("linhas com código vazio ou duplicado são rejeitadas e contadas")
    void rejeitaLinhaInvalida() throws Exception {
        String csv = "codigoUnidade;nome;endereco;cidade;estado;regiaoAdministrativa;cnpj;telefone;site;linkLogo\n"
            + "001;Fatec A;Rua A;SP;SP;Capital;;;;;\n"
            + ";Fatec sem código;Rua B;SP;SP;Capital;;;;;\n"
            + "001;Fatec duplicada;Rua C;SP;SP;Capital;;;;;\n"
            + "002;Fatec B;Rua D;SP;SP;Capital;;;;;\n";

        var resultado = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(resultado.instituicoes()).hasSize(2);
        assertThat(resultado.linhasInvalidas()).hasSize(2);
        assertThat(resultado.totalInvalidas()).isEqualTo(2);
        assertThat(resultado.linhasInvalidas())
            .anyMatch(motivo -> motivo.contains("código da unidade vazio"))
            .anyMatch(motivo -> motivo.contains("duplicado (001)"));
    }

    @Test
    @DisplayName("seed versionado resources/seeds/instituicoes.csv tem 86 linhas válidas e 0 inválidas")
    void seedVersionadoInteiro() throws Exception {
        var recurso = getClass().getClassLoader().getResourceAsStream("seeds/instituicoes.csv");
        assertThat(recurso).as("resources/seeds/instituicoes.csv deve existir").isNotNull();

        var resultado = parser.parse(recurso);

        assertThat(resultado.totalInvalidas()).isZero();
        assertThat(resultado.instituicoes()).hasSize(86);
        assertThat(resultado.instituicoes())
            .allSatisfy(i -> {
                assertThat(i.getCodigoUnidade()).isNotBlank();
                assertThat(i.getNome()).isNotBlank();
                assertThat(i.getEstado()).isEqualTo("SP");
                assertThat(i.isAtivo()).isTrue();
                assertThat(i.getTelefone() == null || !i.getTelefone().contains("Discagem Abreviada"))
                    .as("telefone não deve conter o sufixo Discagem Abreviada")
                    .isTrue();
                assertThat(i.getRegiaoAdministrativa()).isNotBlank();
                assertThat(i.getCnpj()).isNotBlank();
                assertThat(i.getLinkLogo()).isNotBlank();
            });
        assertThat(resultado.instituicoes().stream()
            .filter(i -> i.getTelefone() == null || i.getTelefone().isBlank())
            .map(Instituicao::getCodigoUnidade))
            .as("apenas a Fatec Votorantim (301) fica sem telefone na fonte oficial")
            .containsExactly("301");
        assertThat(resultado.instituicoes().size())
            .isEqualTo(resultado.instituicoes().stream().map(Instituicao::getCodigoUnidade).distinct().count());
    }
}