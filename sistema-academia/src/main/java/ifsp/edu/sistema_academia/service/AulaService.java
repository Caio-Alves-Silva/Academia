package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.request.AulaRequest;
import ifsp.edu.sistema_academia.dto.response.AulaResponse;
import ifsp.edu.sistema_academia.exception.ConflitoHorarioInstrutorException;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.exception.RegraNegocioException;
import ifsp.edu.sistema_academia.model.AulaColetiva;
import ifsp.edu.sistema_academia.model.Modalidade;
import ifsp.edu.sistema_academia.model.Instrutor;
import ifsp.edu.sistema_academia.repository.AulaColetivaRepository;
import ifsp.edu.sistema_academia.repository.InstrutorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** RF08: cadastro de aulas coletivas (um instrutor não pode ter duas aulas sobrepostas). */
@Service
public class AulaService {

    private static final int MINUTOS_NO_DIA = 24 * 60;

    private final AulaColetivaRepository aulaRepository;
    private final InstrutorRepository instrutorRepository;
    private final Clock clock;

    public AulaService(AulaColetivaRepository aulaRepository, InstrutorRepository instrutorRepository, Clock clock) {
        this.aulaRepository = aulaRepository;
        this.instrutorRepository = instrutorRepository;
        this.clock = clock;
    }

    @Transactional
    public AulaResponse criar(AulaRequest request) {
        if (request.horario().toSecondOfDay() / 60 + request.duracaoMinutos() > MINUTOS_NO_DIA) {
            throw new RegraNegocioException("A aula deve terminar até as 24h do mesmo dia");
        }
        // Lock no instrutor: duas criações simultâneas para o mesmo instrutor não furam a regra de sobreposição.
        Instrutor instrutor = instrutorRepository.findByIdForUpdate(request.instrutorId())
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Instrutor", request.instrutorId()));
        if (!Boolean.TRUE.equals(instrutor.getAtivo())) {
            throw new RegraNegocioException("O instrutor " + instrutor.getNome() + " está inativo");
        }

        Optional<AulaColetiva> conflitante = aulaRepository
                .findByInstrutorIdAndDiaSemana(instrutor.getId(), request.diaSemana()).stream()
                .filter(a -> a.isAtiva() && a.sobrepoe(request.diaSemana(), request.horario(), request.duracaoMinutos()))
                .findFirst();
        if (conflitante.isPresent()) {
            throw new ConflitoHorarioInstrutorException(instrutor.getNome(), conflitante.get().getId());
        }

        AulaColetiva aula = new AulaColetiva(request.modalidade(), instrutor, request.diaSemana(),
                request.horario(), request.duracaoMinutos(), request.capacidade(), LocalDate.now(clock));
        return AulaResponse.from(aulaRepository.save(aula));
    }

    @Transactional(readOnly = true)
    public List<AulaResponse> listar(Modalidade modalidade) {
        List<AulaColetiva> aulas = modalidade == null
                ? aulaRepository.findAll()
                : aulaRepository.findByModalidade(modalidade);
        return aulas.stream()
                .sorted(Comparator.comparing(AulaColetiva::getDiaSemana)
                        .thenComparing(AulaColetiva::getHorario)
                        .thenComparing(AulaColetiva::getId))
                .map(AulaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AulaResponse buscar(Long id) {
        return aulaRepository.findById(id)
                .map(AulaResponse::from)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aula", id));
    }
}
