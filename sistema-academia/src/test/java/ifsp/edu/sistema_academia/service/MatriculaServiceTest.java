package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.model.Aluno;

import ifsp.edu.sistema_academia.dto.request.MatriculaRequest;
import ifsp.edu.sistema_academia.dto.request.TrocaPlanoRequest;
import ifsp.edu.sistema_academia.dto.response.CancelamentoMatriculaResponse;
import ifsp.edu.sistema_academia.dto.response.MatriculaResponse;
import ifsp.edu.sistema_academia.dto.response.TrocaPlanoResponse;
import ifsp.edu.sistema_academia.exception.AcessoNegadoException;
import ifsp.edu.sistema_academia.exception.MatriculaAtivaExistenteException;
import ifsp.edu.sistema_academia.exception.MatriculaNaoAtivaException;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.exception.RegraNegocioException;
import ifsp.edu.sistema_academia.model.Matricula;
import ifsp.edu.sistema_academia.model.Modalidade;
import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.model.Plano;
import ifsp.edu.sistema_academia.model.StatusMatricula;
import ifsp.edu.sistema_academia.model.StatusPagamento;
import ifsp.edu.sistema_academia.model.Usuario;
import ifsp.edu.sistema_academia.repository.MatriculaRepository;
import ifsp.edu.sistema_academia.repository.PlanoRepository;
import ifsp.edu.sistema_academia.repository.AlunoRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
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
class MatriculaServiceTest {

    @Mock
    MatriculaRepository matriculaRepository;
    @Mock
    AlunoRepository alunoRepository;
    @Mock
    PlanoRepository planoRepository;

    final Aluno aluno = Fixtures.aluno(1L);
    final Plano trimestral = Fixtures.plano(10L, 3, "300.00", 3, Modalidade.SPINNING);
    final Plano mensal = Fixtures.plano(11L, 1, "100.00", 3, Modalidade.SPINNING);
    final Plano caro = Fixtures.plano(12L, 3, "400.00", 3, Modalidade.SPINNING);

    MatriculaService service;

