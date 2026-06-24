package com.fatecrepository.controller;

import com.fatecrepository.model.Aluno;
import com.fatecrepository.repository.AlunoRepository;
import com.fatecrepository.repository.InstituicaoRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/alunos")
public class AlunoController {
    private final AlunoRepository repo;
    private final InstituicaoRepository instituicaoRepo;

    public AlunoController(AlunoRepository repo, InstituicaoRepository instituicaoRepo) { this.repo = repo; this.instituicaoRepo = instituicaoRepo; }

    @GetMapping
    public List<Aluno> all() { return repo.findAll(); }

    @GetMapping("/{id}")
    public ResponseEntity<Aluno> get(@PathVariable UUID id) {
        return repo.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Aluno> create(@RequestBody Aluno aluno) {
        if (aluno.getInstituicao() != null && aluno.getInstituicao().getId() != null) {
            instituicaoRepo.findById(aluno.getInstituicao().getId()).ifPresent(aluno::setInstituicao);
        }
        Aluno saved = repo.save(aluno);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Aluno> update(@PathVariable UUID id, @RequestBody Aluno in) {
        return repo.findById(id).map(a -> {
            a.setNome(in.getNome());
            a.setEmail(in.getEmail());
            a.setMatricula(in.getMatricula());
            a.setTelefone(in.getTelefone());
            if (in.getInstituicao() != null && in.getInstituicao().getId() != null) {
                instituicaoRepo.findById(in.getInstituicao().getId()).ifPresent(a::setInstituicao);
            }
            return ResponseEntity.ok(repo.save(a));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<@NonNull Object> delete(@PathVariable UUID id) {
        return repo.findById(id).map(a -> { repo.deleteById(id); return ResponseEntity.noContent().build(); }).orElse(ResponseEntity.notFound().build());
    }
}
