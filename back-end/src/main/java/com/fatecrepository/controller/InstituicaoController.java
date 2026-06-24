package com.fatecrepository.controller;

import com.fatecrepository.model.Instituicao;
import com.fatecrepository.repository.InstituicaoRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/instituicoes")
public class InstituicaoController {
    private final InstituicaoRepository repo;

    public InstituicaoController(InstituicaoRepository repo) { this.repo = repo; }

    @GetMapping
    public List<Instituicao> all() { return repo.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Instituicao> get(@PathVariable UUID id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Instituicao> create(@RequestBody Instituicao i) {
        Instituicao saved = repo.save(i);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Instituicao> update(@PathVariable UUID id, @RequestBody Instituicao dto) {
        return repo.findById(id).map(i -> {
            i.setNome(dto.getNome());
            i.setCnpj(dto.getCnpj());
            i.setEmail(dto.getEmail());
            i.setTelefone(dto.getTelefone());
            return ResponseEntity.ok(repo.save(i));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<@NonNull Object> delete(@PathVariable UUID id) {
        return repo.findById(id).map(a -> { repo.deleteById(id); return ResponseEntity.noContent().build(); }).orElse(ResponseEntity.notFound().build());
    }
}
