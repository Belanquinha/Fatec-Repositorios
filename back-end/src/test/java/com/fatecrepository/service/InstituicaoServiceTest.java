package com.fatecrepository.service;

import com.fatecrepository.dto.response.PerfilInstituicaoResponse;
import com.fatecrepository.exception.ResourceNotFoundException;
import com.fatecrepository.mapper.ResponseMapper;
import com.fatecrepository.model.*;
import com.fatecrepository.repository.InstituicaoCursoRepository;
import com.fatecrepository.repository.InstituicaoRepository;
import com.fatecrepository.repository.ProjetoRepository;
import com.fatecrepository.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstituicaoServiceTest {

    @Mock
    private InstituicaoRepository instituicaoRepository;

    @Mock
    private InstituicaoCursoRepository instituicaoCursoRepository;

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private ResponseMapper responseMapper = new ResponseMapper();

    @InjectMocks
    private InstituicaoService instituicaoService;

    private Instituicao fatec;
    private Curso curso;
    private Projeto projeto;

    @BeforeEach
    void setUp() {
        fatec = new Instituicao();
        fatec.setId(UUID.randomUUID());
        fatec.setNome("Fatec Sorocaba");
        fatec.setCodigoUnidade("003");
        fatec.setCidade("Sorocaba");
        fatec.setAtivo(true);

        curso = new Curso();
        curso.setId(UUID.randomUUID());
        curso.setNome("Fabricação Mecânica");

        User autor = new User();
        autor.setId(UUID.randomUUID());
        autor.setNome("Aluno Sorocaba");

        projeto = new Projeto();
        projeto.setId(UUID.randomUUID());
        projeto.setTitulo("Projeto Mecânica");
        projeto.setEstado(ProjetoEstado.APROVADO);
        projeto.setInstituicao(fatec);
        projeto.setCurso(curso);
        projeto.setAutor(autor);
    }

    @Test
    @DisplayName("Deve retornar perfil público da instituição com cursos e projetos aprovados")
    void deveRetornarPerfilInstituicao() {
        when(instituicaoRepository.findById(fatec.getId())).thenReturn(Optional.of(fatec));
        when(instituicaoCursoRepository.buscarCursosAtivosPorInstituicao(fatec.getId())).thenReturn(List.of(curso));
        when(projetoRepository.findByInstituicaoIdAndEstadoOrderByCriadoEmDesc(fatec.getId(), ProjetoEstado.APROVADO))
            .thenReturn(List.of(projeto));

        PerfilInstituicaoResponse perfil = instituicaoService.obterPerfil(fatec.getId());

        assertNotNull(perfil);
        assertEquals(fatec.getId(), perfil.getId());
        assertEquals("Fatec Sorocaba", perfil.getNome());
        assertEquals(1, perfil.getCursos().size());
        assertEquals("Fabricação Mecânica", perfil.getCursos().get(0).getNome());
        assertEquals(1, perfil.getProjetos().size());
        assertEquals("Projeto Mecânica", perfil.getProjetos().get(0).getTitulo());
    }

    @Test
    @DisplayName("Deve lançar ResourceNotFoundException quando instituição não for encontrada")
    void deveLancarExceptionQuandoInstituicaoNaoEncontrada() {
        UUID idInexistente = UUID.randomUUID();
        when(instituicaoRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> instituicaoService.obterPerfil(idInexistente));
    }
}
