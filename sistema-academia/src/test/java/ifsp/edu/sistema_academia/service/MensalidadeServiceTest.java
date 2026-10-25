package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.response.MensalidadeResponse;
import ifsp.edu.sistema_academia.exception.AcessoNegadoException;
import ifsp.edu.sistema_academia.exception.MensalidadeCanceladaException;
import ifsp.edu.sistema_academia.exception.MensalidadeJaPagaException;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.model.Matricula;
import ifsp.edu.sistema_academia.model.Mensalidade;
import ifsp.edu.sistema_academia.model.Modalidade;
import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.model.StatusMensalidade;
import ifsp.edu.sistema_academia.model.Usuario;
import ifsp.edu.sistema_academia.repository.MensalidadeRepository;
import ifsp.edu.sistema_academia.repository.UsuarioRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MensalidadeServiceTest {

    @Mock
    MensalidadeRepository mensalidadeRepository;
    @Mock
    UsuarioRepository usuarioRepository;

    final Usuario aluno = Fixtures.aluno(1L);
    final UsuarioAutenticado dono = new UsuarioAutenticado(1L, "a@a.com", Perfil.ALUNO);
    final UsuarioAutenticado outro = new UsuarioAutenticado(2L, "b@a.com", Perfil.ALUNO);
    final UsuarioAutenticado recepcao = new UsuarioAutenticado(3L, "r@a.com", Perfil.RECEPCAO);

    Matricula matricula;
    Mensalidade primeira;

    MensalidadeService servicoEm(LocalDateTime agora) {
        return new MensalidadeService(mensalidadeRepository, usuarioRepository, Fixtures.relogio(agora));
    }

    @BeforeEach
    void setUp() {
        matricula = Fixtures.matricula(5L, aluno,
                Fixtures.plano(10L, 3, "100.00", 3, Modalidade.SPINNING), LocalDate.of(2026, 3, 11));
        primeira = matricula.getMensalidades().get(0);
        when(mensalidadeRepository.findByIdForUpdate(501L)).thenReturn(Optional.of(primeira));
        when(mensalidadeRepository.findById(501L)).thenReturn(Optional.of(primeira));
        when(usuarioRepository.findByIdAndPerfil(1L, Perfil.ALUNO)).thenReturn(Optional.of(aluno));
    }

    @Test
    @DisplayName("pagamento em dia não tem multa")
    void pagarEmDia() {
        MensalidadeResponse resposta = servicoEm(Fixtures.AGORA).pagar(501L, dono);

        assertThat(resposta.status()).isEqualTo(StatusMensalidade.PAGA);
        assertThat(resposta.multa()).isEqualByComparingTo("0.00");
        assertThat(resposta.valorPago()).isEqualByComparingTo("100.00");
        assertThat(resposta.valorAPagarHoje()).isNull();
    }

    @Test
    @DisplayName("pagamento após o vencimento tem multa de 2%")
    void pagarComAtraso() {
        MensalidadeResponse resposta = servicoEm(LocalDateTime.of(2026, 3, 14, 9, 0)).pagar(501L, recepcao);

        assertThat(resposta.multa()).isEqualByComparingTo("2.00");
        assertThat(resposta.valorPago()).isEqualByComparingTo("102.00");
        assertThat(resposta.dataPagamento()).isEqualTo(LocalDate.of(2026, 3, 14));
    }

    @Test
    void naoPagaDuasVezes() {
        MensalidadeService service = servicoEm(Fixtures.AGORA);
        service.pagar(501L, dono);

        assertThatThrownBy(() -> service.pagar(501L, dono)).isInstanceOf(MensalidadeJaPagaException.class);
    }

    @Test
    void naoPagaMensalidadeCancelada() {
        primeira.cancelar();

        assertThatThrownBy(() -> servicoEm(Fixtures.AGORA).pagar(501L, dono))
                .isInstanceOf(MensalidadeCanceladaException.class);
    }

    @Test
    @DisplayName("o aluno só paga e consulta as próprias mensalidades")
    void restritoAoDono() {
        MensalidadeService service = servicoEm(Fixtures.AGORA);

        assertThatThrownBy(() -> service.pagar(501L, outro)).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> service.buscar(501L, outro)).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> service.listarPorAluno(1L, null, outro))
                .isInstanceOf(AcessoNegadoException.class);
        assertThat(primeira.getStatus()).isEqualTo(StatusMensalidade.PENDENTE);
    }

    @Test
    void mensalidadeInexistente() {
        MensalidadeService service = servicoEm(Fixtures.AGORA);

        assertThatThrownBy(() -> service.pagar(999L, dono)).isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.buscar(999L, dono)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void consultaMostraValorAPagarComMulta() {
        MensalidadeResponse resposta = servicoEm(LocalDateTime.of(2026, 3, 14, 9, 0)).buscar(501L, dono);

        assertThat(resposta.diasEmAtraso()).isEqualTo(3);
        assertThat(resposta.valorAPagarHoje()).isEqualByComparingTo("102.00");
    }

    @Test
    void listaComEsemFiltroDeStatus() {
        MensalidadeService service = servicoEm(Fixtures.AGORA);
        when(mensalidadeRepository.findByMatriculaAlunoIdOrderByVencimentoAscIdAsc(1L))
                .thenReturn(matricula.getMensalidades());
        when(mensalidadeRepository.findByMatriculaAlunoIdAndStatusOrderByVencimentoAscIdAsc(
                1L, StatusMensalidade.PAGA)).thenReturn(List.of());

        assertThat(service.listarPorAluno(1L, null, dono)).hasSize(3);
        assertThat(service.listarPorAluno(1L, StatusMensalidade.PAGA, recepcao)).isEmpty();
        assertThatThrownBy(() -> service.listarPorAluno(7L, null, recepcao))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}