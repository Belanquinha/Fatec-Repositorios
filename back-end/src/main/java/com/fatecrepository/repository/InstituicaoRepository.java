package com.fatecrepository.repository;

import com.fatecrepository.model.Instituicao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InstituicaoRepository extends JpaRepository<Instituicao, UUID> {
    Optional<Instituicao> findByCnpj(String cnpj);
}

