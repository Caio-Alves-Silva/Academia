package ifsp.edu.sistema_academia.dto.response;

import ifsp.edu.sistema_academia.model.Modalidade;
import ifsp.edu.sistema_academia.model.Plano;

import java.math.BigDecimal;
import java.util.Set;
import java.util.TreeSet;

public record PlanoResponse(Long id, String nome, int duracaoMeses, BigDecimal valorMensal,
                            Set<Modalidade> modalidades, int limiteAulasSemana) {

    public static PlanoResponse from(Plano plano) {
        return new PlanoResponse(plano.getId(), plano.getNome(), plano.getDuracaoMeses(), plano.getValorMensal(),
                new TreeSet<>(plano.getModalidades()), plano.getLimiteAulasSemana());
    }
}