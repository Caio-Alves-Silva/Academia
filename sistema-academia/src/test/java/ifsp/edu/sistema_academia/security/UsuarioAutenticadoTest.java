package ifsp.edu.sistema_academia.security;

import ifsp.edu.sistema_academia.exception.AcessoNegadoException;
import ifsp.edu.sistema_academia.model.Perfil;
import ifsp.edu.sistema_academia.security.UsuarioAutenticado;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsuarioAutenticadoTest {

    @Test
    void alunoAcessaApenasOsProprioDados() {
        UsuarioAutenticado aluno = new UsuarioAutenticado(5L, "a@a.com", Perfil.ALUNO);

        assertThat(aluno.ehAluno()).isTrue();
        assertThatCode(() -> aluno.exigirAcessoAoAluno(5L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> aluno.exigirAcessoAoAluno(6L)).isInstanceOf(AcessoNegadoException.class);
    }

    @Test
    void equipeAcessaDadosDeQualquerAluno() {
        UsuarioAutenticado recepcao = new UsuarioAutenticado(1L, "r@a.com", Perfil.RECEPCAO);
        UsuarioAutenticado instrutor = new UsuarioAutenticado(2L, "i@a.com", Perfil.INSTRUTOR);

        assertThat(recepcao.ehAluno()).isFalse();
        assertThatCode(() -> recepcao.exigirAcessoAoAluno(99L)).doesNotThrowAnyException();
        assertThatCode(() -> instrutor.exigirAcessoAoAluno(99L)).doesNotThrowAnyException();
    }
}