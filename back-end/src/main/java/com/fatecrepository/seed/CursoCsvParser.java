package com.fatecrepository.seed;

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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class CursoCsvParser {

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
        List<VinculoCursoSeed> validos = new ArrayList<>();
        List<String> invalidos = new ArrayList<>();
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
                String codigoUnidade = record.isMapped("codigoUnidade") ? record.get("codigoUnidade").trim() : "";
                String nomeCurso = record.isMapped("nomeCurso") ? record.get("nomeCurso").trim() : "";

                if (codigoUnidade.isEmpty()) {
                    invalidos.add("linha " + record.getRecordNumber() + ": código da unidade vazio");
                    continue;
                }
                if (nomeCurso.isEmpty()) {
                    invalidos.add("linha " + record.getRecordNumber() + ": nome do curso vazio");
                    continue;
                }

                String chave = codigoUnidade + "::" + nomeCurso.toLowerCase(Locale.ROOT);
                if (!vistos.add(chave)) {
                    invalidos.add("linha " + record.getRecordNumber() + ": vínculo duplicado (" + codigoUnidade + " - " + nomeCurso + ")");
                    continue;
                }
                validos.add(new VinculoCursoSeed(codigoUnidade, nomeCurso));
            }
        }

        return new ParseResult(validos, invalidos);
    }

    public record VinculoCursoSeed(String codigoUnidade, String nomeCurso) {
    }

    public record ParseResult(List<VinculoCursoSeed> vinculos, List<String> linhasInvalidas) {
        public int totalInvalidas() {
            return linhasInvalidas.size();
        }
    }
}
