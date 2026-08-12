package com.fatecrepository.service;

import com.fatecrepository.dto.request.AlunoRequest;
import com.fatecrepository.dto.response.AlunoResponse;
import com.fatecrepository.exception.BadRequestException;
import com.fatecrepository.exception.ResourceNotFoundException;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.Aluno;
import com.fatecrepository.model.User;
import com.fatecrepository.model.UserRole;
import com.fatecrepository.repository.AlunoRepository;
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
public class AlunoService {

    private final AlunoRepository alunoRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResponseMapper responseMapper;

    @Transactional
    public AlunoResponse criar(AlunoRequest request) {
        log.info("Criando novo aluno: {}", request.getEmail());

        validarAluno(request);

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email já cadastrado");
        }

        User usuario = new User();
        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        usuario.setRole(UserRole.ALUNO);
        usuario.setCriadoEm(LocalDateTime.now());
        usuario.setAtualizadoEm(LocalDateTime.now());

        User usuarioSalvo = userRepository.save(usuario);

        Aluno aluno = new Aluno();
        aluno.setMatricula(request.getMatricula());
        aluno.setTelefone(request.getTelefone());
        aluno.setUsuario(usuarioSalvo);
        aluno.setCriadoEm(LocalDateTime.now());
        aluno.setAtualizadoEm(LocalDateTime.now());

        Aluno alunoSalvo = alunoRepository.save(aluno);
        log.info("Aluno criado com sucesso: {}", alunoSalvo.getId());

        return responseMapper.toAlunoResponse(alunoSalvo);
    }

    public AlunoResponse obterPorId(UUID id) {
        Aluno aluno = alunoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado"));

        return responseMapper.toAlunoResponse(aluno);
    }

    public List<AlunoResponse> obterTodos() {
        return alunoRepository.findAll().stream()
            .map(responseMapper::toAlunoResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public AlunoResponse atualizar(UUID id, AlunoRequest request) {
        log.info("Atualizando aluno: {}", id);

        validarAluno(request);

        Aluno aluno = alunoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado"));

        User usuario = aluno.getUsuario();

        if (!usuario.getEmail().equals(request.getEmail()) && 
            userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BadRequestException("Email já cadastrado");
        }

        usuario.setNome(request.getNome());
        usuario.setEmail(request.getEmail());
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        usuario.setAtualizadoEm(LocalDateTime.now());

        userRepository.save(usuario);

        aluno.setMatricula(request.getMatricula());
        aluno.setTelefone(request.getTelefone());
        aluno.setAtualizadoEm(LocalDateTime.now());

        Aluno alunoAtualizado = alunoRepository.save(aluno);
        log.info("Aluno atualizado com sucesso: {}", id);

        return responseMapper.toAlunoResponse(alunoAtualizado);
    }

    @Transactional
    public void deletar(UUID id) {
        log.info("Deletando aluno: {}", id);

        Aluno aluno = alunoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado"));

        userRepository.delete(aluno.getUsuario());
        alunoRepository.deleteById(id);

        log.info("Aluno deletado com sucesso: {}", id);
    }

    private void validarAluno(AlunoRequest request) {

    }

}
