package com.fatecrepository.service;

import com.fatecrepository.dto.response.InstituicaoResponse;
import com.fatecrepository.exception.BadRequestException;
import com.fatecrepository.exception.ConflictException;
import com.fatecrepository.exception.ResourceNotFoundException;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.Instituicao;
import com.fatecrepository.repository.InstituicaoRepository;
import com.fatecrepository.repository.ProfessorInstituicaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Story 2.3 — ProfessorVinculoService (regras 0–4)")
class ProfessorVinculoServiceTest {

    @Mock
    private ProfessorInstituicaoRepository vinculoRepository;

    @Mock
    private InstituicaoRepository instituicaoRepository;

    @Spy
    private ResponseMapper responseMapper = new ResponseMapper();

    @InjectMocks
    private ProfessorVinculoService service;

    private Instituicao instituicao(String nome) {
        Instituicao i = new Instituicao();
        i.setId(UUID.randomUUID());
        i.setCodigoUnidade("COD-" + nome);
        i.setNome(nome);
        i.setCidade("São Paulo");
        i.setEstado("SP");
        i.setAtivo(true);
        return i;
    }

    @Test
    @DisplayName("Substituir com 2 válidos persiste e retorna ordenado por nome")
    void substituirComDoisValidos() {
        UUID usuarioId = UUID.randomUUID();
        Instituicao b = instituicao("Fatec B");
        Instituicao a = instituicao("Fatec A");
        when(instituicaoRepository.findAllById(List.of(b.getId(), a.getId()))).thenReturn(List.of(b, a));

        List<InstituicaoResponse> resultado = service.substituir(usuarioId, List.of(b.getId(), a.getId()));

        assertThat(resultado).extracting(InstituicaoResponse::getNome)
            .containsExactly("Fatec A", "Fatec B");
        verify(vinculoRepository).deleteByUsuarioId(usuarioId);
        verify(vinculoRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    @DisplayName("Substituir com [] apaga tudo e retorna vazio")
    void substituirComZeroLimpa() {
        UUID usuarioId = UUID.randomUUID();

        List<InstituicaoResponse> resultado = service.substituir(usuarioId, List.of());

        assertThat(resultado).isEmpty();
        verify(vinculoRepository).deleteByUsuarioId(usuarioId);
        verify(vinculoRepository, never()).save(any());
    }

    @Test
    @DisplayName("5 ids → 409 e nada persistido")
    void cincoIdsDa409() {
        UUID usuarioId = UUID.randomUUID();
        List<UUID> cinco = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), UUID.randomUUID());

        assertThatThrownBy(() -> service.substituir(usuarioId, cinco))
            .isInstanceOf(ConflictException.class)
            .hasMessage("Máximo de 4 instituições por professor");
        verify(vinculoRepository, never()).deleteByUsuarioId(any());
        verify(vinculoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Id duplicado → 400 e nada persistido")
    void idDuplicadoDa400() {
        UUID usuarioId = UUID.randomUUID();
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.substituir(usuarioId, List.of(id, id)))
            .isInstanceOf(BadRequestException.class);
        verify(vinculoRepository, never()).deleteByUsuarioId(any());
    }

    @Test
    @DisplayName("Id inexistente → 404 e nada persistido (atomicidade)")
    void idInexistenteDa404() {
        UUID usuarioId = UUID.randomUUID();
        Instituicao valida = instituicao("Fatec Válida");
        UUID fantasma = UUID.randomUUID();
        when(instituicaoRepository.findAllById(List.of(valida.getId(), fantasma)))
            .thenReturn(List.of(valida));

        assertThatThrownBy(() -> service.substituir(usuarioId, List.of(valida.getId(), fantasma)))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(vinculoRepository, never()).deleteByUsuarioId(any());
        verify(vinculoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lista nula → 400")
    void listaNulaDa400() {
        assertThatThrownBy(() -> service.substituir(UUID.randomUUID(), null))
            .isInstanceOf(BadRequestException.class);
    }
}
