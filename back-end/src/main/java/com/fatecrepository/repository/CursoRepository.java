package com.fatecrepository.repository;

import com.fatecrepository.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CursoRepository extends JpaRepository<Curso, UUID> {

    Optional<Curso> findByNomeIgnoreCase(String nome);

    List<Curso> findByAtivoTrueOrderByNomeAsc();
}
