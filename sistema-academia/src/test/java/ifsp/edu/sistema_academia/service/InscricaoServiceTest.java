package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.request.InscricaoRequest;
import ifsp.edu.sistema_academia.dto.response.CancelamentoInscricaoResponse;
import ifsp.edu.sistema_academia.dto.response.InscricaoResponse;
import ifsp.edu.sistema_academia.exception.AcessoNegadoException;
import ifsp.edu.sistema_academia.exception.AlunoBloqueadoException;
import ifsp.edu.sistema_academia.exception.BloqueioPorFaltasException;
import ifsp.edu.sistema_academia.exception.DataAulaInvalidaException;
import ifsp.edu.sistema_academia.exception.InscricaoDuplicadaException;
import ifsp.edu.sistema_academia.exception.InscricaoJaCanceladaException;
import ifsp.edu.sistema_academia.exception.LimiteSemanalAtingidoException;
import ifsp.edu.sistema_academia.exception.MatriculaNaoAtivaException;
import ifsp.edu.sistema_academia.exception.ModalidadeNaoIncluidaException;
import ifsp.edu.sistema_academia.exception.PrazoCancelamentoExpiradoException;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.model.AulaColetiva;
import ifsp.edu.sistema_academia.model.Inscricao;
import ifsp.edu.sistema_academia.model.Matricula;
import ifsp.edu.sistema_academia.model.Modalidade;
import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.model.Plano;
import ifsp.edu.sistema_academia.model.Presenca;
import ifsp.edu.sistema_academia.model.StatusInscricao;
import ifsp.edu.sistema_academia.model.Usuario;
import ifsp.edu.sistema_academia.repository.AulaColetivaRepository;
import ifsp.edu.sistema_academia.repository.InscricaoRepository;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InscricaoServiceTest {

    /** Hoje é quarta 11/03/2026, 10h. A sexta da semana corrente é 13/03. */
    private static final LocalDate SEXTA = LocalDate.of(2026, 3, 13);
    private static final LocalDate HOJE = LocalDate.of(2026, 3, 11);

    @Mock
    InscricaoRepository inscricaoRepository;
    @Mock
    AulaColetivaRepository aulaRepository;
    @Mock
    UsuarioRepository usuarioRepository;
    @Mock
    SituacaoAlunoService situacaoAlunoService;

    final Usuario aluno = Fixtures.aluno(1L);
    final Usuario instrutor = Fixtures.instrutor(50L);
    final Plano plano = Fixtures.plano(10L, 3, "100.00", 2, Modalidade.SPINNING, Modalidade.YOGA);
    final Matricula matricula = Fixtures.matricula(7L, aluno, plano, LocalDate.of(2026, 3, 1));
    final AulaColetiva aula = Fixtures.aula(20L, instrutor, Modalidade.SPINNING, DayOfWeek.FRIDAY,
            LocalTime.of(18, 0), 50, 2, LocalDate.of(2026, 3, 1));
    final UsuarioAutenticado logado = new UsuarioAutenticado(1L, "a@a.com", Perfil.ALUNO);

    InscricaoService service;

    @BeforeEach
    void setUp() {
        service = new InscricaoService(inscricaoRepository, aulaRepository, usuarioRepository,
                situacaoAlunoService, Fixtures.relogio(Fixtures.AGORA));
        when(usuarioRepository.findByIdAndPerfilForUpdate(1L, Perfil.ALUNO)).thenReturn(Optional.of(aluno));
        when(usuarioRepository.findByIdAndPerfil(1L, Perfil.ALUNO)).thenReturn(Optional.of(aluno));
        when(aulaRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(aula));
        when(aulaRepository.findById(20L)).thenReturn(Optional.of(aula));
        when(situacaoAlunoService.exigirLiberado(1L)).thenReturn(matricula);
        when(inscricaoRepository.save(any(Inscricao.class)))
                .thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 99L));
    }

    private InscricaoResponse inscrever(LocalDate data) {
        return service.inscrever(20L, new InscricaoRequest(data), logado);
    }

    private Inscricao inscricao(Long id, Usuario dono, StatusInscricao status, AulaColetiva da, LocalDate data) {
        Inscricao inscricao = Fixtures.comId(new Inscricao(da, dono, data, status, Fixtures.AGORA), id);
        when(inscricaoRepository.findAulaIdById(id)).thenReturn(Optional.of(da.getId()));
        when(inscricaoRepository.findById(id)).thenReturn(Optional.of(inscricao));
        return inscricao;
    }

    // ---------- RF09: inscrição ----------

    @Test
    @DisplayName("com vaga disponível a inscrição é confirmada")
    void inscricaoConfirmada() {
        when(inscricaoRepository.countByAulaIdAndDataAulaAndStatus(20L, SEXTA, StatusInscricao.CONFIRMADA))
                .thenReturn(1L);

        InscricaoResponse resposta = inscrever(SEXTA);

        assertThat(resposta.status()).isEqualTo(StatusInscricao.CONFIRMADA);
        assertThat(resposta.posicaoListaEspera()).isNull();
        assertThat(resposta.dataAula()).isEqualTo(SEXTA);
        assertThat(resposta.alunoId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("sem vaga o aluno entra na lista de espera, por ordem de chegada")
    void listaDeEspera() {
        when(inscricaoRepository.countByAulaIdAndDataAulaAndStatus(20L, SEXTA, StatusInscricao.CONFIRMADA))
                .thenReturn(2L);
        when(inscricaoRepository.countByAulaIdAndDataAulaAndStatusAndIdLessThanEqual(
                20L, SEXTA, StatusInscricao.LISTA_ESPERA, 99L)).thenReturn(3L);

        InscricaoResponse resposta = inscrever(SEXTA);

        assertThat(resposta.status()).isEqualTo(StatusInscricao.LISTA_ESPERA);
        assertThat(resposta.posicaoListaEspera()).isEqualTo(3L);
    }

    @Test
    void dataDeOutroDiaDaSemana() {
        assertThatThrownBy(() -> inscrever(LocalDate.of(2026, 3, 12)))
                .isInstanceOf(DataAulaInvalidaException.class).hasMessageContaining("FRIDAY");
    }

    @Test
    @DisplayName("só aceita datas da semana corrente")
    void foraDaSemanaCorrente() {
        assertThatThrownBy(() -> inscrever(LocalDate.of(2026, 3, 20)))   // sexta da semana seguinte
                .isInstanceOf(DataAulaInvalidaException.class).hasMessageContaining("semana corrente");
        assertThatThrownBy(() -> inscrever(LocalDate.of(2026, 3, 6)))    // sexta da semana passada
                .isInstanceOf(DataAulaInvalidaException.class).hasMessageContaining("semana corrente");
    }

    @Test
    void aulaQueJaComecou() {
        AulaColetiva jaComecou = Fixtures.aula(21L, instrutor, Modalidade.SPINNING, DayOfWeek.WEDNESDAY,
                LocalTime.of(9, 0), 50, 5, LocalDate.of(2026, 3, 1));
        when(aulaRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(jaComecou));

        assertThatThrownBy(() -> service.inscrever(21L, new InscricaoRequest(HOJE), logado))
                .isInstanceOf(DataAulaInvalidaException.class).hasMessageContaining("já começou");
    }

    @Test
    void aulaDeHojeQueAindaNaoComecouAceitaInscricao() {
        AulaColetiva aTarde = Fixtures.aula(21L, instrutor, Modalidade.SPINNING, DayOfWeek.WEDNESDAY,
                LocalTime.of(15, 0), 50, 5, LocalDate.of(2026, 3, 1));
        when(aulaRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(aTarde));

        assertThat(service.inscrever(21L, new InscricaoRequest(HOJE), logado).status())
                .isEqualTo(StatusInscricao.CONFIRMADA);
    }

    @Test
    @DisplayName("a modalidade da aula precisa estar no plano")
    void modalidadeForaDoPlano() {
        AulaColetiva luta = Fixtures.aula(22L, instrutor, Modalidade.LUTA, DayOfWeek.FRIDAY,
                LocalTime.of(19, 0), 50, 5, LocalDate.of(2026, 3, 1));
        when(aulaRepository.findByIdForUpdate(22L)).thenReturn(Optional.of(luta));

        assertThatThrownBy(() -> service.inscrever(22L, new InscricaoRequest(SEXTA), logado))
                .isInstanceOf(ModalidadeNaoIncluidaException.class).hasMessageContaining("LUTA");
    }

    @Test
    @DisplayName("limite semanal do plano")
    void limiteSemanal() {
        when(inscricaoRepository.countByAlunoIdAndDataAulaBetweenAndStatusIn(
                eq(1L), eq(LocalDate.of(2026, 3, 9)), eq(LocalDate.of(2026, 3, 15)), any())).thenReturn(2L);

        assertThatThrownBy(() -> inscrever(SEXTA)).isInstanceOf(LimiteSemanalAtingidoException.class)
                .hasMessageContaining("2");
    }

    @Test
    @DisplayName("2 faltas na semana anterior impedem a inscrição; 1 falta não")
    void bloqueioPorFaltas() {
        when(inscricaoRepository.countByAlunoIdAndDataAulaBetweenAndPresenca(
                1L, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 8), Presenca.FALTA)).thenReturn(1L);
        assertThat(inscrever(SEXTA).status()).isEqualTo(StatusInscricao.CONFIRMADA);

        when(inscricaoRepository.countByAlunoIdAndDataAulaBetweenAndPresenca(
                1L, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 8), Presenca.FALTA)).thenReturn(2L);
        assertThatThrownBy(() -> inscrever(SEXTA)).isInstanceOf(BloqueioPorFaltasException.class);
    }

    @Test
    void inscricaoDuplicada() {
        when(inscricaoRepository.existsByAulaIdAndAlunoIdAndDataAulaAndStatusIn(
                eq(20L), eq(1L), eq(SEXTA), any())).thenReturn(true);

        assertThatThrownBy(() -> inscrever(SEXTA)).isInstanceOf(InscricaoDuplicadaException.class);
    }

    @Test
    void alunoBloqueadoOuSemMatriculaNaoSeInscreve() {
        when(situacaoAlunoService.exigirLiberado(1L)).thenThrow(new AlunoBloqueadoException());
        assertThatThrownBy(() -> inscrever(SEXTA)).isInstanceOf(AlunoBloqueadoException.class);

        org.mockito.Mockito.doThrow(MatriculaNaoAtivaException.doAluno(1L)).when(situacaoAlunoService).exigirLiberado(1L);
        assertThatThrownBy(() -> inscrever(SEXTA)).isInstanceOf(MatriculaNaoAtivaException.class);
    }

    @Test
    void matriculaQueTerminaAntesDaAula() {
        Matricula curta = Fixtures.matricula(8L, aluno, Fixtures.plano(11L, 1, "100.00", 3, Modalidade.SPINNING),
                LocalDate.of(2026, 2, 12)); // termina em 11/03
        when(situacaoAlunoService.exigirLiberado(1L)).thenReturn(curta);

        assertThatThrownBy(() -> inscrever(SEXTA)).isInstanceOf(MatriculaNaoAtivaException.class);
    }

    @Test
    void aulaOuAlunoInexistente() {
        assertThatThrownBy(() -> service.inscrever(404L, new InscricaoRequest(SEXTA), logado))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        assertThatThrownBy(() -> service.inscrever(20L, new InscricaoRequest(SEXTA),
                new UsuarioAutenticado(77L, "x@x.com", Perfil.ALUNO)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ---------- RF10: cancelamento ----------

    @Test
    @DisplayName("cancelar uma vaga promove o primeiro da lista de espera que ainda está apto")
    void cancelamentoPromoveOPrimeiroAptoDaFila() {
        Inscricao confirmada = inscricao(30L, aluno, StatusInscricao.CONFIRMADA, aula, SEXTA);
        Inscricao primeiro = inscricao(31L, Fixtures.aluno(2L), StatusInscricao.LISTA_ESPERA, aula, SEXTA);
        Inscricao segundo = inscricao(32L, Fixtures.aluno(3L), StatusInscricao.LISTA_ESPERA, aula, SEXTA);
        Inscricao terceiro = inscricao(33L, Fixtures.aluno(4L), StatusInscricao.LISTA_ESPERA, aula, SEXTA);
        when(inscricaoRepository.findByAulaIdAndDataAulaAndStatusOrderByIdAsc(
                20L, SEXTA, StatusInscricao.LISTA_ESPERA)).thenReturn(List.of(primeiro, segundo, terceiro));
        when(situacaoAlunoService.podeFrequentar(2L)).thenReturn(false); // ficou inadimplente
        when(situacaoAlunoService.podeFrequentar(3L)).thenReturn(true);

        CancelamentoInscricaoResponse resposta = service.cancelar(30L, logado);

        assertThat(confirmada.getStatus()).isEqualTo(StatusInscricao.CANCELADA);
        assertThat(resposta.inscricaoCancelada().status()).isEqualTo(StatusInscricao.CANCELADA);
        assertThat(resposta.inscricaoPromovida().id()).isEqualTo(32L);
        assertThat(resposta.inscricaoPromovida().status()).isEqualTo(StatusInscricao.CONFIRMADA);
        assertThat(segundo.getStatus()).isEqualTo(StatusInscricao.CONFIRMADA);
        assertThat(primeiro.getStatus()).isEqualTo(StatusInscricao.LISTA_ESPERA);
        assertThat(terceiro.getStatus()).isEqualTo(StatusInscricao.LISTA_ESPERA);
    }

    @Test
    void cancelamentoSemListaDeEspera() {
        inscricao(30L, aluno, StatusInscricao.CONFIRMADA, aula, SEXTA);

        CancelamentoInscricaoResponse resposta = service.cancelar(30L, logado);

        assertThat(resposta.inscricaoPromovida()).isNull();
    }

    @Test
    void cancelarNaListaDeEsperaNaoLiberaVaga() {
        Inscricao naFila = inscricao(31L, aluno, StatusInscricao.LISTA_ESPERA, aula, SEXTA);
        Inscricao outro = inscricao(32L, Fixtures.aluno(3L), StatusInscricao.LISTA_ESPERA, aula, SEXTA);
        when(inscricaoRepository.findByAulaIdAndDataAulaAndStatusOrderByIdAsc(
                20L, SEXTA, StatusInscricao.LISTA_ESPERA)).thenReturn(List.of(naFila, outro));
        when(situacaoAlunoService.podeFrequentar(3L)).thenReturn(true);

        CancelamentoInscricaoResponse resposta = service.cancelar(31L, logado);

        assertThat(resposta.inscricaoPromovida()).isNull();
        assertThat(outro.getStatus()).isEqualTo(StatusInscricao.LISTA_ESPERA);
    }

    @Test
    @DisplayName("cancelamento permitido até exatamente 2 horas antes; depois disso, recusado")
    void prazoDeDuasHoras() {
        AulaColetiva as12h = Fixtures.aula(21L, instrutor, Modalidade.SPINNING, DayOfWeek.WEDNESDAY,
                LocalTime.of(12, 0), 50, 5, LocalDate.of(2026, 3, 1));
        AulaColetiva as1159 = Fixtures.aula(22L, instrutor, Modalidade.SPINNING, DayOfWeek.WEDNESDAY,
                LocalTime.of(11, 59), 50, 5, LocalDate.of(2026, 3, 1));
        when(aulaRepository.findByIdForUpdate(21L)).thenReturn(Optional.of(as12h));
        when(aulaRepository.findByIdForUpdate(22L)).thenReturn(Optional.of(as1159));
        Inscricao noLimite = inscricao(40L, aluno, StatusInscricao.CONFIRMADA, as12h, HOJE);
        Inscricao tarde = inscricao(41L, aluno, StatusInscricao.CONFIRMADA, as1159, HOJE);

        assertThat(service.cancelar(40L, logado).inscricaoCancelada().status())
                .isEqualTo(StatusInscricao.CANCELADA);
        assertThatThrownBy(() -> service.cancelar(41L, logado))
                .isInstanceOf(PrazoCancelamentoExpiradoException.class);
        assertThat(noLimite.getStatus()).isEqualTo(StatusInscricao.CANCELADA);
        assertThat(tarde.getStatus()).isEqualTo(StatusInscricao.CONFIRMADA);
    }

    @Test
    void cancelarInscricaoJaCancelada() {
        inscricao(30L, aluno, StatusInscricao.CANCELADA, aula, SEXTA);

        assertThatThrownBy(() -> service.cancelar(30L, logado)).isInstanceOf(InscricaoJaCanceladaException.class);
    }

    @Test
    @DisplayName("o aluno só cancela a própria inscrição; a recepção cancela qualquer uma")
    void cancelamentoRestritoAoDono() {
        inscricao(30L, Fixtures.aluno(2L), StatusInscricao.CONFIRMADA, aula, SEXTA);

        assertThatThrownBy(() -> service.cancelar(30L, logado)).isInstanceOf(AcessoNegadoException.class);
        assertThat(service.cancelar(30L, new UsuarioAutenticado(3L, "r@a.com", Perfil.RECEPCAO))
                .inscricaoCancelada().status()).isEqualTo(StatusInscricao.CANCELADA);
    }

    @Test
    void cancelarInscricaoInexistente() {
        assertThatThrownBy(() -> service.cancelar(404L, logado)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ---------- consultas ----------

    @Test
    void listarPorAlunoEPorOcorrencia() {
        Inscricao confirmada = inscricao(30L, aluno, StatusInscricao.CONFIRMADA, aula, SEXTA);
        Inscricao naFila = inscricao(31L, aluno, StatusInscricao.LISTA_ESPERA, aula, SEXTA);
        when(inscricaoRepository.findByAlunoIdOrderByDataAulaDescIdDesc(1L))
                .thenReturn(List.of(naFila, confirmada));
        when(inscricaoRepository.findByAulaIdAndDataAulaAndStatusInOrderByIdAsc(eq(20L), eq(SEXTA), any()))
                .thenReturn(List.of(confirmada, naFila));
        when(inscricaoRepository.countByAulaIdAndDataAulaAndStatusAndIdLessThanEqual(
                20L, SEXTA, StatusInscricao.LISTA_ESPERA, 31L)).thenReturn(1L);

        assertThat(service.listarPorAluno(1L, logado)).hasSize(2);
        assertThat(service.listarPorOcorrencia(20L, SEXTA)).extracting(InscricaoResponse::posicaoListaEspera)
                .containsExactly(null, 1L);
        assertThatThrownBy(() -> service.listarPorAluno(2L, logado)).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> service.listarPorOcorrencia(20L, LocalDate.of(2026, 3, 12)))
                .isInstanceOf(DataAulaInvalidaException.class);
        assertThatThrownBy(() -> service.listarPorOcorrencia(404L, SEXTA))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
