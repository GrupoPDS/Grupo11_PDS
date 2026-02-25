package br.ufu.pds.library.infrastructure.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class LoanServiceTest {

    @Mock private LoanRepository loanRepository;

    @Mock private UserRepository userRepository;

    @Mock private BookRepository bookRepository;

    @InjectMocks private LoanService loanService;

    private User user;
    private Book book;
    private Loan loan;

    @BeforeEach
    void setup() {
        user = User.builder().id(1L).name("João").email("joao@example.com").role("STUDENT").build();

        book =
                Book.builder()
                        .id(1L)
                        .title("Clean Code")
                        .author("Robert C. Martin")
                        .isbn("978-0132350884")
                        .quantity(2)
                        .build();

        loan =
                Loan.builder()
                        .id(1L)
                        .user(user)
                        .book(book)
                        .loanDate(LocalDate.now())
                        .dueDate(LocalDate.now().plusDays(14))
                        .status(LoanStatus.ACTIVE)
                        .build();
    }

    // =============================================
    // save()
    // =============================================

    @Test
    void save_success_whenBookAvailable() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(loanRepository.countByBookIdAndStatus(1L, LoanStatus.ACTIVE)).thenReturn(0L);
        when(loanRepository.save(any(Loan.class)))
                .thenAnswer(
                        inv -> {
                            Loan l = inv.getArgument(0);
                            l.setId(1L);
                            return l;
                        });

        LocalDate dueDate = LocalDate.now().plusDays(14);
        Loan saved = loanService.save(1L, 1L, dueDate);

        assertNotNull(saved.getId());
        assertEquals(LoanStatus.ACTIVE, saved.getStatus());
        assertEquals(user, saved.getUser());
        assertEquals(book, saved.getBook());
        verify(loanRepository).save(any(Loan.class));
    }

    @Test
    void save_throwsUserNotFound_whenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> loanService.save(99L, 1L, LocalDate.now().plusDays(14)));
    }

    @Test
    void save_throwsBookNotFound_whenBookMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                BookNotFoundException.class,
                () -> loanService.save(1L, 99L, LocalDate.now().plusDays(14)));
    }

    @Test
    void save_throwsBookNotAvailable_whenNoStockLeft() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        // book.quantity = 2, activeLoans = 2 → indisponível
        when(loanRepository.countByBookIdAndStatus(1L, LoanStatus.ACTIVE)).thenReturn(2L);

        assertThrows(
                BookNotAvailableException.class,
                () -> loanService.save(1L, 1L, LocalDate.now().plusDays(14)));
    }

    // =============================================
    // findById()
    // =============================================

    @Test
    void findById_success_whenExists() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        Loan found = loanService.findById(1L);

        assertEquals(1L, found.getId());
        assertEquals(LoanStatus.ACTIVE, found.getStatus());
    }

    @Test
    void findById_throwsNotFound_whenMissing() {
        when(loanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(LoanNotFoundException.class, () -> loanService.findById(99L));
    }

    // =============================================
    // findAll()
    // =============================================

    @Test
    void findAll_returnsList() {
        when(loanRepository.findAll()).thenReturn(List.of(loan));

        List<Loan> loans = loanService.findAll();

        assertEquals(1, loans.size());
        assertEquals(1L, loans.get(0).getId());
    }

    // =============================================
    // findByUserId()
    // =============================================

    @Test
    void findByUserId_success_whenUserExists() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(loanRepository.findByUserId(1L)).thenReturn(List.of(loan));

        List<Loan> loans = loanService.findByUserId(1L);

        assertEquals(1, loans.size());
    }

    @Test
    void findByUserId_throwsNotFound_whenUserMissing() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> loanService.findByUserId(99L));
    }

    // =============================================
    // returnLoan()
    // =============================================

    @Test
    void returnLoan_success_whenActive() {
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        Loan returned = loanService.returnLoan(1L);

        assertEquals(LoanStatus.RETURNED, returned.getStatus());
        assertEquals(LocalDate.now(), returned.getReturnDate());
    }

    @Test
    void returnLoan_success_whenOverdue() {
        loan.setStatus(LoanStatus.OVERDUE);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        Loan returned = loanService.returnLoan(1L);

        assertEquals(LoanStatus.RETURNED, returned.getStatus());
        assertNotNull(returned.getReturnDate());
    }

    @Test
    void returnLoan_throwsInvalidStatus_whenAlreadyReturned() {
        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnDate(LocalDate.now().minusDays(1));
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        assertThrows(InvalidLoanStatusException.class, () -> loanService.returnLoan(1L));
    }

    @Test
    void returnLoan_throwsNotFound_whenMissing() {
        when(loanRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(LoanNotFoundException.class, () -> loanService.returnLoan(99L));
    }

    // =============================================
    // delete()
    // =============================================

    @Test
    void delete_success_whenExists() {
        when(loanRepository.existsById(1L)).thenReturn(true);

        loanService.delete(1L);

        verify(loanRepository).deleteById(1L);
    }

    @Test
    void delete_throwsNotFound_whenMissing() {
        when(loanRepository.existsById(99L)).thenReturn(false);

        assertThrows(LoanNotFoundException.class, () -> loanService.delete(99L));
    }
}
