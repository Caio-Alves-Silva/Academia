package ifsp.edu.sistema_academia.repository;

import ifsp.edu.sistema_academia.dto.response.FrequenciaAlunoResponse;
import ifsp.edu.sistema_academia.model.Checkin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CheckinRepository extends JpaRepository<Checkin, Long> {

    List<Checkin> findByAlunoIdOrderByDataHoraEntradaDesc(Long alunoId);

    /** Entradas liberadas por aluno no intervalo [inicio, fim), incluindo alunos sem nenhuma entrada. */
    @Query("""
            select new ifsp.edu.sistema_academia.dto.response.FrequenciaAlunoResponse(a.id, a.nome, count(c))
            from Aluno a
            left join Checkin c on c.aluno.id = a.id and c.acessoLiberado = true
                and c.dataHoraEntrada >= :inicio and c.dataHoraEntrada < :fim
            group by a.id, a.nome
            order by count(c) desc, a.nome asc
            """)
    List<FrequenciaAlunoResponse> frequenciaPorAluno(@Param("inicio") LocalDateTime inicio,
                                                      @Param("fim") LocalDateTime fim);
}