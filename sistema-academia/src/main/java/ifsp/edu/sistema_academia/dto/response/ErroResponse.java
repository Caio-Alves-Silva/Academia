package ifsp.edu.sistema_academia.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** Formato único de erro da API. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(Instant timestamp, int status, String erro, String mensagem, String caminho,
                           List<CampoInvalido> campos) {

    /** Campo que falhou na validação da requisição. */
    public record CampoInvalido(String campo, String mensagem) {
    }
}