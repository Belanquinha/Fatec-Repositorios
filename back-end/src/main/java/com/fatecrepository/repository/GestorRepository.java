package com.fatecrepository.repository;

import com.fatecrepository.model.Gestor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GestorRepository extends JpaRepository<Gestor, UUID> {
    Optional<Gestor> findByEmail(String email);
}
