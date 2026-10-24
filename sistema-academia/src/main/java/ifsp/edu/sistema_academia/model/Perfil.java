package ifsp.edu.sistema_academia.model;

/**
 * Perfis de acesso do sistema. Alunos se cadastram pela API; recepção e instrutores
 * têm contas criadas na carga inicial.
 */
public enum Perfil {
    ALUNO,
    RECEPCAO,
    INSTRUTOR;

    /** Nome da authority usada pelo Spring Security (ex.: ROLE_ALUNO). */
    public String authority() {
        return "ROLE_" + name();
    }
}
