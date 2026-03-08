package br.ufu.pds.library.infrastructure.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.ufu.pds.library.core.domain.*;
import br.ufu.pds.library.infrastructure.persistence.BookCopyRepository;
import br.ufu.pds.library.infrastructure.persistence.BookRepository;
import br.ufu.pds.library.infrastructure.persistence.LoanRepository;
import br.ufu.pds.library.infrastructure.persistence.ReservationRepository;
import br.ufu.pds.library.infrastructure.persistence.UserRepository;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanServiceSearchTest {

    @Mock private LoanRepository loanRepository;
    @Mock private UserRepository userRepository;
    @Mock private BookRepository bookRepository;
    @Mock private BookCopyRepository bookCopyRepository;
    @Mock private ReservationRepository reservationRepository;

    @InjectMocks private LoanService loanService;

    private User user1;
    private User user2;
    private Book book1;
    private Book book2;
    private Loan loanActive;
    private Loan loanOverdue;
    private Loan loanReturned;

    @BeforeEach
    void setUp() {
        user1 = User.builder().id(1L).name("Maria").email("maria@ufu.br").role("STUDENT").build();
        user2 = User.builder().id(2L).name("João").email("joao@ufu.br").role("STUDENT").build();

        book1 =
                Book.builder()
                        .id(1L)
                        .title("Clean Code")
                        .author("Uncle Bob")
                        .isbn("978-0132350884")
                        .quantity(2)
                        .build();
        book2 =
                Book.builder()
                        .id(2L)
                        .title("Domain-Driven Design")
                        .author("Eric Evans")
                        .isbn("978-0321125217")
                        .quantity(1)
                        .build();

        loanActive =
                Loan.builder()
                        .id(1L)
                        .user(user1)
                        .book(book1)
                        .loanDate(LocalDate.now().minusDays(5))
                        .dueDate(LocalDate.now().plusDays(9))
                        .status(LoanStatus.ACTIVE)
                        .build();

        loanOverdue =
                Loan.builder()
                        .id(2L)
                        .user(user2)
                        .book(book2)
                        .loanDate(LocalDate.now().minusDays(20))
                        .dueDate(LocalDate.now().minusDays(6))
                        .status(LoanStatus.OVERDUE)
                        .build();

        loanReturned =
                Loan.builder()
                        .id(3L)
                        .user(user1)
                        .book(book2)
                        .loanDate(LocalDate.now().minusDays(30))
                        .dueDate(LocalDate.now().minusDays(16))
                        .returnDate(LocalDate.now().minusDays(18))
                        .status(LoanStatus.RETURNED)
                        .build();
    }

    // =============================================
    // searchActiveLoans — busca por email parcial
    // =============================================

    @Test
    void searchActiveLoans_byPartialEmail_returnsMatchingLoans() {
        List<LoanStatus> expectedStatuses = Arrays.asList(LoanStatus.ACTIVE, LoanStatus.OVERDUE);
        when(loanRepository.findLoansByEmailOrIsbnAndStatuses(expectedStatuses, "maria"))
                .thenReturn(List.of(loanActive));

        List<Loan> result = loanService.searchActiveLoans("maria", null);

        assertEquals(1, result.size());
        assertEquals("maria@ufu.br", result.get(0).getUser().getEmail());
        assertEquals(LoanStatus.ACTIVE, result.get(0).getStatus());
        verify(loanRepository).findLoansByEmailOrIsbnAndStatuses(expectedStatuses, "maria");
        verify(loanRepository, never()).findAll();
    }

    // =============================================
    // searchActiveLoans — busca por ISBN completo
    // =============================================

    @Test
    void searchActiveLoans_byFullIsbn_returnsSingleLoan() {
        List<LoanStatus> expectedStatuses = Arrays.asList(LoanStatus.ACTIVE, LoanStatus.OVERDUE);
        when(loanRepository.findLoansByEmailOrIsbnAndStatuses(expectedStatuses, "978-0321125217"))
                .thenReturn(List.of(loanOverdue));

        List<Loan> result = loanService.searchActiveLoans("978-0321125217", null);

        assertEquals(1, result.size());
        assertEquals("978-0321125217", result.get(0).getBook().getIsbn());
        verify(loanRepository)
                .findLoansByEmailOrIsbnAndStatuses(expectedStatuses, "978-0321125217");
    }

    // =============================================
    // searchActiveLoans — busca vazia retorna todos ativos
    // =============================================

    @Test
    void searchActiveLoans_emptyQuery_returnsAllActiveAndOverdue() {
        when(loanRepository.findByStatusInOrderByDueDateAsc(anyList()))
                .thenReturn(List.of(loanActive, loanOverdue));

        List<Loan> result = loanService.searchActiveLoans("", null);

        assertEquals(2, result.size());
        assertTrue(
                result.stream()
                        .allMatch(
                                l ->
                                        l.getStatus() == LoanStatus.ACTIVE
                                                || l.getStatus() == LoanStatus.OVERDUE));
        verify(loanRepository).findByStatusInOrderByDueDateAsc(anyList());
        verify(loanRepository, never()).findLoansByEmailOrIsbnAndStatuses(any(), any());
    }

    @Test
    void searchActiveLoans_nullQuery_returnsAllActiveAndOverdue() {
        when(loanRepository.findByStatusInOrderByDueDateAsc(anyList()))
                .thenReturn(List.of(loanActive, loanOverdue));

        List<Loan> result = loanService.searchActiveLoans(null, null);

        assertEquals(2, result.size());
        verify(loanRepository).findByStatusInOrderByDueDateAsc(anyList());
    }

    // =============================================
    // searchActiveLoans — filtro por status
    // =============================================

    @Test
    void searchActiveLoans_withStatusFilter_filtersCorrectly() {
        when(loanRepository.findAllOverdueWithDetails()).thenReturn(List.of(loanOverdue));

        List<Loan> result = loanService.searchActiveLoans(null, "OVERDUE");

        assertEquals(1, result.size());
        assertEquals(LoanStatus.OVERDUE, result.get(0).getStatus());
    }

    @Test
    void searchActiveLoans_withInvalidStatus_fallsBackToDefault() {
        when(loanRepository.findByStatusInOrderByDueDateAsc(anyList()))
                .thenReturn(List.of(loanActive, loanOverdue));

        List<Loan> result = loanService.searchActiveLoans(null, "INVALID_STATUS");

        // Status inválido → volta ao default ACTIVE + OVERDUE
        assertEquals(2, result.size());
    }
}
