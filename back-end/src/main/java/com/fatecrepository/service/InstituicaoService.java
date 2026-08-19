package com.fatecrepository.service;

import com.fatecrepository.dto.request.InstituicaoRequest;
import com.fatecrepository.dto.response.InstituicaoResponse;
import com.fatecrepository.exception.BadRequestException;
import com.fatecrepository.exception.ResourceNotFoundException;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.Gestor;
import com.fatecrepository.model.Instituicao;
import com.fatecrepository.repository.GestorRepository;
import com.fatecrepository.repository.InstituicaoRepository;
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
public class InstituicaoService {

    private final InstituicaoRepository instituicaoRepository;
    private final GestorRepository gestorRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResponseMapper responseMapper;

    @Transactional
    public InstituicaoResponse criar(InstituicaoRequest request) {
        log.info("Criando nova instituição: {}", request.getNome());

        if (instituicaoRepository.findByCnpj(request.getCnpj()).isPresent()) {
            throw new BadRequestException("CNPJ já cadastrado");
        }

        if (request.getGestor() == null) {
            throw new BadRequestException("Gestor é obrigatório");
        }

        if (gestorRepository.findByEmail(request.getGestor().getEmail()).isPresent()) {
            throw new BadRequestException("Email do gestor já cadastrado");
        }

        Instituicao instituicao = new Instituicao();
        instituicao.setNome(request.getNome());
        instituicao.setCnpj(request.getCnpj());
        instituicao.setEndereco(request.getEndereco());
        instituicao.setCidade(request.getCidade());
        instituicao.setEstado(request.getEstado());
        instituicao.setCriadoEm(LocalDateTime.now());
        instituicao.setAtualizadoEm(LocalDateTime.now());

        Gestor gestor = new Gestor();
        gestor.setNome(request.getGestor().getNome());
        gestor.setEmail(request.getGestor().getEmail());
        gestor.setTelefone(request.getGestor().getTelefone());
        gestor.setSenha(passwordEncoder.encode(request.getGestor().getSenha()));
        gestor.setInstituicao(instituicao);
        instituicao.setGestor(gestor);

        Instituicao salva = instituicaoRepository.save(instituicao);
        log.info("Instituição criada com sucesso: {}", salva.getId());

        return responseMapper.toInstituicaoResponse(salva);
    }

    public InstituicaoResponse obterPorId(UUID id) {
        Instituicao instituicao = instituicaoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Instituição não encontrada"));

        return responseMapper.toInstituicaoResponse(instituicao);
    }

    public List<InstituicaoResponse> obterTodas() {
        return instituicaoRepository.findAll().stream()
            .map(responseMapper::toInstituicaoResponse)
            .collect(Collectors.toList());
    }

    @Transactional
    public InstituicaoResponse atualizar(UUID id, InstituicaoRequest request) {
        log.info("Atualizando instituição: {}", id);

        Instituicao instituicao = instituicaoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Instituição não encontrada"));

        if (!instituicao.getCnpj().equals(request.getCnpj()) && 
            instituicaoRepository.findByCnpj(request.getCnpj()).isPresent()) {
            throw new BadRequestException("CNPJ já cadastrado");
        }

        if (request.getGestor() != null && !request.getGestor().getEmail().equalsIgnoreCase(instituicao.getGestor() != null ? instituicao.getGestor().getEmail() : "")
            && gestorRepository.findByEmail(request.getGestor().getEmail()).isPresent()) {
            throw new BadRequestException("Email do gestor já cadastrado");
        }

        instituicao.setNome(request.getNome());
        instituicao.setCnpj(request.getCnpj());
        instituicao.setEndereco(request.getEndereco());
        instituicao.setCidade(request.getCidade());
        instituicao.setEstado(request.getEstado());
        instituicao.setAtualizadoEm(LocalDateTime.now());

        if (request.getGestor() != null) {
            Gestor gestor = instituicao.getGestor();
            if (gestor == null) {
                gestor = new Gestor();
                gestor.setInstituicao(instituicao);
            }

            gestor.setNome(request.getGestor().getNome());
            gestor.setEmail(request.getGestor().getEmail());
            gestor.setTelefone(request.getGestor().getTelefone());
            if (request.getGestor().getSenha() != null && !request.getGestor().getSenha().isBlank()) {
                gestor.setSenha(passwordEncoder.encode(request.getGestor().getSenha()));
            }
            gestor.setAtualizadoEm(LocalDateTime.now());
            instituicao.setGestor(gestor);
        }

        Instituicao atualizada = instituicaoRepository.save(instituicao);
        log.info("Instituição atualizada com sucesso: {}", id);

        return responseMapper.toInstituicaoResponse(atualizada);
    }

    @Transactional
    public void deletar(UUID id) {
        log.info("Deletando instituição: {}", id);

        Instituicao instituicao = instituicaoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Instituição não encontrada"));

        if (instituicao.getUsers() != null && !instituicao.getUsers().isEmpty()) {
            throw new BadRequestException("Não é possível deletar uma instituição que possui usuários");
        }

        instituicaoRepository.deleteById(id);
        log.info("Instituição deletada com sucesso: {}", id);
    }

}
