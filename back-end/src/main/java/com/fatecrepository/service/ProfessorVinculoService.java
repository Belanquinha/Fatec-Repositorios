package com.fatecrepository.service;

import com.fatecrepository.dto.response.InstituicaoResponse;
import com.fatecrepository.exception.BadRequestException;
import com.fatecrepository.exception.ConflictException;
import com.fatecrepository.exception.ResourceNotFoundException;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.Instituicao;
import com.fatecrepository.model.ProfessorInstituicao;
import com.fatecrepository.repository.InstituicaoRepository;
import com.fatecrepository.repository.ProfessorInstituicaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfessorVinculoService {

    public static final int MAX_VINCULOS = 4;
    public static final String MENSAGEM_TETO = "Máximo de 4 instituições por professor";

    private final ProfessorInstituicaoRepository vinculoRepository;
    private final InstituicaoRepository instituicaoRepository;
    private final ResponseMapper responseMapper;

    /**
     * Lê os vínculos de um professor.
     *
     * <p>O {@code usuarioId} deve vir sempre do token autenticado
     * (ver {@code ProfessorController}) — nunca de parâmetro do request.
     */

    @Transactional(readOnly = true)
    public List<InstituicaoResponse> ler(UUID usuarioId) {
        return vinculoRepository.buscarComInstituicao(usuarioId).stream()
            .map(ProfessorInstituicao::getInstituicao)
            .filter(Objects::nonNull)
            .sorted(ordenacao())
            .map(responseMapper::toInstituicaoResponse)
            .toList();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<InstituicaoResponse> substituir(UUID usuarioId, List<UUID> instituicaoIds) {
        if (instituicaoIds == null) {
            throw new BadRequestException("instituicaoIds é obrigatório (use [] para limpar os vínculos)");
        }
        if (instituicaoIds.stream().anyMatch(Objects::isNull)) {
            throw new BadRequestException("instituicaoIds contém id nulo");
        }
        if (new HashSet<>(instituicaoIds).size() != instituicaoIds.size()) {
            throw new BadRequestException("Instituição duplicada na lista de vínculos");
        }
        if (instituicaoIds.size() > MAX_VINCULOS) {
            throw new ConflictException(MENSAGEM_TETO);
        }

        Map<UUID, Instituicao> porId = instituicaoRepository.findAllById(instituicaoIds).stream()
            .collect(Collectors.toMap(Instituicao::getId, Function.identity()));
        List<Instituicao> instituicoes = new ArrayList<>();
        for (UUID id : instituicaoIds) {
            Instituicao instituicao = porId.get(id);
            if (instituicao == null) {
                throw new ResourceNotFoundException("Instituição não encontrada");
            }
            instituicoes.add(instituicao);
        }
        instituicoes.sort(ordenacao());

        try {
            vinculoRepository.deleteByUsuarioId(usuarioId);
            vinculoRepository.flush();
            for (Instituicao instituicao : instituicoes) {
                ProfessorInstituicao vinculo = new ProfessorInstituicao();
                vinculo.setUsuarioId(usuarioId);
                vinculo.setInstituicao(instituicao);
                vinculoRepository.save(vinculo);
            }
            vinculoRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Conflito ao persistir vínculos (duplicado ou limite excedido)", e);
        }

        log.info("Vínculos do professor {} substituídos: {} instituições", usuarioId, instituicoes.size());
        return instituicoes.stream().map(responseMapper::toInstituicaoResponse).toList();
    }

    private static Comparator<Instituicao> ordenacao() {
        return Comparator.comparing(Instituicao::getNome, Comparator.nullsLast(String::compareToIgnoreCase))
            .thenComparing(Instituicao::getCidade, Comparator.nullsLast(String::compareToIgnoreCase))
            .thenComparing(Instituicao::getId, Comparator.nullsLast(UUID::compareTo));
    }
}
