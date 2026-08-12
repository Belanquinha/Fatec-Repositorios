package com.fatecrepository.service;

import com.fatecrepository.dto.request.ProfessorRequest;
import com.fatecrepository.dto.response.ProfessorResponse;
import com.fatecrepository.exception.BadRequestException;
import com.fatecrepository.exception.ResourceNotFoundException;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.Professor;
import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import com.fatecrepository.repository.ProfessorRepository;
import com.fatecrepository.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProfessorService {

    private final ProfessorRepository professorRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResponseMapper responseMapper;

    @Transactional
    public ProfessorResponse criar(ProfessorRequest request) {
        log.info("Criando novo professor: {}", request.getEmail());

        validarProfessor(request);


        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email já cadastrado");
        }

        User usuario = new User();
        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        usuario.setRole(UserRole.PROFESSOR);
        usuario.setCriadoEm(LocalDateTime.now());
        usuario.setAtualizadoEm(LocalDateTime.now());

        User usuarioSalvo = userRepository.save(usuario);

        Professor professor = new Professor();
        professor.setTelefone(request.getTelefone());
        professor.setAreaEnsino(request.getAreaEnsino());
        professor.setUsuario(usuarioSalvo);
        professor.setCriadoEm(LocalDateTime.now());
        professor.setAtualizadoEm(LocalDateTime.now());

        Professor professorSalvo = professorRepository.save(professor);
        log.info("Professor criado com sucesso: {}", professorSalvo.getId());

        return responseMapper.toProfessorResponse(professorSalvo);
    }

    public ProfessorResponse obterPorId(UUID id) {
        Professor professor = professorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado"));

        return responseMapper.toProfessorResponse(professor);
    }

    public List<ProfessorResponse> obterTodos() {
        return professorRepository.findAll().stream()
            .map(responseMapper::toProfessorResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public ProfessorResponse atualizar(UUID id, ProfessorRequest request) {
        log.info("Atualizando professor: {}", id);

        validarProfessor(request);

        Professor professor = professorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado"));

        User usuario = professor.getUsuario();

        if (!usuario.getEmail().equals(request.getEmail()) && 
            userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email já cadastrado");
        }

        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        usuario.setAtualizadoEm(LocalDateTime.now());

        userRepository.save(usuario);

        professor.setTelefone(request.getTelefone());
        professor.setAreaEnsino(request.getAreaEnsino());
        professor.setAtualizadoEm(LocalDateTime.now());

        Professor professorAtualizado = professorRepository.save(professor);
        log.info("Professor atualizado com sucesso: {}", id);

        return responseMapper.toProfessorResponse(professorAtualizado);
    }

    @Transactional
    public void deletar(UUID id) {
        log.info("Deletando professor: {}", id);

        Professor professor = professorRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado"));

        userRepository.delete(professor.getUsuario());
        professorRepository.deleteById(id);

        log.info("Professor deletado com sucesso: {}", id);
    }

    private void validarProfessor(ProfessorRequest request) {

    }

}
