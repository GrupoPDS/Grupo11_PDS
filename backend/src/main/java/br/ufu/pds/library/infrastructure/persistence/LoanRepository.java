package br.ufu.pds.library.infrastructure.persistence;

import br.ufu.pds.library.core.domain.Loan;
import br.ufu.pds.library.core.domain.LoanStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByUserId(Long userId);

    List<Loan> findByBookId(Long bookId);

    List<Loan> findByStatus(LoanStatus status);

    List<Loan> findByUserIdAndStatus(Long userId, LoanStatus status);

    long countByBookIdAndStatus(Long bookId, LoanStatus status);
}
