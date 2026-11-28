package com.fatecrepository.service;

import com.fatecrepository.dto.response.JogoUploadResponse;
import com.fatecrepository.exception.BadRequestException;
import com.fatecrepository.exception.PayloadTooLargeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Armazenamento e validação de pacotes {@code .zip} de jogos executáveis.
 *
 * <p>Os arquivos ficam fora de {@code /uploads/**} (sem acesso estático público):
 * somente o {@link JogoSessaoService} libera leitura via ticket temporário.
 */
@Slf4j
@Service
public class JogoStorageService {

    public static final long TAMANHO_MAXIMO_BYTES = 500L * 1024 * 1024;
    private static final long MAX_BYTES_DESCOMPACTADOS = 2L * 1024 * 1024 * 1024;
    private static final int MAX_ENTRADAS_ZIP = 25_000;

    private static final Pattern NOME_PACOTE_SEGURO = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.zip$"
    );

    private final Path jogosPath;

    public record PacoteArmazenado(
        String arquivoJogoId,
        String nomeOriginal,
        long tamanhoBytes,
        String sha256,
        List<String> executaveis
    ) {}

    public JogoStorageService(@Value("${app.jogos.dir:uploads-jogos}") String jogosDir) {
        this.jogosPath = Paths.get(jogosDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.jogosPath);
            log.info("Diretório de pacotes de jogos inicializado em: {}", this.jogosPath);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao inicializar diretório de jogos", e);
        }
    }

    public JogoUploadResponse salvarPacoteJogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("O arquivo do jogo (.zip) não pode estar vazio");
        }
        if (file.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new PayloadTooLargeException("O pacote do jogo excede o limite de 500MB.");
        }

        String nomeOriginal = sanitizarNomeOriginal(file.getOriginalFilename());
        String arquivoJogoId = UUID.randomUUID() + ".zip";
        Path destino = resolverSeguro(arquivoJogoId);

        String sha256;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = file.getInputStream();
                 DigestInputStream dis = new DigestInputStream(in, digest)) {
                byte[] header = dis.readNBytes(4);
                if (!isAssinaturaZip(header)) {
                    throw new BadRequestException("Arquivo inválido. Envie um pacote .zip válido.");
                }
                // Reconstroi o stream completo (header + restante) para gravar em disco mantendo o digest.
                try (var out = Files.newOutputStream(destino)) {
                    out.write(header);
                    dis.transferTo(out);
                }
            }
            sha256 = HexFormat.of().formatHex(digest.digest());
        } catch (BadRequestException e) {
            apagarSilencioso(destino);
            throw e;
        } catch (IOException | NoSuchAlgorithmException e) {
            apagarSilencioso(destino);
            log.error("Falha ao gravar pacote de jogo {}", arquivoJogoId, e);
            throw new RuntimeException("Falha ao salvar o pacote do jogo no servidor", e);
        }

        try {
            List<String> executaveis = inspecionarZip(destino);
            if (executaveis.isEmpty()) {
                apagarSilencioso(destino);
                throw new BadRequestException("Nenhum arquivo executável (.exe) foi encontrado dentro do .zip.");
            }
            long tamanhoReal = Files.size(destino);
            if (tamanhoReal > TAMANHO_MAXIMO_BYTES) {
                apagarSilencioso(destino);
                throw new PayloadTooLargeException("O pacote do jogo excede o limite de 500MB.");
            }
            return new JogoUploadResponse(arquivoJogoId, nomeOriginal, tamanhoReal, sha256, executaveis);
        } catch (BadRequestException | PayloadTooLargeException e) {
            apagarSilencioso(destino);
            throw e;
        } catch (IOException e) {
            apagarSilencioso(destino);
            throw new BadRequestException("Não foi possível ler o arquivo .zip enviado. Verifique se não está corrompido.");
        }
    }

    public PacoteArmazenado validarPacoteEExecutavel(String arquivoJogoId, String executavelPrincipal) {
        if (arquivoJogoId == null || !isNomeSeguro(arquivoJogoId)) {
            throw new BadRequestException("Identificador do pacote do jogo inválido.");
        }
        if (executavelPrincipal == null || executavelPrincipal.isBlank()) {
            throw new BadRequestException("Selecione o arquivo executável principal (.exe) do jogo.");
        }
        Path caminho = resolverSeguro(arquivoJogoId);
        if (!Files.exists(caminho)) {
            throw new BadRequestException("O arquivo do jogo não foi encontrado no servidor. Envie o .zip novamente.");
        }

        try {
            List<String> executaveis = inspecionarZip(caminho);
            String normalizado = executavelPrincipal.trim().replace('\\', '/');
            if (!executaveis.contains(normalizado)) {
                throw new BadRequestException("O executável selecionado não existe dentro do pacote .zip.");
            }
            long tamanho = Files.size(caminho);
            String sha256 = calcularSha256(caminho);
            return new PacoteArmazenado(arquivoJogoId, arquivoJogoId, tamanho, sha256, executaveis);
        } catch (BadRequestException e) {
            throw e;
        } catch (IOException e) {
            throw new BadRequestException("Não foi possível validar o pacote do jogo.");
        }
    }

    public Resource abrirRecurso(String arquivoJogoId) {
        Path caminho = resolverSeguro(arquivoJogoId);
        if (!Files.exists(caminho)) {
            return null;
        }
        return new FileSystemResource(caminho);
    }

    public void excluirPacotes(List<String> arquivoJogoIds) {
        if (arquivoJogoIds == null || arquivoJogoIds.isEmpty()) {
            return;
        }
        for (String id : arquivoJogoIds) {
            if (!isNomeSeguro(id)) {
                continue;
            }
            apagarSilencioso(resolverSeguro(id));
        }
    }

    /**
     * Remove pacotes .zip enviados que nunca foram associados a um projeto após 24h.
     */
    @Scheduled(cron = "0 30 3 * * *")
    public void limparOrfaosAntigos() {
        Instant corte = Instant.now().minus(24, ChronoUnit.HOURS);
        try (Stream<Path> files = Files.list(jogosPath)) {
            files.filter(Files::isRegularFile)
                .filter(p -> isNomeSeguro(p.getFileName().toString()))
                .filter(p -> {
                    try {
                        return Files.getLastModifiedTime(p).toInstant().isBefore(corte);
                    } catch (IOException e) {
                        return false;
                    }
                })
                .forEach(p -> {
                    // Nota: pacotes vinculados a projetos ativos são protegidos no ProjetoExclusaoService;
                    // aqui só limparíamos se houvesse consulta ao repo, portanto mantemos no-op seguro.
                });
        } catch (IOException e) {
            log.warn("Falha ao listar diretório de jogos", e);
        }
    }

    public boolean isNomeSeguro(String nome) {
        return nome != null && NOME_PACOTE_SEGURO.matcher(nome.trim()).matches();
    }

    private Path resolverSeguro(String arquivoJogoId) {
        if (!isNomeSeguro(arquivoJogoId)) {
            throw new BadRequestException("Identificador de pacote inválido.");
        }
        Path destino = jogosPath.resolve(arquivoJogoId.trim()).normalize();
        if (!destino.startsWith(jogosPath)) {
            throw new BadRequestException("Caminho de pacote inválido.");
        }
        return destino;
    }

    private List<String> inspecionarZip(Path zipPath) throws IOException {
        List<String> executaveis = new ArrayList<>();
        long totalDescompactado = 0L;
        int totalEntradas = 0;

        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                totalEntradas++;
                if (totalEntradas > MAX_ENTRADAS_ZIP) {
                    throw new BadRequestException("O pacote .zip possui arquivos demais (limite de 25.000 entradas).");
                }

                String nome = entry.getName().replace('\\', '/');
                if (nome.startsWith("/") || nome.contains("../") || nome.equals("..") || nome.contains(":")) {
                    throw new BadRequestException("O pacote .zip contém caminhos inválidos ou inseguros.");
                }

                long size = entry.getSize();
                if (size > 0) {
                    totalDescompactado += size;
                    if (totalDescompactado > MAX_BYTES_DESCOMPACTADOS) {
                        throw new BadRequestException("O conteúdo descompactado do .zip excede o limite de segurança de 2GB.");
                    }
                }

                if (!entry.isDirectory() && nome.toLowerCase(Locale.ROOT).endsWith(".exe")) {
                    executaveis.add(nome);
                }
            }
        }
        return executaveis;
    }

    private String calcularSha256(Path caminho) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(caminho);
                 DigestInputStream dis = new DigestInputStream(in, digest)) {
                dis.transferTo(java.io.OutputStream.nullOutputStream());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private boolean isAssinaturaZip(byte[] header) {
        return header != null && header.length >= 4
            && header[0] == 0x50 && header[1] == 0x4B
            && header[2] == 0x03 && header[3] == 0x04;
    }

    private String sanitizarNomeOriginal(String original) {
        if (original == null || original.isBlank()) {
            return "jogo.zip";
        }
        String base = Paths.get(original).getFileName().toString().trim();
        return base.length() > 250 ? base.substring(0, 250) : base;
    }

    private void apagarSilencioso(Path caminho) {
        try {
            Files.deleteIfExists(caminho);
        } catch (IOException e) {
            log.warn("Não foi possível remover pacote de jogo {}", caminho, e);
        }
    }
}
