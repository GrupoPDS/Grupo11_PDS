package br.ufu.pds.library.infrastructure.persistence;

import br.ufu.pds.library.core.domain.Loan;
import br.ufu.pds.library.core.domain.LoanStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUserId(Long userId);

    List<Loan> findByBookId(Long bookId);

    List<Loan> findByStatus(LoanStatus status);

    List<Loan> findByUserIdAndStatus(Long userId, LoanStatus status);

    long countByBookIdAndStatus(Long bookId, LoanStatus status);

    long countByUserIdAndBookIdAndStatusIn(Long userId, Long bookId, List<LoanStatus> statuses);

    long countByUserIdAndStatus(Long userId, LoanStatus status);

    List<Loan> findByStatusInOrderByDueDateAsc(List<LoanStatus> statuses);

    @Query(
            "SELECT l FROM Loan l JOIN l.user u JOIN l.book b WHERE l.status IN :statuses AND (LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR b.isbn LIKE CONCAT('%', :search, '%')) ORDER BY l.dueDate ASC")
    List<Loan> findLoansByEmailOrIsbnAndStatuses(
            @Param("statuses") List<LoanStatus> statuses, @Param("search") String search);

    // =============================================
    // Histórico paginado do leitor (Backlog 7)
    // =============================================

    Page<Loan> findByUserIdAndStatusInOrderByLoanDateDesc(
            Long userId, List<LoanStatus> statuses, Pageable pageable);

    Page<Loan> findByUserIdOrderByLoanDateDesc(Long userId, Pageable pageable);

    @Query(
            "SELECT COUNT(l) FROM Loan l WHERE l.user.id = :userId AND l.status = 'RETURNED' AND l.returnDate <= l.dueDate")
    long countOnTimeReturns(@Param("userId") Long userId);

    // =============================================
    // Relatório de Atrasos (Backlog 8)
    // =============================================

    @Query(
            "SELECT l FROM Loan l JOIN FETCH l.user JOIN FETCH l.book WHERE l.status = 'OVERDUE' ORDER BY l.dueDate ASC")
    List<Loan> findAllOverdueWithDetails();

    @Query(
            "SELECT l FROM Loan l JOIN FETCH l.user JOIN FETCH l.book WHERE l.status = 'OVERDUE' ORDER BY l.dueDate ASC")
    Page<Loan> findAllOverduePaged(Pageable pageable);

    @Query(
            "SELECT l FROM Loan l JOIN FETCH l.user u JOIN FETCH l.book b WHERE l.status = 'OVERDUE' "
                    + "AND (LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) "
                    + "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) "
                    + "OR LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%')) "
                    + "OR b.isbn LIKE CONCAT('%', :search, '%')) "
                    + "ORDER BY l.dueDate ASC")
    List<Loan> searchOverdueLoans(@Param("search") String search);

    long countByStatus(LoanStatus status);

    @Query(
            value = "SELECT AVG(CURRENT_DATE - l.due_date) FROM loans l WHERE l.status = 'OVERDUE'",
            nativeQuery = true)
    Double averageOverdueDays();

    // =============================================
    // Scheduler de Atrasos
    // =============================================

    /**
     * Busca empréstimos ACTIVE com dueDate estritamente anterior à data informada (para marcação
     * OVERDUE)
     */
    List<Loan> findByStatusAndDueDateBefore(LoanStatus status, LocalDate date);

    /** Busca OVERDUE com dueDate anterior ou igual a maxDueDate (filtro por dias mínimos) */
    @Query(
            "SELECT l FROM Loan l JOIN FETCH l.user JOIN FETCH l.book "
                    + "WHERE l.status = 'OVERDUE' AND l.dueDate <= :maxDueDate ORDER BY l.dueDate ASC")
    List<Loan> findOverdueWithMinDays(@Param("maxDueDate") LocalDate maxDueDate);

    /**
     * Busca empréstimos ativos ou em atraso vinculados a exemplares específicos. Usado para montar
     * o painel de detalhes de exemplares no admin.
     */
    @Query(
            "SELECT l FROM Loan l JOIN FETCH l.user WHERE l.bookCopy.id IN :copyIds AND l.status IN :statuses")
    List<Loan> findActiveLoansByCopyIds(
            @Param("copyIds") List<Long> copyIds, @Param("statuses") List<LoanStatus> statuses);
}
