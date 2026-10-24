package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.request.PlanoRequest;
import ifsp.edu.sistema_academia.dto.response.PlanoResponse;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.model.Plano;
import ifsp.edu.sistema_academia.repository.PlanoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;

/** RF03: cadastro de planos pela recepção. */
@Service
public class PlanoService {

    private final PlanoRepository planoRepository;

    public PlanoService(PlanoRepository planoRepository) {
        this.planoRepository = planoRepository;
    }

    @Transactional
    public PlanoResponse criar(PlanoRequest request) {
        Plano plano = new Plano(request.nome().trim(), request.duracaoMeses(),
                request.valorMensal().setScale(2, RoundingMode.HALF_UP), request.modalidades(),
                request.limiteAulasSemana());
        return PlanoResponse.from(planoRepository.save(plano));
    }

    @Transactional(readOnly = true)
    public List<PlanoResponse> listar() {
        return planoRepository.findAllByOrderByDuracaoMesesAscNomeAsc().stream().map(PlanoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public PlanoResponse buscar(Long id) {
        return planoRepository.findById(id)
                .map(PlanoResponse::from)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Plano", id));
    }
}