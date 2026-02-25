package br.ufu.pds.library.infrastructure.service;

import br.ufu.pds.library.core.domain.*;
import br.ufu.pds.library.core.exceptions.BookNotAvailableException;
import br.ufu.pds.library.core.exceptions.BookNotFoundException;
import br.ufu.pds.library.core.exceptions.InvalidLoanStatusException;
import br.ufu.pds.library.core.exceptions.LoanNotFoundException;
import br.ufu.pds.library.core.exceptions.UserNotFoundException;
import br.ufu.pds.library.infrastructure.persistence.BookRepository;
import br.ufu.pds.library.infrastructure.persistence.LoanRepository;
import br.ufu.pds.library.infrastructure.persistence.UserRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;

    @Transactional
    public Loan save(Long userId, Long bookId, LocalDate dueDate) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserNotFoundException(userId));

        Book book =
                bookRepository
                        .findById(bookId)
                        .orElseThrow(() -> new BookNotFoundException(bookId));

        // Verifica disponibilidade: quantidade do livro > empréstimos ativos desse livro
        long activeLoans = loanRepository.countByBookIdAndStatus(bookId, LoanStatus.ACTIVE);
        if (activeLoans >= book.getQuantity()) {
            throw new BookNotAvailableException(bookId);
        }

        Loan loan =
                Loan.builder()
                        .user(user)
                        .book(book)
                        .loanDate(LocalDate.now())
                        .dueDate(dueDate)
                        .status(LoanStatus.ACTIVE)
                        .build();

        return loanRepository.save(loan);
    }

    @Transactional(readOnly = true)
    public List<Loan> findAll() {
        return loanRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Loan findById(Long id) {
        return loanRepository.findById(id).orElseThrow(() -> new LoanNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<Loan> findByUserId(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        return loanRepository.findByUserId(userId);
    }

    @Transactional
    public Loan returnLoan(Long id) {
        Loan loan = loanRepository.findById(id).orElseThrow(() -> new LoanNotFoundException(id));

        if (!loan.isActive() && loan.getStatus() != LoanStatus.OVERDUE) {
            throw new InvalidLoanStatusException("Empréstimo com ID " + id + " já foi devolvido");
        }

        loan.setReturnDate(LocalDate.now());
        loan.setStatus(LoanStatus.RETURNED);

        return loanRepository.save(loan);
    }

    @Transactional
    public void delete(Long id) {
        if (!loanRepository.existsById(id)) {
            throw new LoanNotFoundException(id);
        }
        loanRepository.deleteById(id);
    }
}
