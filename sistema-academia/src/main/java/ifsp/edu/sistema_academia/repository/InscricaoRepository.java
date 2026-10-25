package ifsp.edu.sistema_academia.repository;

import ifsp.edu.sistema_academia.model.Inscricao;
import ifsp.edu.sistema_academia.model.StatusInscricao;
import ifsp.edu.sistema_academia.model.StatusPresenca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {

    /** Total de inscrições confirmadas por ocorrência (aula + data), usado no relatório de ocupação. */
    interface OcupacaoSessao {
        Long getAulaId();

        LocalDate getDataAula();

        Long getTotal();
    }

    long countByAulaColetivaIdAndDataAulaAndStatus(Long aulaId, LocalDate dataAula, StatusInscricao status);

    /** Fila FIFO: ordenada por {@code criadaEm} (o id desempata inscrições criadas no mesmo instante). */
    List<Inscricao> findByAulaColetivaIdAndDataAulaAndStatusOrderByCriadaEmAscIdAsc(
            Long aulaId, LocalDate dataAula, StatusInscricao status);

    List<Inscricao> findByAulaColetivaIdAndDataAulaAndStatusInOrderByCriadaEmAscIdAsc(
            Long aulaId, LocalDate dataAula, Collection<StatusInscricao> status);

    boolean existsByAulaColetivaIdAndAlunoIdAndDataAulaAndStatusIn(
            Long aulaId, Long alunoId, LocalDate dataAula, Collection<StatusInscricao> status);

    long countByAlunoIdAndDataAulaBetweenAndStatusIn(
            Long alunoId, LocalDate inicio, LocalDate fim, Collection<StatusInscricao> status);

    long countByAlunoIdAndDataAulaBetweenAndPresenca(
            Long alunoId, LocalDate inicio, LocalDate fim, StatusPresenca presenca);

    List<Inscricao> findByAlunoIdOrderByDataAulaDescIdDesc(Long alunoId);

    @Query("select i.aulaColetiva.id from Inscricao i where i.id = :id")
    Optional<Long> findAulaIdById(@Param("id") Long id);

    @Query("""
            select i.aulaColetiva.id as aulaId, i.dataAula as dataAula, count(i) as total
            from Inscricao i
            where i.status = :status and i.dataAula between :inicio and :fim
            group by i.aulaColetiva.id, i.dataAula
            """)
    List<OcupacaoSessao> contarPorSessao(@Param("status") StatusInscricao status,
                                         @Param("inicio") LocalDate inicio,
                                         @Param("fim") LocalDate fim);
}