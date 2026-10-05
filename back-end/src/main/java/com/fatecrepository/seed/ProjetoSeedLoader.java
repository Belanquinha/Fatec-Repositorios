package com.fatecrepository.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fatecrepository.model.*;
import com.fatecrepository.repository.InstituicaoRepository;
import com.fatecrepository.repository.ProjetoRepository;
import com.fatecrepository.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ProjetoSeedLoader {

    private final ProjetoRepository projetoRepository;
    private final InstituicaoRepository instituicaoRepository;
    private final UserRepository userRepository;
    private final ResourceLoader resourceLoader;
    private final String uploadDir;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProjetoSeedLoader(
            ProjetoRepository projetoRepository,
            InstituicaoRepository instituicaoRepository,
            UserRepository userRepository,
            ResourceLoader resourceLoader,
            @Value("${app.upload.dir:uploads}") String uploadDir
    ) {
        this.projetoRepository = projetoRepository;
        this.instituicaoRepository = instituicaoRepository;
        this.userRepository = userRepository;
        this.resourceLoader = resourceLoader;
        this.uploadDir = uploadDir;
    }

    @Transactional
    public int carregar(InputStream input, String mode) {
        try {
            List<ProjetoSeedDto> dtos = objectMapper.readValue(input, new TypeReference<List<ProjetoSeedDto>>() {});
            if (dtos == null || dtos.isEmpty()) {
                log.warn("[seed-projeto] nenhum projeto encontrado no arquivo de seed");
                return 0;
            }

            int inseridos = 0;
            int existentes = 0;

            for (ProjetoSeedDto dto : dtos) {
                // Se o seed indicar um arquivo do classpath para a imagem de capa, provisiona no sistema de uploads real do backend
                if (dto.getImagemSeedClasspath() != null && dto.getImagemCapaUrl() != null) {
                    provisionarImagemDeUpload(dto.getImagemSeedClasspath(), dto.getImagemCapaUrl());
                }

                if ("replace".equalsIgnoreCase(mode)) {
                    projetoRepository.findByTitulo(dto.getTitulo()).ifPresent(projetoRepository::delete);
                } else if (projetoRepository.existsByTitulo(dto.getTitulo())) {
                    log.info("[seed-projeto] projeto '{}' já existente mantido", dto.getTitulo());
                    existentes++;
                    continue;
                }

                Instituicao instituicao = instituicaoRepository.findByCodigoUnidade(dto.getCodigoUnidadeInstituicao())
                        .orElseGet(() -> {
                            log.warn("[seed-projeto] instituição com código {} não encontrada, buscando primeira ativa", dto.getCodigoUnidadeInstituicao());
                            return instituicaoRepository.findAll().stream()
                                    .filter(Instituicao::isAtivo)
                                    .findFirst()
                                    .orElseThrow(() -> new IllegalStateException("Nenhuma instituição disponível para vincular o projeto"));
                        });

                User autor = userRepository.findByEmail(dto.getAutorEmail())
                        .orElseGet(() -> {
                            log.info("[seed-projeto] autor {} não encontrado, criando usuário", dto.getAutorEmail());
                            User novo = new User();
                            novo.setNome(dto.getAutorNome() != null ? dto.getAutorNome() : "Aluno Fatec");
                            novo.setEmail(dto.getAutorEmail());
                            novo.setRole(UserRole.ALUNO);
                            novo.setCriadoEm(LocalDateTime.now());
                            novo.setAtualizadoEm(LocalDateTime.now());
                            return userRepository.save(novo);
                        });

                Projeto projeto = new Projeto();
                projeto.setTitulo(dto.getTitulo());
                projeto.setDescricaoCurta(dto.getDescricaoCurta());
                projeto.setConteudoEditorJs(dto.getConteudoEditorJs());
                projeto.setLinkRepositorio(dto.getLinkRepositorio());
                projeto.setImagemCapaUrl(dto.getImagemCapaUrl());
                projeto.setPalavrasChave(dto.getPalavrasChave());
                projeto.setAnoPublicado(dto.getAnoPublicado() != null ? dto.getAnoPublicado() : 2025);

                ProjetoEstado estado = ProjetoEstado.APROVADO;
                if (dto.getEstado() != null) {
                    try {
                        estado = ProjetoEstado.fromValue(dto.getEstado());
                    } catch (Exception e) {
                        log.warn("[seed-projeto] estado inválido {}, assumindo APROVADO", dto.getEstado());
                    }
                }
                projeto.setEstado(estado);
                projeto.setEmailProfessorResponsavel(dto.getEmailProfessorResponsavel());
                projeto.setInstituicao(instituicao);
                projeto.setAutor(autor);
                projeto.setCriadoEm(LocalDateTime.now());
                projeto.setAtualizadoEm(LocalDateTime.now());

                if (dto.getIntegrantes() != null) {
                    for (ProjetoSeedDto.IntegranteSeedDto iDto : dto.getIntegrantes()) {
                        Integrante integrante = new Integrante();
                        integrante.setNome(iDto.getNome());
                        integrante.setLinkLinkedin(iDto.getLinkLinkedin());
                        integrante.setCriadoEm(LocalDateTime.now());
                        projeto.adicionarIntegrante(integrante);
                    }
                }

                projetoRepository.save(projeto);
                inseridos++;
                log.info("[seed-projeto] projeto '{}' persistido com sucesso com estado {}", projeto.getTitulo(), projeto.getEstado());
            }

            log.info("[seed-projeto] carga finalizada: {} inseridos, {} já existentes mantidos", inseridos, existentes);
            return inseridos;
        } catch (Exception e) {
            log.error("[seed-projeto] erro ao processar seed de projetos: {}", e.getMessage(), e);
            throw new RuntimeException("Falha ao semear projetos", e);
        }
    }

    private void provisionarImagemDeUpload(String classpathLocation, String imagemCapaUrl) {
        if (!imagemCapaUrl.startsWith("/uploads/")) {
            return;
        }
        // Só o nome do arquivo é aproveitado: evita path traversal via seed
        // (ex.: "/uploads/../../etc/passwd" vira "passwd" e fica contido no uploadDir).
        String nomeArquivo = Paths.get(imagemCapaUrl.substring("/uploads/".length())).getFileName().toString();
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path destino = uploadPath.resolve(nomeArquivo).normalize();
        if (!destino.startsWith(uploadPath)) {
            log.warn("[seed-projeto] nome de arquivo de capa fora do diretório de uploads, ignorado: {}", imagemCapaUrl);
            return;
        }

        try {
            if (!Files.exists(destino)) {
                Resource resource = resourceLoader.getResource("classpath:" + classpathLocation);
                if (resource.exists()) {
                    Files.createDirectories(uploadPath);
                    try (InputStream in = resource.getInputStream()) {
                        Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
                        log.info("[seed-projeto] Imagem de capa provisionada fisicamente no diretório de uploads: {}", destino);
                    }
                } else {
                    log.warn("[seed-projeto] Imagem de seed não encontrada no classpath: {}", classpathLocation);
                }
            }
        } catch (IOException e) {
            log.error("[seed-projeto] Falha ao provisionar imagem no diretório de uploads: {}", e.getMessage(), e);
        }
    }
}
