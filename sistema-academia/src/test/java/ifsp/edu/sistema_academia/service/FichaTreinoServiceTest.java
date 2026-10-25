package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.request.FichaTreinoRequest;
import ifsp.edu.sistema_academia.dto.request.ItemTreinoRequest;
import ifsp.edu.sistema_academia.dto.response.FichaTreinoResponse;
import ifsp.edu.sistema_academia.exception.AcessoNegadoException;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.model.FichaTreino;
import ifsp.edu.sistema_academia.model.ItemTreino;
import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.model.Usuario;
import ifsp.edu.sistema_academia.repository.FichaTreinoRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FichaTreinoServiceTest {

    @Mock
    FichaTreinoRepository fichaRepository;
    @Mock
    UsuarioRepository usuarioRepository;

    final Usuario aluno = Fixtures.aluno(1L);
    final Usuario instrutor = Fixtures.instrutor(50L);
    final UsuarioAutenticado instrutorLogado = new UsuarioAutenticado(50L, "i@a.com", Perfil.INSTRUTOR);
    final UsuarioAutenticado alunoLogado = new UsuarioAutenticado(1L, "a@a.com", Perfil.ALUNO);
    FichaTreinoService service;

    @BeforeEach
    void setUp() {
        service = new FichaTreinoService(fichaRepository, usuarioRepository, Fixtures.relogio(Fixtures.AGORA));
        when(usuarioRepository.findByIdAndPerfilForUpdate(1L, Perfil.ALUNO)).thenReturn(Optional.of(aluno));
        when(usuarioRepository.findByIdAndPerfil(1L, Perfil.ALUNO)).thenReturn(Optional.of(aluno));
        when(usuarioRepository.findById(50L)).thenReturn(Optional.of(instrutor));
        when(fichaRepository.save(any(FichaTreino.class))).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 8L));
    }

    private FichaTreinoRequest request() {
        return new FichaTreinoRequest("  foco em pernas ",
                List.of(new ItemTreinoRequest(" Agachamento ", 4, 12, new BigDecimal("60")),
                        new ItemTreinoRequest("Prancha", 3, 1, BigDecimal.ZERO)));
    }

    @Test
    @DisplayName("nova ficha arquiva a atual anterior")
    void novaFichaArquivaAAnterior() {
        FichaTreino anterior = new FichaTreino(aluno, instrutor, Fixtures.AGORA.minusDays(30), null,
                List.of(new ItemTreino("Remada", 3, 10, new BigDecimal("30.00"))));
        when(fichaRepository.findByAlunoIdAndAtualTrue(1L)).thenReturn(Optional.of(anterior));

        FichaTreinoResponse resposta = service.criar(1L, request(), instrutorLogado);

        assertThat(anterior.isAtual()).isFalse();
        assertThat(resposta.atual()).isTrue();
        assertThat(resposta.observacoes()).isEqualTo("foco em pernas");
        assertThat(resposta.instrutorId()).isEqualTo(50L);
        assertThat(resposta.itens()).hasSize(2);
        assertThat(resposta.itens().get(0).exercicio()).isEqualTo("Agachamento");
        assertThat(resposta.itens().get(0).cargaKg()).isEqualByComparingTo("60.00");
    }

    @Test
    void primeiraFichaNaoTemAnterior() {
        when(fichaRepository.findByAlunoIdAndAtualTrue(1L)).thenReturn(Optional.empty());

        assertThat(service.criar(1L, new FichaTreinoRequest(" ", request().itens()), instrutorLogado)
                .observacoes()).isNull();
    }

    @Test
    void alunoInexistente() {
        assertThatThrownBy(() -> service.criar(99L, request(), instrutorLogado))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void alunoVeAFichaAtualEOHistorico() {
        FichaTreino atual = Fixtures.comId(new FichaTreino(aluno, instrutor, Fixtures.AGORA, null,
                List.of(new ItemTreino("Remada", 3, 10, new BigDecimal("30.00")))), 9L);
        FichaTreino velha = Fixtures.comId(new FichaTreino(aluno, instrutor, Fixtures.AGORA.minusDays(30), null,
                List.of(new ItemTreino("Remada", 3, 10, new BigDecimal("20.00")))), 7L);
        velha.arquivar();
        when(fichaRepository.findByAlunoIdAndAtualTrue(1L)).thenReturn(Optional.of(atual));
        when(fichaRepository.findByAlunoIdAndAtualFalseOrderByCriadaEmDescIdDesc(1L)).thenReturn(List.of(velha));

        assertThat(service.buscarAtual(1L, alunoLogado).id()).isEqualTo(9L);
        assertThat(service.listarHistorico(1L, alunoLogado)).extracting(FichaTreinoResponse::id).containsExactly(7L);
        assertThat(service.listarHistorico(1L, instrutorLogado)).hasSize(1);
    }

    @Test
    void alunoNaoVeFichaDeOutroAluno() {
        UsuarioAutenticado outro = new UsuarioAutenticado(2L, "o@a.com", Perfil.ALUNO);

        assertThatThrownBy(() -> service.buscarAtual(1L, outro)).isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> service.listarHistorico(1L, outro)).isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void semFichaAtual() {
        when(fichaRepository.findByAlunoIdAndAtualTrue(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarAtual(1L, alunoLogado))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}