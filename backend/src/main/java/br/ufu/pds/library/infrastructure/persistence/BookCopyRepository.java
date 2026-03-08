package br.ufu.pds.library.infrastructure.persistence;

import br.ufu.pds.library.core.domain.BookCopy;
import br.ufu.pds.library.core.domain.LoanStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookCopyRepository extends JpaRepository<BookCopy, Long> {

    List<BookCopy> findByBookId(Long bookId);

    List<BookCopy> findByBookIdIn(List<Long> bookIds);

    long countByBookId(Long bookId);

    Optional<BookCopy> findByCopyCode(String copyCode);

    boolean existsByCopyCode(String copyCode);

    /**
     * Retorna exemplares disponíveis (não emprestados) de um livro. Exclui exemplares que possuem
     * empréstimos ACTIVE ou OVERDUE.
     */
    @Query(
            "SELECT bc FROM BookCopy bc WHERE bc.book.id = :bookId "
                    + "AND bc.id NOT IN ("
                    + "  SELECT l.bookCopy.id FROM Loan l "
                    + "  WHERE l.bookCopy IS NOT NULL AND l.status IN :statuses"
                    + ") ORDER BY bc.id ASC")
    List<BookCopy> findAvailableCopies(
            @Param("bookId") Long bookId, @Param("statuses") List<LoanStatus> statuses);
}
