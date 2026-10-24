package ifsp.edu.sistema_academia.repository;

import ifsp.edu.sistema_academia.dto.response.FrequenciaAlunoResponse;
import ifsp.edu.sistema_academia.model.Checkin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CheckinRepository extends JpaRepository<Checkin, Long> {

    List<Checkin> findByAlunoIdOrderByDataHoraDesc(Long alunoId);

    /** Entradas por aluno no intervalo [inicio, fim), incluindo alunos sem nenhuma entrada. */
    @Query("""
            select new ifsp.edu.sistema_academia.dto.response.FrequenciaAlunoResponse(u.id, u.nome, count(c))
            from Usuario u
            left join Checkin c on c.aluno.id = u.id and c.dataHora >= :inicio and c.dataHora < :fim
            where u.perfil = ifsp.edu.sistema_academia.model.Perfil.ALUNO
            group by u.id, u.nome
            order by count(c) desc, u.nome asc
            """)
    List<FrequenciaAlunoResponse> frequenciaPorAluno(@Param("inicio") LocalDateTime inicio,
                                                      @Param("fim") LocalDateTime fim);
}