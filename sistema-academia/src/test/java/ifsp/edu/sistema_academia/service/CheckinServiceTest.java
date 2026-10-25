package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.model.Aluno;

import ifsp.edu.sistema_academia.dto.request.CheckinRequest;
import ifsp.edu.sistema_academia.dto.response.CheckinResponse;
import ifsp.edu.sistema_academia.exception.AlunoBloqueadoException;
import ifsp.edu.sistema_academia.exception.MatriculaNaoAtivaException;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.model.Checkin;
import ifsp.edu.sistema_academia.model.TipoAcesso;
import org.mockito.ArgumentCaptor;
import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.model.Usuario;
import ifsp.edu.sistema_academia.repository.CheckinRepository;
import ifsp.edu.sistema_academia.repository.AlunoRepository;
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
    AlunoRepository alunoRepository;
    @Mock
    SituacaoAlunoService situacaoAlunoService;

    final Aluno aluno = Fixtures.aluno(1L);
    CheckinService service;

    @BeforeEach
    void setUp() {
        service = new CheckinService(checkinRepository, alunoRepository, situacaoAlunoService,
                Fixtures.relogio(Fixtures.AGORA));
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(checkinRepository.save(any(Checkin.class))).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 5L));
    }

    @Test
    void registraEntrada() {
        CheckinResponse resposta = service.registrar(new CheckinRequest(1L));

        assertThat(resposta.alunoId()).isEqualTo(1L);
        assertThat(resposta.dataHoraEntrada()).isEqualTo(Fixtures.AGORA);
        assertThat(resposta.acessoLiberado()).isTrue();
        assertThat(resposta.motivoBloqueio()).isNull();
        assertThat(resposta.tipoAcesso()).isEqualTo(TipoAcesso.RECEPCAO);
        verify(situacaoAlunoService).exigirLiberado(1L);
    }

    @Test
    @DisplayName("aluno bloqueado ou sem matrícula ativa é recusado, mas a tentativa é gravada com o motivo")
    void recusaBloqueadoOuSemMatricula() {
        when(situacaoAlunoService.exigirLiberado(1L)).thenThrow(new AlunoBloqueadoException());
        assertThatThrownBy(() -> service.registrar(new CheckinRequest(1L, TipoAcesso.CATRACA)))
                .isInstanceOf(AlunoBloqueadoException.class);

        ArgumentCaptor<Checkin> gravado = ArgumentCaptor.forClass(Checkin.class);
        verify(checkinRepository).save(gravado.capture());
        assertThat(gravado.getValue().getAcessoLiberado()).isFalse();
        assertThat(gravado.getValue().getTipoAcesso()).isEqualTo(TipoAcesso.CATRACA);
        assertThat(gravado.getValue().getMotivoBloqueio()).contains("mensalidade vencida");

        org.mockito.Mockito.reset(checkinRepository);
        org.mockito.Mockito.doThrow(MatriculaNaoAtivaException.doAluno(1L)).when(situacaoAlunoService).exigirLiberado(1L);
        assertThatThrownBy(() -> service.registrar(new CheckinRequest(1L)))
                .isInstanceOf(MatriculaNaoAtivaException.class);
        verify(checkinRepository).save(gravado.capture());
        assertThat(gravado.getValue().getAcessoLiberado()).isFalse();
        assertThat(gravado.getValue().getMotivoBloqueio()).contains("não possui matrícula ativa");
    }

    @Test
    void alunoInexistente() {
        assertThatThrownBy(() -> service.registrar(new CheckinRequest(99L)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}