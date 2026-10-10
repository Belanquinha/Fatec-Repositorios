package com.fatecrepository.service;

import com.fatecrepository.dto.response.UploadResponse;
import com.fatecrepository.exception.BadRequestException;
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

        if (file.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new BadRequestException("Imagem excede o limite de 10MB. Comprima ou escolha um arquivo menor");
        }

        String originalFilename = file.getOriginalFilename();
        String extensao = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extensao = originalFilename.substring(originalFilename.lastIndexOf("."));
        } else {
            if ("image/png".equalsIgnoreCase(contentType)) extensao = ".png";
            else if ("image/webp".equalsIgnoreCase(contentType)) extensao = ".webp";
            else extensao = ".jpg";
        }

        String nomeArquivoUnico = UUID.randomUUID() + extensao.toLowerCase();
        Path destino = this.uploadPath.resolve(nomeArquivoUnico);

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
}
