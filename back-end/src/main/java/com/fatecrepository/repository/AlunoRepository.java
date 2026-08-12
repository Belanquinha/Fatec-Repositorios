package com.fatecrepository.repository;

import com.fatecrepository.model.Aluno;
import com.fatecrepository.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AlunoRepository extends JpaRepository<Aluno, UUID> {
    Optional<Aluno> findByUsuario(User usuario);
    Optional<Aluno> findByMatricula(String matricula);
}

