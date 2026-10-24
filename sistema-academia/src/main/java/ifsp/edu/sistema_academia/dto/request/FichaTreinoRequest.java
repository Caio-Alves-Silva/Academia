package ifsp.edu.sistema_academia.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record FichaTreinoRequest(
        @Size(max = 500, message = "As observações devem ter no máximo 500 caracteres")
        String observacoes,

        @NotEmpty(message = "A ficha precisa ter ao menos um exercício")
        List<@Valid ItemTreinoRequest> itens) {
}