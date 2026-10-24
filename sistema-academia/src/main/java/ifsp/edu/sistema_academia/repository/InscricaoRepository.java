package ifsp.edu.sistema_academia.repository;

import ifsp.edu.sistema_academia.model.Inscricao;
import ifsp.edu.sistema_academia.model.Presenca;
import ifsp.edu.sistema_academia.model.StatusInscricao;
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

    long countByAulaIdAndDataAulaAndStatus(Long aulaId, LocalDate dataAula, StatusInscricao status);

    long countByAulaIdAndDataAulaAndStatusAndIdLessThanEqual(
            Long aulaId, LocalDate dataAula, StatusInscricao status, Long id);

    List<Inscricao> findByAulaIdAndDataAulaAndStatusOrderByIdAsc(
            Long aulaId, LocalDate dataAula, StatusInscricao status);

    List<Inscricao> findByAulaIdAndDataAulaAndStatusInOrderByIdAsc(
            Long aulaId, LocalDate dataAula, Collection<StatusInscricao> status);

    boolean existsByAulaIdAndAlunoIdAndDataAulaAndStatusIn(
            Long aulaId, Long alunoId, LocalDate dataAula, Collection<StatusInscricao> status);

    long countByAlunoIdAndDataAulaBetweenAndStatusIn(
            Long alunoId, LocalDate inicio, LocalDate fim, Collection<StatusInscricao> status);

    long countByAlunoIdAndDataAulaBetweenAndPresenca(
            Long alunoId, LocalDate inicio, LocalDate fim, Presenca presenca);

    List<Inscricao> findByAlunoIdOrderByDataAulaDescIdDesc(Long alunoId);

    @Query("select i.aula.id from Inscricao i where i.id = :id")
    Optional<Long> findAulaIdById(@Param("id") Long id);

    @Query("""
            select i.aula.id as aulaId, i.dataAula as dataAula, count(i) as total
            from Inscricao i
            where i.status = :status and i.dataAula between :inicio and :fim
            group by i.aula.id, i.dataAula
            """)
    List<OcupacaoSessao> contarPorSessao(@Param("status") StatusInscricao status,
                                         @Param("inicio") LocalDate inicio,
                                         @Param("fim") LocalDate fim);
}