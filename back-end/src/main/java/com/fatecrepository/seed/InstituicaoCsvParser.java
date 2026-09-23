package com.fatecrepository.seed;

import com.fatecrepository.model.Instituicao;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class InstituicaoCsvParser {

    public ParseResult parse(InputStream input) throws IOException {
        byte[] bytes = input.readAllBytes();
        int offset = (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF)
            ? 3
            : 0;
        Reader reader = new InputStreamReader(
            new ByteArrayInputStream(bytes, offset, bytes.length - offset),
            StandardCharsets.UTF_8
        );
        return parse(reader);
    }

    private ParseResult parse(Reader reader) throws IOException {
        List<Instituicao> validas = new ArrayList<>();
        List<String> invalidas = new ArrayList<>();
        Set<String> vistos = new HashSet<>();

        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setDelimiter(';')
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .setTrim(true)
            .build();

        try (CSVParser parser = format.parse(reader)) {
            for (CSVRecord record : parser) {
                Instituicao instituicao = toInstituicao(record);
                if (instituicao == null || instituicao.getCodigoUnidade().isBlank()) {
                    invalidas.add("linha " + record.getRecordNumber() + ": código da unidade vazio");
                    continue;
                }
                if (!vistos.add(instituicao.getCodigoUnidade())) {
                    invalidas.add("linha " + record.getRecordNumber() + ": código da unidade duplicado (" + instituicao.getCodigoUnidade() + ")");
                    continue;
                }
                validas.add(instituicao);
            }
        }

        return new ParseResult(validas, invalidas);
    }

    private Instituicao toInstituicao(CSVRecord record) {
        Instituicao instituicao = new Instituicao();
        instituicao.setCodigoUnidade(record.get("codigoUnidade"));
        instituicao.setNome(record.get("nome"));
        instituicao.setEndereco(nullIfBlank(record.get("endereco")));
        instituicao.setCidade(nullIfBlank(record.get("cidade")));
        instituicao.setEstado(nullIfBlank(record.get("estado")));
        instituicao.setRegiaoAdministrativa(nullIfBlank(record.get("regiaoAdministrativa")));
        instituicao.setCnpj(nullIfBlank(record.get("cnpj")));
        instituicao.setTelefone(nullIfBlank(record.get("telefone")));
        instituicao.setSite(nullIfBlank(record.get("site")));
        instituicao.setLinkLogo(nullIfBlank(record.get("linkLogo")));
        instituicao.setAtivo(true);
        LocalDateTime agora = LocalDateTime.now();
        instituicao.setCriadoEm(agora);
        instituicao.setAtualizadoEm(agora);
        return instituicao;
    }

    private String nullIfBlank(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    public record ParseResult(List<Instituicao> instituicoes, List<String> linhasInvalidas) {

        public int totalInvalidas() {
            return linhasInvalidas.size();
        }
    }
}