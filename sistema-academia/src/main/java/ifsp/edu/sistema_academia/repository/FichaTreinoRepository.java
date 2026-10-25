package ifsp.edu.sistema_academia.repository;

import ifsp.edu.sistema_academia.model.FichaTreino;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FichaTreinoRepository extends JpaRepository<FichaTreino, Long> {

    Optional<FichaTreino> findFirstByAlunoIdAndIsAtualTrueOrderByCriadaEmDescIdDesc(Long alunoId);

    List<FichaTreino> findAllByAlunoIdAndIsAtualTrue(Long alunoId);

    List<FichaTreino> findByAlunoIdAndIsAtualFalseOrderByCriadaEmDescIdDesc(Long alunoId);
}