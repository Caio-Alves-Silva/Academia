package ifsp.edu.sistema_academia.dto.response;

import ifsp.edu.sistema_academia.model.FichaTreino;

import java.time.LocalDateTime;
import java.util.List;

public record FichaTreinoResponse(Long id, Long alunoId, Long instrutorId, String instrutorNome,
                                  LocalDateTime criadaEm, boolean atual, String observacoes,
                                  List<ItemTreinoResponse> itens) {

    public static FichaTreinoResponse from(FichaTreino ficha) {
        return new FichaTreinoResponse(ficha.getId(), ficha.getAluno().getId(), ficha.getInstrutor().getId(),
                ficha.getInstrutor().getNome(), ficha.getCriadaEm(), ficha.isAtual(), ficha.getObservacoes(),
                ficha.getItens().stream().map(ItemTreinoResponse::from).toList());
    }
}