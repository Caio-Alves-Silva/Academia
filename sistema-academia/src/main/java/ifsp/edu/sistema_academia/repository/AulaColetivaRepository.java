package ifsp.edu.sistema_academia.repository;

import ifsp.edu.sistema_academia.model.AulaColetiva;
import ifsp.edu.sistema_academia.model.Modalidade;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface AulaColetivaRepository extends JpaRepository<AulaColetiva, Long> {

    List<AulaColetiva> findByInstrutorIdAndDiaSemana(Long instrutorId, DayOfWeek diaSemana);

    List<AulaColetiva> findByModalidade(Modalidade modalidade);

    /** Bloqueia a aula até o fim da transação: serializa inscrições e cancelamentos (vagas e lista de espera). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AulaColetiva a where a.id = :id")
    Optional<AulaColetiva> findByIdForUpdate(@Param("id") Long id);
}