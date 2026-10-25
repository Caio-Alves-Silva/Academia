package ifsp.edu.sistema_academia.repository;

import ifsp.edu.sistema_academia.model.Matricula;
import ifsp.edu.sistema_academia.model.StatusMatricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    Optional<Matricula> findFirstByAlunoIdAndStatusMatriculaAndDataFimGreaterThanEqual(
            Long alunoId, StatusMatricula status, LocalDate hoje);

    /** Matrícula ativa: status ATIVA e vigência ainda não encerrada em {@code hoje}. */
    default Optional<Matricula> findAtiva(Long alunoId, LocalDate hoje) {
        return findFirstByAlunoIdAndStatusMatriculaAndDataFimGreaterThanEqual(alunoId, StatusMatricula.ATIVA, hoje);
    }

    List<Matricula> findByAlunoIdOrderByDataInicioDescIdDesc(Long alunoId);

    @Query("select m.aluno.id from Matricula m where m.id = :id")
    Optional<Long> findAlunoIdById(@Param("id") Long id);
}