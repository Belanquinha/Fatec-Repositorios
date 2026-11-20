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
    private static final String CATALOGO_CURSOS = "cursos_instituicoes.csv";
    private static final String SEED_PROJETOS = "projetos.json";

    private final SeedProperties seedProperties;
    private final InstituicaoCsvParser instituicaoCsvParser;
    private final InstituicaoSeedLoader instituicaoSeedLoader;
    private final CursoCsvParser cursoCsvParser;
    private final CursoSeedLoader cursoSeedLoader;
    private final ProjetoSeedLoader projetoSeedLoader;
    private final ResourceLoader resourceLoader;

    @Override
    public void run(String... args) {
        log.info("[seed] inicializando catálogo e dados iniciais (faterepo.seed.mode={})", seedProperties.getMode());

        if (seedProperties.getFiles() == null || seedProperties.getFiles().isEmpty()) {
            log.warn("[seed] nenhum arquivo configurado em faterepo.seed.files");
            return;
        }

        for (String arquivo : seedProperties.getFiles()) {
            // Compara o nome do arquivo, não o path com endsWith: "cursos_instituicoes.csv"
            // termina com "instituicoes.csv" e seria erroneamente tratado como catálogo de
            // instituições (o parser não acharia a coluna "nome" e derrubaria o boot).
            String nome = arquivo.substring(arquivo.lastIndexOf('/') + 1);
            if (CATALOGO_INSTITUICOES.equals(nome)) {
                carregarInstituicoes(arquivo);
            } else if (CATALOGO_CURSOS.equals(nome)) {
                carregarCursos(arquivo);
            } else if (SEED_PROJETOS.equals(nome)) {
                carregarProjetos(arquivo);
            } else {
                log.warn("[seed] arquivo de seed não suportado, ignorado: {}", arquivo);
            }
        }
    }

    private void carregarProjetos(String resourcePath) {
        Resource resource = resourceLoader.getResource("classpath:" + resourcePath);
        try (InputStream input = resource.getInputStream()) {
            int inseridos = projetoSeedLoader.carregar(input, seedProperties.getMode());
            log.info("[seed] {} => carga concluída ({} projetos novos inseridos)", resourcePath, inseridos);
        } catch (IOException ex) {
            log.error("[seed] falha ao ler o recurso {}: {}", resourcePath, ex.getMessage());
        }
    }

    private void carregarCursos(String resourcePath) {
        Resource resource = resourceLoader.getResource("classpath:" + resourcePath);
        try (InputStream input = resource.getInputStream()) {
            var resultado = cursoCsvParser.parse(input);

            resultado.linhasInvalidas()
                .forEach(motivo -> log.warn("[seed] linha inválida em {}: {} (ignorada)", resourcePath, motivo));

            if ("replace".equalsIgnoreCase(seedProperties.getMode())) {
                int reinseridos = cursoSeedLoader.replace(resultado.vinculos());
                log.info("[seed] {} => replace: {} vínculos de cursos carregados ({} linhas inválidas ignoradas)",
                    resourcePath, reinseridos, resultado.totalInvalidas());
            } else {
                int inseridos = cursoSeedLoader.upsert(resultado.vinculos());
                log.info("[seed] {} => upsert: {} novos vínculos inseridos ({} linhas inválidas ignoradas)",
                    resourcePath, inseridos, resultado.totalInvalidas());
            }
        } catch (IOException ex) {
            log.error("[seed] falha ao ler o recurso {}: {}", resourcePath, ex.getMessage());
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
