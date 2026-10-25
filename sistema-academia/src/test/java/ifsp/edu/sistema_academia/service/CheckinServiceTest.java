package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.request.CheckinRequest;
import ifsp.edu.sistema_academia.dto.response.CheckinResponse;
import ifsp.edu.sistema_academia.exception.AlunoBloqueadoException;
import ifsp.edu.sistema_academia.exception.MatriculaNaoAtivaException;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.model.Checkin;
import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.model.Usuario;
import ifsp.edu.sistema_academia.repository.CheckinRepository;
import ifsp.edu.sistema_academia.repository.UsuarioRepository;
import ifsp.edu.sistema_academia.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CheckinServiceTest {

    @Mock
    CheckinRepository checkinRepository;
    @Mock
    UsuarioRepository usuarioRepository;
    @Mock
    SituacaoAlunoService situacaoAlunoService;

    final Usuario aluno = Fixtures.aluno(1L);
    CheckinService service;

    @BeforeEach
    void setUp() {
        service = new CheckinService(checkinRepository, usuarioRepository, situacaoAlunoService,
                Fixtures.relogio(Fixtures.AGORA));
        when(usuarioRepository.findByIdAndPerfil(1L, Perfil.ALUNO)).thenReturn(Optional.of(aluno));
        when(checkinRepository.save(any(Checkin.class))).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 5L));
    }

    @Test
    void registraEntrada() {
        CheckinResponse resposta = service.registrar(new CheckinRequest(1L));

        assertThat(resposta.alunoId()).isEqualTo(1L);
        assertThat(resposta.dataHora()).isEqualTo(Fixtures.AGORA);
        verify(situacaoAlunoService).exigirLiberado(1L);
    }

    @Test
    @DisplayName("aluno bloqueado ou sem matrícula ativa é recusado e nada é gravado")
    void recusaBloqueadoOuSemMatricula() {
        when(situacaoAlunoService.exigirLiberado(1L)).thenThrow(new AlunoBloqueadoException());
        assertThatThrownBy(() -> service.registrar(new CheckinRequest(1L))).isInstanceOf(AlunoBloqueadoException.class);

        org.mockito.Mockito.doThrow(MatriculaNaoAtivaException.doAluno(1L)).when(situacaoAlunoService).exigirLiberado(1L);
        assertThatThrownBy(() -> service.registrar(new CheckinRequest(1L)))
                .isInstanceOf(MatriculaNaoAtivaException.class);
        verify(checkinRepository, never()).save(any());
    }

    @Test
    void alunoInexistente() {
        assertThatThrownBy(() -> service.registrar(new CheckinRequest(99L)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}