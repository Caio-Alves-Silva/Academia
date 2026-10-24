package ifsp.edu.sistema_academia.dto.response;

import ifsp.edu.sistema_academia.model.ItemTreino;

import java.math.BigDecimal;

public record ItemTreinoResponse(String exercicio, int series, int repeticoes, BigDecimal cargaKg) {

    public static ItemTreinoResponse from(ItemTreino item) {
        return new ItemTreinoResponse(item.getExercicio(), item.getSeries(), item.getRepeticoes(),
                item.getCargaKg());
    }
}