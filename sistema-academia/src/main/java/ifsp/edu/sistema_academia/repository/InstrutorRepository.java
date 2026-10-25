package ifsp.edu.sistema_academia.repository;

import ifsp.edu.sistema_academia.model.Instrutor;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InstrutorRepository extends JpaRepository<Instrutor, Long> {

    /** Bloqueia o instrutor até o fim da transação: cadastros simultâneos de aulas não furam o RF08. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Instrutor i where i.id = :id")
    Optional<Instrutor> findByIdForUpdate(@Param("id") Long id);
}