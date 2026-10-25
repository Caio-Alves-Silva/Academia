package ifsp.edu.sistema_academia.service;

import ifsp.edu.sistema_academia.dto.request.PresencaRequest;
import ifsp.edu.sistema_academia.dto.response.InscricaoResponse;
import ifsp.edu.sistema_academia.dto.response.PresencaResponse;
import ifsp.edu.sistema_academia.exception.AcessoNegadoException;
import ifsp.edu.sistema_academia.exception.DataAulaInvalidaException;
import ifsp.edu.sistema_academia.exception.PresencaInvalidaException;
import ifsp.edu.sistema_academia.exception.RecursoNaoEncontradoException;
import ifsp.edu.sistema_academia.model.AulaColetiva;
import ifsp.edu.sistema_academia.model.Inscricao;
import ifsp.edu.sistema_academia.model.StatusPresenca;
import ifsp.edu.sistema_academia.model.StatusInscricao;
import ifsp.edu.sistema_academia.repository.AulaColetivaRepository;
import ifsp.edu.sistema_academia.repository.InscricaoRepository;
import ifsp.edu.sistema_academia.security.UsuarioAutenticado;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RF11: o instrutor da aula registra a lista de presença. Os alunos listados recebem presença; os demais inscritos
 * confirmados da ocorrência recebem falta. Registrar novamente corrige o lançamento anterior.
 */
@Service
public class PresencaService {

    private final AulaColetivaRepository aulaRepository;
    private final InscricaoRepository inscricaoRepository;
    private final Clock clock;

    public PresencaService(AulaColetivaRepository aulaRepository, InscricaoRepository inscricaoRepository,
                           Clock clock) {
        this.aulaRepository = aulaRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.clock = clock;
    }

    @Transactional
    public PresencaResponse registrar(Long aulaId, PresencaRequest request, UsuarioAutenticado instrutor) {
        LocalDateTime agora = LocalDateTime.now(clock);
        LocalDate data = request.data();

        AulaColetiva aula = aulaRepository.findById(aulaId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Aula", aulaId));
        if (!aula.getInstrutor().getId().equals(instrutor.id())) {
            throw new AcessoNegadoException("Somente o instrutor responsável pela aula registra a presença");
        }
        if (data.getDayOfWeek() != aula.getDiaSemana()) {
            throw new DataAulaInvalidaException("A aula acontece às " + aula.getDiaSemana()
                    + ", mas a data informada é " + data.getDayOfWeek());
        }
        if (aula.inicioEm(data).isAfter(agora)) {
            throw new DataAulaInvalidaException("A aula de " + data + " ainda não começou");
        }

        List<Inscricao> confirmadas = inscricaoRepository.findByAulaColetivaIdAndDataAulaAndStatusOrderByCriadaEmAscIdAsc(
                aulaId, data, StatusInscricao.CONFIRMADA);
        Set<Long> presentes = new HashSet<>(request.alunosPresentes());
        Set<Long> idsConfirmados = confirmadas.stream().map(i -> i.getAluno().getId()).collect(Collectors.toSet());

        Set<Long> desconhecidos = new HashSet<>(presentes);
        desconhecidos.removeAll(idsConfirmados);
        if (!desconhecidos.isEmpty()) {
            throw new PresencaInvalidaException(
                    "Alunos sem inscrição confirmada nesta aula/data: " + desconhecidos.stream().sorted().toList());
        }

        confirmadas.forEach(i -> i.registrarPresenca(presentes.contains(i.getAluno().getId())));

        int totalPresentes = (int) confirmadas.stream().filter(i -> i.getPresenca() == StatusPresenca.PRESENTE).count();
        List<InscricaoResponse> inscricoes = confirmadas.stream()
                .map(i -> InscricaoResponse.from(i, null))
                .toList();
        return new PresencaResponse(aulaId, data, totalPresentes, confirmadas.size() - totalPresentes, inscricoes);
    }
}
