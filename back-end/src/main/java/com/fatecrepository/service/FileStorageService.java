package com.fatecrepository.service;

import com.fatecrepository.dto.response.UploadResponse;
import com.fatecrepository.exception.BadRequestException;
import com.fatecrepository.exception.PayloadTooLargeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    private final Path uploadPath;

    private static final List<String> EXTENSOES_PERMITIDAS = Arrays.asList(
        "image/png", "image/jpeg", "image/jpg", "image/webp"
    );

    /** Extensão derivada do content-type validado — nunca do nome original (anti path traversal). */
    private static final java.util.Map<String, String> EXTENSAO_POR_CONTENT_TYPE = java.util.Map.of(
        "image/png", ".png",
        "image/jpeg", ".jpg",
        "image/jpg", ".jpg",
        "image/webp", ".webp"
    );

    /** Mesmo teto do multipart no application.yml: barreira amigável antes do 500 do container. */
    private static final long TAMANHO_MAXIMO_BYTES = 10L * 1024 * 1024;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadPath);
            log.info("Diretório de upload inicializado em: {}", this.uploadPath);
        } catch (IOException e) {
            log.error("Não foi possível criar o diretório de uploads: {}", this.uploadPath, e);
            throw new RuntimeException("Erro ao inicializar diretório de uploads", e);
        }
    }

    public UploadResponse salvarArquivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("O arquivo de upload não pode estar vazio");
        }

        String contentType = file.getContentType();
        if (contentType == null || !EXTENSOES_PERMITIDAS.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Tipo de arquivo não permitido. Use PNG, JPG, JPEG ou WEBP");
        }
        contentType = contentType.toLowerCase();

        if (file.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new PayloadTooLargeException("Imagem excede o limite de 10MB. Comprima ou escolha um arquivo menor");
        }

        // Magic-bytes: o Content-Type é controlado pelo cliente e pode ser forjado
        // (polyglot HTML/JS com Content-Type image/png). Valida a assinatura real.
        byte[] assinatura;
        try {
            try (java.io.InputStream in = file.getInputStream()) {
                assinatura = in.readNBytes(16);
            }
        } catch (IOException e) {
            throw new BadRequestException("Não foi possível ler o conteúdo do arquivo");
        }
        if (!assinaturaConfereComTipo(assinatura, contentType)) {
            throw new BadRequestException("Conteúdo do arquivo não corresponde ao tipo declarado. Use PNG, JPG ou WEBP válido");
        }

        // Extensão vem do tipo validado — o nome original é descartado por completo,
        // eliminando path traversal via "../" ou separadores no filename.
        String extensao = EXTENSAO_POR_CONTENT_TYPE.getOrDefault(contentType, ".jpg");

        String nomeArquivoUnico = UUID.randomUUID() + extensao;
        Path destino = this.uploadPath.resolve(nomeArquivoUnico).normalize();
        if (!destino.startsWith(this.uploadPath)) {
            throw new BadRequestException("Nome de arquivo inválido");
        }

        try {
            Files.copy(file.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            log.info("Arquivo salvo com sucesso: {}", destino);
            String url = "/uploads/" + nomeArquivoUnico;
            return new UploadResponse(url, nomeArquivoUnico);
        } catch (IOException e) {
            log.error("Falha ao salvar arquivo no disco: {}", nomeArquivoUnico, e);
            throw new RuntimeException("Falha ao salvar arquivo no servidor", e);
        }
    }

    /**
     * Confere os magic-bytes contra o tipo declarado.
     * PNG: 89 50 4E 47 0D 0A 1A 0A · JPEG: FF D8 FF · WEBP: RIFF....WEBP.
     */
    private boolean assinaturaConfereComTipo(byte[] assinatura, String contentType) {
        if (assinatura == null || assinatura.length < 4) {
            return false;
        }
        return switch (contentType) {
            case "image/png" -> assinatura.length >= 8
                && (assinatura[0] & 0xFF) == 0x89 && assinatura[1] == 0x50
                && assinatura[2] == 0x4E && assinatura[3] == 0x47
                && assinatura[4] == 0x0D && assinatura[5] == 0x0A
                && assinatura[6] == 0x1A && assinatura[7] == 0x0A;
            case "image/jpeg", "image/jpg" -> (assinatura[0] & 0xFF) == 0xFF
                && (assinatura[1] & 0xFF) == 0xD8 && (assinatura[2] & 0xFF) == 0xFF;
            case "image/webp" -> assinatura.length >= 12
                && assinatura[0] == 0x52 && assinatura[1] == 0x49
                && assinatura[2] == 0x46 && assinatura[3] == 0x46
                && assinatura[8] == 0x57 && assinatura[9] == 0x45
                && assinatura[10] == 0x42 && assinatura[11] == 0x50;
            default -> false;
        };
    }
}
