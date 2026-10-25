package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.request.AulaRequest;
import ifsp.edu.sistema_academia.dto.response.AulaResponse;
import ifsp.edu.sistema_academia.exception.ConflitoHorarioInstrutorException;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.exception.RegraNegocioException;
import ifsp.edu.sistema_academia.model.AulaColetiva;
import ifsp.edu.sistema_academia.model.Modalidade;
import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.model.Usuario;
import ifsp.edu.sistema_academia.repository.AulaColetivaRepository;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AulaServiceTest {

    @Mock
    AulaColetivaRepository aulaRepository;
    @Mock
    UsuarioRepository usuarioRepository;

    final Usuario instrutor = Fixtures.instrutor(50L);
    AulaService service;

    @BeforeEach
    void setUp() {
        service = new AulaService(aulaRepository, usuarioRepository, Fixtures.relogio(Fixtures.AGORA));
        when(usuarioRepository.findByIdAndPerfilForUpdate(50L, Perfil.INSTRUTOR)).thenReturn(Optional.of(instrutor));
        when(aulaRepository.save(any(AulaColetiva.class))).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 1L));
    }

    private AulaRequest request(DayOfWeek dia, LocalTime horario, int duracao) {
        return new AulaRequest(Modalidade.SPINNING, 50L, dia, horario, duracao, 15);
    }

    @Test
    void criaAula() {
        AulaResponse resposta = service.criar(request(DayOfWeek.MONDAY, LocalTime.of(18, 0), 50));

        assertThat(resposta.horarioFim()).isEqualTo(LocalTime.of(18, 50));
        assertThat(resposta.instrutorNome()).isEqualTo(instrutor.getNome());
        assertThat(resposta.capacidade()).isEqualTo(15);
    }

    @Test
    @DisplayName("um instrutor não pode ter duas aulas sobrepostas")
    void conflitoDeHorario() {
        AulaColetiva existente = Fixtures.aula(9L, instrutor, Modalidade.YOGA, DayOfWeek.MONDAY,
                LocalTime.of(18, 30), 60, 10, LocalDate.of(2026, 3, 1));
        when(aulaRepository.findByInstrutorIdAndDiaSemana(50L, DayOfWeek.MONDAY)).thenReturn(List.of(existente));

        assertThatThrownBy(() -> service.criar(request(DayOfWeek.MONDAY, LocalTime.of(18, 0), 50)))
                .isInstanceOf(ConflitoHorarioInstrutorException.class);
        verify(aulaRepository, never()).save(any());
    }

    @Test
    void aulaEncostadaNaOutraNaoConflita() {
        AulaColetiva existente = Fixtures.aula(9L, instrutor, Modalidade.YOGA, DayOfWeek.MONDAY,
                LocalTime.of(18, 50), 60, 10, LocalDate.of(2026, 3, 1));
        when(aulaRepository.findByInstrutorIdAndDiaSemana(50L, DayOfWeek.MONDAY)).thenReturn(List.of(existente));

        assertThat(service.criar(request(DayOfWeek.MONDAY, LocalTime.of(18, 0), 50)).id()).isEqualTo(1L);
    }

    @Test
    void aulaNaoPodeAtravessarAMeiaNoite() {
        assertThatThrownBy(() -> service.criar(request(DayOfWeek.MONDAY, LocalTime.of(23, 30), 60)))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(service.criar(request(DayOfWeek.MONDAY, LocalTime.of(23, 0), 60)).horarioFim())
                .isEqualTo(LocalTime.MIDNIGHT);
    }

    @Test
    void instrutorInexistente() {
        AulaRequest request = new AulaRequest(Modalidade.SPINNING, 99L, DayOfWeek.MONDAY, LocalTime.of(18, 0), 50, 15);

        assertThatThrownBy(() -> service.criar(request)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void listarOrdenadoPorDiaEHorario() {
        AulaColetiva sexta = Fixtures.aula(1L, instrutor, Modalidade.YOGA, DayOfWeek.FRIDAY, LocalTime.of(7, 0), 50,
                10, LocalDate.of(2026, 3, 1));
        AulaColetiva segundaTarde = Fixtures.aula(2L, instrutor, Modalidade.YOGA, DayOfWeek.MONDAY,
                LocalTime.of(19, 0), 50, 10, LocalDate.of(2026, 3, 1));
        AulaColetiva segundaManha = Fixtures.aula(3L, instrutor, Modalidade.YOGA, DayOfWeek.MONDAY,
                LocalTime.of(7, 0), 50, 10, LocalDate.of(2026, 3, 1));
        when(aulaRepository.findAll()).thenReturn(List.of(sexta, segundaTarde, segundaManha));
        when(aulaRepository.findByModalidade(Modalidade.YOGA)).thenReturn(List.of(sexta));

        assertThat(service.listar(null)).extracting(AulaResponse::id).containsExactly(3L, 2L, 1L);
        assertThat(service.listar(Modalidade.YOGA)).extracting(AulaResponse::id).containsExactly(1L);
    }

    @Test
    void buscar() {
        when(aulaRepository.findById(1L)).thenReturn(Optional.of(
                Fixtures.aula(1L, instrutor, Modalidade.YOGA, DayOfWeek.FRIDAY, LocalTime.of(7, 0), 50, 10,
                        LocalDate.of(2026, 3, 1))));

        assertThat(service.buscar(1L).modalidade()).isEqualTo(Modalidade.YOGA);
        assertThatThrownBy(() -> service.buscar(2L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}