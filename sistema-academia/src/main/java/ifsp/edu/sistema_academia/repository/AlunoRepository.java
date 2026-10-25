package ifsp.edu.sistema_academia.repository;

import ifsp.edu.sistema_academia.model.Aluno;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {

    List<Aluno> findAllByOrderByNomeAsc();

    List<Aluno> findByNomeContainingIgnoreCaseOrderByNomeAsc(String nome);

    /** Bloqueia a linha do aluno até o fim da transação, serializando operações concorrentes sobre o mesmo aluno. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Aluno a where a.id = :id")
    Optional<Aluno> findByIdForUpdate(@Param("id") Long id);
}