    @BeforeEach
    void setUp() {
        service = servicoEm(Fixtures.AGORA);
        when(alunoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(aluno));
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(planoRepository.findById(10L)).thenReturn(Optional.of(trimestral));
        when(planoRepository.findById(11L)).thenReturn(Optional.of(mensal));
        when(planoRepository.findById(12L)).thenReturn(Optional.of(caro));
        when(matriculaRepository.save(any(Matricula.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private MatriculaService servicoEm(LocalDateTime agora) {
        return new MatriculaService(matriculaRepository, alunoRepository, planoRepository,
                Fixtures.relogio(agora));
    }

    private Matricula matriculaPagaEm(LocalDate inicio) {
        Matricula matricula = Fixtures.matricula(5L, aluno, trimestral, inicio);
        matricula.getMensalidades().get(0).pagar(inicio);
        when(matriculaRepository.findAlunoIdById(5L)).thenReturn(Optional.of(1L));
        when(matriculaRepository.findById(5L)).thenReturn(Optional.of(matricula));
        return matricula;
    }

    @Test
    @DisplayName("matricular gera todas as mensalidades da vigência")
    void matricular() {
        when(matriculaRepository.findAtiva(1L, LocalDate.of(2026, 3, 11))).thenReturn(Optional.empty());

        MatriculaResponse resposta = service.matricular(new MatriculaRequest(1L, 10L));

        assertThat(resposta.status()).isEqualTo(StatusMatricula.ATIVA);
        assertThat(resposta.dataInicio()).isEqualTo(LocalDate.of(2026, 3, 11));
        assertThat(resposta.dataFim()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(resposta.mensalidades()).hasSize(3);
        assertThat(resposta.mensalidades().get(1).vencimento()).isEqualTo(LocalDate.of(2026, 4, 11));
    }

    @Test
    @DisplayName("aluno com matrícula ativa não pode se matricular de novo")
    void apenasUmaMatriculaAtiva() {
        when(matriculaRepository.findAtiva(1L, LocalDate.of(2026, 3, 11)))
                .thenReturn(Optional.of(Fixtures.matricula(5L, aluno, trimestral, LocalDate.of(2026, 3, 1))));

        assertThatThrownBy(() -> service.matricular(new MatriculaRequest(1L, 10L)))
                .isInstanceOf(MatriculaAtivaExistenteException.class);
        verify(matriculaRepository, never()).save(any());
    }

    @Test
    void matricularAlunoOuPlanoInexistente() {
        assertThatThrownBy(() -> service.matricular(new MatriculaRequest(99L, 10L)))
                .isInstanceOf(RecursoNaoEncontradoException.class).hasMessageContaining("Aluno");
        assertThatThrownBy(() -> service.matricular(new MatriculaRequest(1L, 99L)))
                .isInstanceOf(RecursoNaoEncontradoException.class).hasMessageContaining("Plano");
    }

    @Test
    @DisplayName("troca de plano: crédito proporcional vira desconto na 1ª mensalidade da nova matrícula")
    void trocarPlano() {
        service = servicoEm(LocalDateTime.of(2026, 3, 21, 10, 0));
        Matricula atual = matriculaPagaEm(LocalDate.of(2026, 3, 11));
        when(matriculaRepository.findAtiva(1L, LocalDate.of(2026, 3, 21))).thenReturn(Optional.of(atual));

        TrocaPlanoResponse resposta = service.trocarPlano(5L, new TrocaPlanoRequest(12L));

        assertThat(resposta.creditoCalculado()).isEqualByComparingTo("193.55");
        assertThat(resposta.descontoAplicado()).isEqualByComparingTo("193.55");
        assertThat(resposta.mensalidadesCanceladas()).isEqualTo(2);
        assertThat(resposta.matriculaEncerrada().status()).isEqualTo(StatusMatricula.ENCERRADA);
        assertThat(resposta.matriculaEncerrada().mensalidades()).extracting("status")
                .containsExactly(StatusPagamento.PAGO, StatusPagamento.CANCELADO, StatusPagamento.CANCELADO);
        assertThat(resposta.novaMatricula().status()).isEqualTo(StatusMatricula.ATIVA);
        assertThat(resposta.novaMatricula().plano().id()).isEqualTo(12L);
        assertThat(resposta.novaMatricula().dataInicio()).isEqualTo(LocalDate.of(2026, 3, 21));
        assertThat(resposta.novaMatricula().mensalidades().get(0).valor()).isEqualByComparingTo("206.45");
        assertThat(resposta.novaMatricula().mensalidades().get(1).valor()).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("o desconto da troca não ultrapassa o valor da primeira mensalidade")
    void descontoLimitadoAoValorDaPrimeira() {
        service = servicoEm(LocalDateTime.of(2026, 3, 21, 10, 0));
        Matricula atual = matriculaPagaEm(LocalDate.of(2026, 3, 11));
        when(matriculaRepository.findAtiva(1L, LocalDate.of(2026, 3, 21))).thenReturn(Optional.of(atual));

        TrocaPlanoResponse resposta = service.trocarPlano(5L, new TrocaPlanoRequest(11L)); // mensal de 100

        assertThat(resposta.creditoCalculado()).isEqualByComparingTo("193.55");
        assertThat(resposta.descontoAplicado()).isEqualByComparingTo("100.00");
        assertThat(resposta.novaMatricula().mensalidades().get(0).valor()).isEqualByComparingTo("0.00");
    }

    @Test
    void trocarParaOMesmoPlanoEhRecusado() {
        Matricula atual = matriculaPagaEm(LocalDate.of(2026, 3, 11));
        when(matriculaRepository.findAtiva(1L, LocalDate.of(2026, 3, 11))).thenReturn(Optional.of(atual));

        assertThatThrownBy(() -> service.trocarPlano(5L, new TrocaPlanoRequest(10L)))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("diferente");
        assertThat(atual.getStatusMatricula()).isEqualTo(StatusMatricula.ATIVA);
    }

    @Test
    void trocarMatriculaNaoAtivaOuInexistente() {
        Matricula cancelada = matriculaPagaEm(LocalDate.of(2026, 3, 1));
        cancelada.cancelar(LocalDate.of(2026, 3, 2));

        assertThatThrownBy(() -> service.trocarPlano(5L, new TrocaPlanoRequest(12L)))
                .isInstanceOf(MatriculaNaoAtivaException.class);
        assertThatThrownBy(() -> service.trocarPlano(404L, new TrocaPlanoRequest(12L)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("cancelar plano trimestral cobra 10% das mensalidades futuras canceladas")
    void cancelar() {
        Matricula atual = matriculaPagaEm(LocalDate.of(2026, 3, 11));

        CancelamentoMatriculaResponse resposta = service.cancelar(5L);

        assertThat(resposta.mensalidadesCanceladas()).isEqualTo(2);
        assertThat(resposta.valorMensalidadesCanceladas()).isEqualByComparingTo("600.00");
        assertThat(resposta.multaCancelamento()).isEqualByComparingTo("60.00");
        assertThat(resposta.matricula().status()).isEqualTo(StatusMatricula.CANCELADA);
        assertThat(atual.getStatusMatricula()).isEqualTo(StatusMatricula.CANCELADA);
    }

    @Test
    void cancelarMatriculaJaCancelada() {
        Matricula atual = matriculaPagaEm(LocalDate.of(2026, 3, 11));
        atual.cancelar(LocalDate.of(2026, 3, 11));

        assertThatThrownBy(() -> service.cancelar(5L)).isInstanceOf(MatriculaNaoAtivaException.class);
    }

    @Test
    @DisplayName("o aluno só consulta a própria matrícula")
    void consultaRestritaAoDono() {
        Matricula matricula = Fixtures.matricula(5L, aluno, trimestral, LocalDate.of(2026, 3, 11));
        when(matriculaRepository.findById(5L)).thenReturn(Optional.of(matricula));
        UsuarioAutenticado dono = new UsuarioAutenticado(1L, "a@a.com", Perfil.ALUNO);
        UsuarioAutenticado outro = new UsuarioAutenticado(2L, "b@a.com", Perfil.ALUNO);
        UsuarioAutenticado recepcao = new UsuarioAutenticado(3L, "r@a.com", Perfil.RECEPCAO);

        assertThat(service.buscar(5L, dono).id()).isEqualTo(5L);
        assertThat(service.buscar(5L, recepcao).id()).isEqualTo(5L);
        assertThatThrownBy(() -> service.buscar(5L, outro)).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> service.buscarAtiva(1L, outro)).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> service.listarPorAluno(1L, outro)).isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void buscarAtivaEListar() {
        Matricula matricula = Fixtures.matricula(5L, aluno, trimestral, LocalDate.of(2026, 3, 11));
        when(matriculaRepository.findAtiva(1L, LocalDate.of(2026, 3, 11))).thenReturn(Optional.of(matricula));
        when(matriculaRepository.findByAlunoIdOrderByDataInicioDescIdDesc(1L)).thenReturn(List.of(matricula));
        UsuarioAutenticado recepcao = new UsuarioAutenticado(3L, "r@a.com", Perfil.RECEPCAO);

        assertThat(service.buscarAtiva(1L, recepcao).id()).isEqualTo(5L);
        assertThat(service.listarPorAluno(1L, recepcao)).hasSize(1);

        when(matriculaRepository.findAtiva(1L, LocalDate.of(2026, 3, 11))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.buscarAtiva(1L, recepcao))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.buscar(404L, recepcao)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}