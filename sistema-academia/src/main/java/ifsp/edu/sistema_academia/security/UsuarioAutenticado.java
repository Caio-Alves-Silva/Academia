package ifsp.edu.sistema_academia.security;

import ifsp.edu.sistema_academia.exception.AcessoNegadoException;
import ifsp.edu.sistema_academia.model.Perfil;

/** Usuário identificado pelo token JWT da requisição corrente. */
public record UsuarioAutenticado(Long id, String email, Perfil perfil) {

    public boolean ehAluno() {
        return perfil == Perfil.ALUNO;
    }

    /**
     * Regra geral de acesso: o aluno só acessa os próprios dados. Recepção e instrutor passam (a restrição por
     * perfil de cada endpoint é feita antes, via {@code @PreAuthorize}).
     */
    public void exigirAcessoAoAluno(Long alunoId) {
        if (ehAluno() && !id.equals(alunoId)) {
            throw new AcessoNegadoException("O aluno só pode acessar os próprios dados");
        }
    }
}