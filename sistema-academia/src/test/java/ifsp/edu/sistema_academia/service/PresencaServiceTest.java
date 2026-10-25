package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.request.PresencaRequest;
import ifsp.edu.sistema_academia.dto.response.PresencaResponse;
import ifsp.edu.sistema_academia.exception.AcessoNegadoException;
import ifsp.edu.sistema_academia.exception.DataAulaInvalidaException;
import ifsp.edu.sistema_academia.exception.PresencaInvalidaException;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.model.AulaColetiva;
import ifsp.edu.sistema_academia.model.Inscricao;
import ifsp.edu.sistema_academia.model.Modalidade;
import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.model.Presenca;
import ifsp.edu.sistema_academia.model.StatusInscricao;
import ifsp.edu.sistema_academia.repository.AulaColetivaRepository;
import ifsp.edu.sistema_academia.repository.InscricaoRepository;
import ifsp.edu.sistema_academia.security.UsuarioAutenticado;
import ifsp.edu.sistema_academia.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PresencaServiceTest {

    /** Aula de segunda às 18h; "agora" é quarta 11/03, então a ocorrência de 09/03 já aconteceu. */
    private static final LocalDate SEGUNDA = LocalDate.of(2026, 3, 9);

    @Mock
    AulaColetivaRepository aulaRepository;
    @Mock
    InscricaoRepository inscricaoRepository;

    final AulaColetiva aula = Fixtures.aula(20L, Fixtures.instrutor(50L), Modalidade.SPINNING, DayOfWeek.MONDAY,
            LocalTime.of(18, 0), 50, 10, LocalDate.of(2026, 3, 1));
    final UsuarioAutenticado instrutor = new UsuarioAutenticado(50L, "i@a.com", Perfil.INSTRUTOR);

    PresencaService service;
    Inscricao i1;
    Inscricao i2;
    Inscricao i3;

    @BeforeEach
    void setUp() {
        service = new PresencaService(aulaRepository, inscricaoRepository, Fixtures.relogio(Fixtures.AGORA));
        when(aulaRepository.findById(20L)).thenReturn(Optional.of(aula));
        i1 = new Inscricao(aula, Fixtures.aluno(1L), SEGUNDA, StatusInscricao.CONFIRMADA, Fixtures.AGORA);
        i2 = new Inscricao(aula, Fixtures.aluno(2L), SEGUNDA, StatusInscricao.CONFIRMADA, Fixtures.AGORA);
        i3 = new Inscricao(aula, Fixtures.aluno(3L), SEGUNDA, StatusInscricao.CONFIRMADA, Fixtures.AGORA);
        when(inscricaoRepository.findByAulaIdAndDataAulaAndStatusOrderByIdAsc(
                20L, SEGUNDA, StatusInscricao.CONFIRMADA)).thenReturn(List.of(i1, i2, i3));
    }

    @Test
    @DisplayName("presentes recebem presença e os demais confirmados recebem falta")
    void registraPresencaEFaltas() {
        PresencaResponse resposta = service.registrar(20L, new PresencaRequest(SEGUNDA, List.of(1L, 2L)), instrutor);

        assertThat(i1.getPresenca()).isEqualTo(Presenca.PRESENTE);
        assertThat(i2.getPresenca()).isEqualTo(Presenca.PRESENTE);
        assertThat(i3.getPresenca()).isEqualTo(Presenca.FALTA);
        assertThat(resposta.presentes()).isEqualTo(2);
        assertThat(resposta.faltas()).isEqualTo(1);
        assertThat(resposta.inscricoes()).hasSize(3);
    }

    @Test
    void lancamentoPodeSerCorrigido() {
        service.registrar(20L, new PresencaRequest(SEGUNDA, List.of()), instrutor);
        assertThat(i1.getPresenca()).isEqualTo(Presenca.FALTA);

        service.registrar(20L, new PresencaRequest(SEGUNDA, List.of(1L)), instrutor);
        assertThat(i1.getPresenca()).isEqualTo(Presenca.PRESENTE);
    }

    @Test
    void alunoSemInscricaoConfirmadaNaListaDePresenca() {
        assertThatThrownBy(() -> service.registrar(20L, new PresencaRequest(SEGUNDA, List.of(1L, 9L)), instrutor))
                .isInstanceOf(PresencaInvalidaException.class).hasMessageContaining("[9]");
        assertThat(i1.getPresenca()).isNull();
    }

    @Test
    @DisplayName("somente o instrutor da aula registra a presença")
    void outroInstrutor() {
        UsuarioAutenticado outro = new UsuarioAutenticado(51L, "o@a.com", Perfil.INSTRUTOR);

        assertThatThrownBy(() -> service.registrar(20L, new PresencaRequest(SEGUNDA, List.of()), outro))
                .isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void aulaQueAindaNaoAconteceuOuDiaErrado() {
        assertThatThrownBy(() -> service.registrar(20L,
                new PresencaRequest(LocalDate.of(2026, 3, 16), List.of()), instrutor))
                .isInstanceOf(DataAulaInvalidaException.class).hasMessageContaining("ainda não começou");
        assertThatThrownBy(() -> service.registrar(20L,
                new PresencaRequest(LocalDate.of(2026, 3, 10), List.of()), instrutor))
                .isInstanceOf(DataAulaInvalidaException.class);
    }

    @Test
    void aulaInexistente() {
        assertThatThrownBy(() -> service.registrar(404L, new PresencaRequest(SEGUNDA, List.of()), instrutor))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
