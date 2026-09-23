package com.fatecrepository.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogoSeeder implements CommandLineRunner {

    private static final String CATALOGO_INSTITUICOES = "instituicoes.csv";

    private final SeedProperties seedProperties;
    private final InstituicaoCsvParser instituicaoCsvParser;
    private final InstituicaoSeedLoader instituicaoSeedLoader;
    private final ResourceLoader resourceLoader;

    @Override
    public void run(String... args) {
        log.info("[seed] inicializando catálogo de instituições (faterepo.seed.mode={})", seedProperties.getMode());

        if (seedProperties.getFiles() == null || seedProperties.getFiles().isEmpty()) {
            log.warn("[seed] nenhum arquivo configurado em faterepo.seed.files");
            return;
        }

        for (String arquivo : seedProperties.getFiles()) {
            if (arquivo.endsWith(CATALOGO_INSTITUICOES)) {
                carregarInstituicoes(arquivo);
            } else {
                log.warn("[seed] arquivo de seed não suportado, ignorado: {}", arquivo);
            }
        }
    }

    private void carregarInstituicoes(String resourcePath) {
        Resource resource = resourceLoader.getResource("classpath:" + resourcePath);
        try (InputStream input = resource.getInputStream()) {

            var resultado = instituicaoCsvParser.parse(input);

            resultado.linhasInvalidas()
                .forEach(motivo -> log.warn("[seed] linha inválida em {}: {} (ignorada)", resourcePath, motivo));

            if ("replace".equalsIgnoreCase(seedProperties.getMode())) {
                int reinseridas = instituicaoSeedLoader.replace(resultado.instituicoes());
                log.info("[seed] {} => replace: {} instituições carregadas ({} linhas inválidas ignoradas)",
                    resourcePath, reinseridas, resultado.totalInvalidas());
            } else {
                int inseridas = instituicaoSeedLoader.upsert(resultado.instituicoes());
                log.info("[seed] {} => upsert: {} novas inseridas, {} já existentes mantidas ({} linhas inválidas ignoradas)",
                    resourcePath, inseridas, resultado.instituicoes().size() - inseridas, resultado.totalInvalidas());
            }
        } catch (IOException ex) {
            log.error("[seed] falha ao ler o recurso {}: {}", resourcePath, ex.getMessage());
        }
    }
}