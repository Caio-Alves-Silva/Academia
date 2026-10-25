package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.exception.AlunoBloqueadoException;
import ifsp.edu.sistema_academia.exception.MatriculaNaoAtivaException;
import ifsp.edu.sistema_academia.model.Matricula;
import ifsp.edu.sistema_academia.model.Modalidade;
import ifsp.edu.sistema_academia.model.StatusPagamento;
import ifsp.edu.sistema_academia.repository.MatriculaRepository;
import ifsp.edu.sistema_academia.repository.MensalidadeRepository;
import ifsp.edu.sistema_academia.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SituacaoAlunoServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 3, 11);

    @Mock
    MatriculaRepository matriculaRepository;
    @Mock
    MensalidadeRepository mensalidadeRepository;

    SituacaoAlunoService service;
    Matricula matricula;

    @BeforeEach
    void setUp() {
        service = new SituacaoAlunoService(matriculaRepository, mensalidadeRepository,
                Fixtures.relogio(Fixtures.AGORA));
        matricula = Fixtures.matricula(5L, Fixtures.aluno(1L),
                Fixtures.plano(10L, 3, "100.00", 3, Modalidade.SPINNING), HOJE);
    }

    @Test
    @DisplayName("bloqueio considera mensalidades pendentes vencidas antes de hoje - 5 dias")
    void bloqueioUsaLimiteDeCincoDias() {
        when(mensalidadeRepository.existsByMatriculaAlunoIdAndStatusPagamentoAndVencimentoBefore(
                1L, StatusPagamento.PENDENTE, HOJE.minusDays(5))).thenReturn(true);

        assertThat(service.estaBloqueado(1L)).isTrue();
        verify(mensalidadeRepository).existsByMatriculaAlunoIdAndStatusPagamentoAndVencimentoBefore(
                1L, StatusPagamento.PENDENTE, LocalDate.of(2026, 3, 6));
        assertThat(service.estaBloqueado(2L)).isFalse();
    }

    @Test
    void exigirLiberadoSemMatricula() {
        when(matriculaRepository.findAtiva(1L, HOJE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.exigirLiberado(1L)).isInstanceOf(MatriculaNaoAtivaException.class);
        assertThat(service.podeFrequentar(1L)).isFalse();
    }

    @Test
    void exigirLiberadoBloqueado() {
        when(matriculaRepository.findAtiva(1L, HOJE)).thenReturn(Optional.of(matricula));
        when(mensalidadeRepository.existsByMatriculaAlunoIdAndStatusPagamentoAndVencimentoBefore(
                1L, StatusPagamento.PENDENTE, HOJE.minusDays(5))).thenReturn(true);

        assertThatThrownBy(() -> service.exigirLiberado(1L)).isInstanceOf(AlunoBloqueadoException.class);
        assertThat(service.podeFrequentar(1L)).isFalse();
    }

    @Test
    void exigirLiberadoDevolveAMatricula() {
        when(matriculaRepository.findAtiva(1L, HOJE)).thenReturn(Optional.of(matricula));

        assertThat(service.exigirLiberado(1L)).isSameAs(matricula);
        assertThat(service.podeFrequentar(1L)).isTrue();
        assertThat(service.buscarMatriculaAtiva(1L)).contains(matricula);
        assertThatCode(() -> service.exigirLiberado(1L)).doesNotThrowAnyException();
    }
}