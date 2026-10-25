package ifsp.edu.sistema_academia.repository;

import ifsp.edu.sistema_academia.model.Mensalidade;
import ifsp.edu.sistema_academia.model.StatusPagamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MensalidadeRepository extends JpaRepository<Mensalidade, Long> {

    boolean existsByMatriculaAlunoIdAndStatusPagamentoAndVencimentoBefore(
            Long alunoId, StatusPagamento status, LocalDate limite);

    List<Mensalidade> findByMatriculaAlunoIdOrderByVencimentoAscIdAsc(Long alunoId);

    List<Mensalidade> findByMatriculaAlunoIdAndStatusPagamentoOrderByVencimentoAscIdAsc(
            Long alunoId, StatusPagamento status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Mensalidade m where m.id = :id")
    Optional<Mensalidade> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select m from Mensalidade m
            join fetch m.matricula mat
            join fetch mat.aluno
            where m.statusPagamento = :status and m.vencimento < :hoje
            order by m.vencimento asc
            """)
    List<Mensalidade> findVencidas(@Param("status") StatusPagamento status, @Param("hoje") LocalDate hoje);
}