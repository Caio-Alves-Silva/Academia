package ifsp.edu.sistema_academia.repository;

import ifsp.edu.sistema_academia.model.Plano;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanoRepository extends JpaRepository<Plano, Long> {

    List<Plano> findAllByOrderByDuracaoMesesAscNomeAsc();
}