package com.fatecrepository.controller;

import com.fatecrepository.model.Professor;
import com.fatecrepository.repository.InstituicaoRepository;
import com.fatecrepository.repository.ProfessorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/professores")
public class ProfessorController {
    private final ProfessorRepository repo;
    private final InstituicaoRepository instituicaoRepo;

    public ProfessorController(ProfessorRepository repo, InstituicaoRepository instituicaoRepo) { this.repo = repo; this.instituicaoRepo = instituicaoRepo; }

    @GetMapping
    public List<Professor> all() { return repo.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Professor> get(@PathVariable UUID id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Professor> create(@RequestBody Professor p) {
        if (p.getInstituicao() != null && p.getInstituicao().getId() != null) instituicaoRepo.findById(p.getInstituicao().getId()).ifPresent(p::setInstituicao);
        return ResponseEntity.status(HttpStatus.CREATED).body(repo.save(p));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Professor> update(@PathVariable UUID id, @RequestBody Professor dto) {
        return repo.findById(id).map(p -> {
            p.setNome(dto.getNome());
            p.setEmail(dto.getEmail());
            p.setTelefone(dto.getTelefone());
            p.setAreaEnsino(dto.getAreaEnsino());
            if (dto.getInstituicao() != null && dto.getInstituicao().getId() != null) instituicaoRepo.findById(dto.getInstituicao().getId()).ifPresent(p::setInstituicao);
            return ResponseEntity.ok(repo.save(p));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> delete(@PathVariable UUID id) {
        return repo.findById(id).map(a -> { repo.deleteById(id); return ResponseEntity.noContent().build(); }).orElse(ResponseEntity.notFound().build());
    }
}
