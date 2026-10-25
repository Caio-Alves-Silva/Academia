package ifsp.edu.sistema_academia.dto.request;

import ifsp.edu.sistema_academia.model.TipoAcesso;
import jakarta.validation.constraints.NotNull;

/** {@code tipoAcesso} é opcional (padrão: RECEPCAO). */
public record CheckinRequest(@NotNull(message = "O aluno é obrigatório") Long alunoId, TipoAcesso tipoAcesso) {

    public CheckinRequest(Long alunoId) {
        this(alunoId, null);
    }
}