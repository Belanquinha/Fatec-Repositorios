package com.fatecrepository.controller;

import com.fatecrepository.dto.response.UploadResponse;
import com.fatecrepository.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
@Tag(name = "Uploads", description = "Endpoints para upload de arquivos e imagens")
public class UploadController {

    private final FileStorageService fileStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Fazer upload de imagem", description = "Envia uma imagem para a capa do projeto ou corpo do editor (PNG, JPG, WEBP até 10MB)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Imagem salva com sucesso"),
        @ApiResponse(responseCode = "400", description = "Arquivo inválido ou extensão não permitida"),
        @ApiResponse(responseCode = "500", description = "Erro ao salvar arquivo")
    })
    public ResponseEntity<UploadResponse> uploadImagem(@RequestParam("file") MultipartFile file) {
        log.info("POST /uploads: arquivo recebido: {}, tamanho: {} bytes", file.getOriginalFilename(), file.getSize());
        UploadResponse response = fileStorageService.salvarArquivo(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
