package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.model.Aluno;

import ifsp.edu.sistema_academia.dto.request.PlanoRequest;
import ifsp.edu.sistema_academia.dto.response.PlanoResponse;
import ifsp.edu.sistema_academia.dto.response.UsuarioResponse;
import ifsp.edu.sistema_academia.exception.AcessoNegadoException;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.model.Modalidade;
import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.model.Plano;
import ifsp.edu.sistema_academia.repository.PlanoRepository;
import ifsp.edu.sistema_academia.repository.AlunoRepository;
import ifsp.edu.sistema_academia.security.UsuarioAutenticado;
import ifsp.edu.sistema_academia.support.Fixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlanoEUsuarioServiceTest {

    @Mock
    PlanoRepository planoRepository;
    @Mock
    AlunoRepository alunoRepository;

    @Test
    void criaListaEBuscaPlanos() {
        PlanoService service = new PlanoService(planoRepository);
        when(planoRepository.save(any(Plano.class))).thenAnswer(inv -> Fixtures.comId(inv.getArgument(0), 3L));
        Plano existente = Fixtures.plano(1L, 1, "99.90", 3, Modalidade.SPINNING);
        when(planoRepository.findAllByOrderByDuracaoMesesAscNomeAsc()).thenReturn(List.of(existente));
        when(planoRepository.findById(1L)).thenReturn(Optional.of(existente));

        PlanoResponse criado = service.criar(new PlanoRequest(" Trimestral ", 3, new BigDecimal("129.9"),
                Set.of(Modalidade.SPINNING, Modalidade.YOGA), 4));

        assertThat(criado.nome()).isEqualTo("Trimestral");
        assertThat(criado.valorMensal()).isEqualByComparingTo("129.90");
        assertThat(criado.modalidades()).containsExactly(Modalidade.SPINNING, Modalidade.YOGA);
        assertThat(criado.limiteAulasSemana()).isEqualTo(4);
        assertThat(service.listar()).hasSize(1);
        assertThat(service.buscar(1L).duracaoMeses()).isEqualTo(1);
        assertThatThrownBy(() -> service.buscar(2L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void planosDeTresEDozeMesesTemMultaDeCancelamento() {
        assertThat(Fixtures.plano(1L, 1, "10", 1, Modalidade.YOGA).temMultaDeCancelamento()).isFalse();
        assertThat(Fixtures.plano(1L, 3, "10", 1, Modalidade.YOGA).temMultaDeCancelamento()).isTrue();
        assertThat(Fixtures.plano(1L, 12, "10", 1, Modalidade.YOGA).temMultaDeCancelamento()).isTrue();
    }

    @Test
    void consultaDeAlunos() {
        UsuarioService service = new UsuarioService(alunoRepository);
        var aluno = Fixtures.aluno(1L);
        when(alunoRepository.findAllByOrderByNomeAsc()).thenReturn(List.of(aluno));
        when(alunoRepository.findByNomeContainingIgnoreCaseOrderByNomeAsc("alu"))
                .thenReturn(List.of(aluno));
        when(alunoRepository.findById(1L)).thenReturn(Optional.of(aluno));

        assertThat(service.listarAlunos(null)).hasSize(1);
        assertThat(service.listarAlunos("  ")).hasSize(1);
        assertThat(service.listarAlunos(" alu ")).hasSize(1);

        UsuarioResponse resposta = service.buscarAluno(1L, new UsuarioAutenticado(1L, "a@a.com", Perfil.ALUNO));
        assertThat(resposta.email()).isEqualTo(aluno.getEmail());
        assertThat(service.buscarAluno(1L, new UsuarioAutenticado(9L, "r@a.com", Perfil.RECEPCAO)).id()).isEqualTo(1L);
        assertThatThrownBy(() -> service.buscarAluno(1L, new UsuarioAutenticado(2L, "b@a.com", Perfil.ALUNO)))
                .isInstanceOf(AcessoNegadoException.class);
        assertThatThrownBy(() -> service.buscarAluno(5L, new UsuarioAutenticado(9L, "r@a.com", Perfil.RECEPCAO)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}