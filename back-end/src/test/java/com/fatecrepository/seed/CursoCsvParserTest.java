package com.fatecrepository.seed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CursoCsvParserTest {

    private final CursoCsvParser parser = new CursoCsvParser();

    @Test
    @DisplayName("parse de CSV válido com BOM produz vínculos limpos")
    void parseValido() throws Exception {
        String csv = "\uFEFFcodigoUnidade;nomeCurso\n"
            + "003;Análise e Desenvolvimento de Sistemas\n"
            + "003;Logística\n"
            + "204;Big Data para Negócios\n";

        var resultado = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(resultado.totalInvalidas()).isZero();
        assertThat(resultado.vinculos()).hasSize(3);
        assertThat(resultado.vinculos().get(0).codigoUnidade()).isEqualTo("003");
        assertThat(resultado.vinculos().get(0).nomeCurso()).isEqualTo("Análise e Desenvolvimento de Sistemas");
    }

    @Test
    @DisplayName("rejeita linhas com código vazio, curso vazio ou vínculo duplicado")
    void rejeitaInvalidas() throws Exception {
        String csv = "codigoUnidade;nomeCurso\n"
            + "003;Logística\n"
            + ";Ciência de Dados\n"
            + "003;\n"
            + "003;logística\n"
            + "004;Gestão Empresarial\n";

        var resultado = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(resultado.vinculos()).hasSize(2);
        assertThat(resultado.totalInvalidas()).isEqualTo(3);
    }

    @Test
    @DisplayName("seed versionado resources/seeds/cursos_instituicoes.csv cobre todas as 86 unidades sem erros")
    void seedVersionadoInteiro() throws Exception {
        var recurso = getClass().getClassLoader().getResourceAsStream("seeds/cursos_instituicoes.csv");
        assertThat(recurso).as("resources/seeds/cursos_instituicoes.csv deve existir").isNotNull();

        var resultado = parser.parse(recurso);

        assertThat(resultado.totalInvalidas()).isZero();
        assertThat(resultado.vinculos()).hasSize(401);

        long unidadesCobertas = resultado.vinculos().stream()
            .map(CursoCsvParser.VinculoCursoSeed::codigoUnidade)
            .distinct()
            .count();
        assertThat(unidadesCobertas).isEqualTo(86);

        long cursosUnicos = resultado.vinculos().stream()
            .map(CursoCsvParser.VinculoCursoSeed::nomeCurso)
            .distinct()
            .count();
        assertThat(cursosUnicos).isEqualTo(102);
    }
}
