package com.fatecrepository.controller;

import com.fatecrepository.dto.response.JogoUploadResponse;
import com.fatecrepository.dto.response.SessaoJogoMetadataResponse;
import com.fatecrepository.service.JogoSessaoService;
import com.fatecrepository.service.JogoStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/jogos")
@RequiredArgsConstructor
@Tag(name = "Jogos", description = "Upload de pacotes de jogos (.zip) e sessões temporárias do Fatec Launcher")
public class JogoController {

    private final JogoStorageService jogoStorageService;
    private final JogoSessaoService jogoSessaoService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Fazer upload de pacote de jogo (.zip até 500MB)")
    public ResponseEntity<JogoUploadResponse> uploadPacote(@RequestParam("file") MultipartFile file) {
        log.info("POST /jogos/upload: arquivo={}, tamanho={}", file.getOriginalFilename(), file.getSize());
        JogoUploadResponse resp = jogoStorageService.salvarPacoteJogo(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/sessoes/{ticket}")
    @Operation(summary = "Obter metadados de uma sessão temporária de jogo")
    public ResponseEntity<SessaoJogoMetadataResponse> obterMetadados(@PathVariable String ticket) {
        return ResponseEntity.ok(jogoSessaoService.obterMetadados(ticket));
    }

    @GetMapping("/sessoes/{ticket}/arquivo")
    @Operation(summary = "Baixar o pacote .zip de uma sessão temporária de jogo")
    public ResponseEntity<Resource> baixarPacote(@PathVariable String ticket) {
        SessaoJogoMetadataResponse meta = jogoSessaoService.obterMetadados(ticket);
        Resource resource = jogoSessaoService.abrirPacote(ticket);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + meta.getNomeOriginal() + "\"")
            .body(resource);
    }
}
