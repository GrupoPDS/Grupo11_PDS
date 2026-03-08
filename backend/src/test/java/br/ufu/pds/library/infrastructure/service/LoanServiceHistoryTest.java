package br.ufu.pds.library.infrastructure.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.ufu.pds.library.core.domain.*;
import br.ufu.pds.library.core.exceptions.UserNotFoundException;
import br.ufu.pds.library.entrypoint.api.dto.LoanHistoryResponse;
import br.ufu.pds.library.entrypoint.api.dto.LoanHistorySummaryResponse;
import br.ufu.pds.library.entrypoint.api.dto.PagedResponse;
import br.ufu.pds.library.infrastructure.persistence.BookCopyRepository;
import br.ufu.pds.library.infrastructure.persistence.BookRepository;
import br.ufu.pds.library.infrastructure.persistence.LoanRepository;
import br.ufu.pds.library.infrastructure.persistence.ReservationRepository;
import br.ufu.pds.library.infrastructure.persistence.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
public class LoanServiceHistoryTest {

    @Mock private LoanRepository loanRepository;
    @Mock private UserRepository userRepository;
    @Mock private BookRepository bookRepository;
    @Mock private BookCopyRepository bookCopyRepository;
    @Mock private ReservationRepository reservationRepository;

    @InjectMocks private LoanService loanService;

    private User user;
    private Book book;
    private Loan activeLoan;
    private Loan returnedLoanOnTime;
    private Loan returnedLoanLate;
    private Loan overdueLoan;

    @BeforeEach
    void setup() {
        user = User.builder().id(1L).name("Maria").email("maria@ufu.br").role("STUDENT").build();

        book =
                Book.builder()
                        .id(1L)
                        .title("Refactoring")
                        .author("Martin Fowler")
                        .isbn("978-0134757599")
                        .quantity(3)
                        .build();

        activeLoan =
                Loan.builder()
                        .id(1L)
                        .user(user)
                        .book(book)
                        .loanDate(LocalDate.now().minusDays(5))
                        .dueDate(LocalDate.now().plusDays(9))
                        .status(LoanStatus.ACTIVE)
                        .build();

        returnedLoanOnTime =
                Loan.builder()
                        .id(2L)
                        .user(user)
                        .book(book)
                        .loanDate(LocalDate.now().minusDays(20))
                        .dueDate(LocalDate.now().minusDays(6))
                        .returnDate(LocalDate.now().minusDays(8))
                        .status(LoanStatus.RETURNED)
                        .build();

        returnedLoanLate =
                Loan.builder()
                        .id(3L)
                        .user(user)
                        .book(book)
                        .loanDate(LocalDate.now().minusDays(30))
                        .dueDate(LocalDate.now().minusDays(20))
                        .returnDate(LocalDate.now().minusDays(15))
                        .status(LoanStatus.RETURNED)
                        .build();

        overdueLoan =
                Loan.builder()
                        .id(4L)
                        .user(user)
                        .book(book)
                        .loanDate(LocalDate.now().minusDays(25))
                        .dueDate(LocalDate.now().minusDays(11))
                        .status(LoanStatus.OVERDUE)
                        .build();
    }

    // ── getLoanHistory ──

    @Test
    void getLoanHistory_success_returnsPaginatedAndOrdered() {
        when(userRepository.existsById(1L)).thenReturn(true);

        List<Loan> loans = List.of(activeLoan, returnedLoanOnTime, returnedLoanLate);
        Pageable pageable = PageRequest.of(0, 6);
        Page<Loan> page = new PageImpl<>(loans, pageable, loans.size());

        when(loanRepository.findByUserIdOrderByLoanDateDesc(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<LoanHistoryResponse> result = loanService.getLoanHistory(1L, null, 0, 6);

        assertEquals(3, result.getContent().size());
        assertEquals(0, result.getPage());
        assertEquals(6, result.getSize());
        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertFalse(result.isHasNext());
        verify(loanRepository).findByUserIdOrderByLoanDateDesc(eq(1L), any(Pageable.class));
    }

    @Test
    void getLoanHistory_filterByStatus_onlyReturnedItems() {
        when(userRepository.existsById(1L)).thenReturn(true);

        List<Loan> loans = List.of(returnedLoanOnTime, returnedLoanLate);
        Pageable pageable = PageRequest.of(0, 6);
        Page<Loan> page = new PageImpl<>(loans, pageable, loans.size());

        when(loanRepository.findByUserIdAndStatusInOrderByLoanDateDesc(
                        eq(1L), anyList(), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<LoanHistoryResponse> result =
                loanService.getLoanHistory(1L, List.of("RETURNED"), 0, 6);

        assertEquals(2, result.getContent().size());
        result.getContent().forEach(item -> assertEquals("RETURNED", item.getStatus()));
    }

    @Test
    void getLoanHistory_emptyResult_returnsEmptyPage() {
        when(userRepository.existsById(1L)).thenReturn(true);

        Pageable pageable = PageRequest.of(0, 6);
        Page<Loan> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(loanRepository.findByUserIdOrderByLoanDateDesc(eq(1L), any(Pageable.class)))
                .thenReturn(emptyPage);

        PagedResponse<LoanHistoryResponse> result = loanService.getLoanHistory(1L, null, 0, 6);

        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getTotalPages());
    }

    @Test
    void getLoanHistory_userNotFound_throwsException() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThrows(
                UserNotFoundException.class, () -> loanService.getLoanHistory(999L, null, 0, 6));
        verify(loanRepository, never()).findByUserIdOrderByLoanDateDesc(anyLong(), any());
    }

    @Test
    void getLoanHistory_paginationMetadata_isCorrect() {
        when(userRepository.existsById(1L)).thenReturn(true);

        // Simula página 1 de 3 (size=2, total=5)
        List<Loan> loans = List.of(activeLoan, returnedLoanOnTime);
        Pageable pageable = PageRequest.of(1, 2);
        Page<Loan> page = new PageImpl<>(loans, pageable, 5);

        when(loanRepository.findByUserIdOrderByLoanDateDesc(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<LoanHistoryResponse> result = loanService.getLoanHistory(1L, null, 1, 2);

        assertEquals(2, result.getContent().size());
        assertEquals(1, result.getPage());
        assertEquals(2, result.getSize());
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertTrue(result.isHasNext());
    }

    // ── getLoanSummary ──

    @Test
    void getLoanSummary_success_calculatesCorrectly() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(loanRepository.countByUserIdAndStatus(1L, LoanStatus.ACTIVE)).thenReturn(1L);
        when(loanRepository.countByUserIdAndStatus(1L, LoanStatus.RETURNED)).thenReturn(4L);
        when(loanRepository.countByUserIdAndStatus(1L, LoanStatus.OVERDUE)).thenReturn(1L);
        when(loanRepository.countOnTimeReturns(1L)).thenReturn(3L);

        LoanHistorySummaryResponse result = loanService.getLoanSummary(1L);

        assertEquals(6, result.getTotalLoans());
        assertEquals(1, result.getActiveLoans());
        assertEquals(4, result.getReturnedLoans());
        assertEquals(1, result.getOverdueLoans());
        assertEquals(0.75, result.getOnTimeReturnRate(), 0.001);
    }

    @Test
    void getLoanSummary_noReturnedLoans_rateIsZero() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(loanRepository.countByUserIdAndStatus(1L, LoanStatus.ACTIVE)).thenReturn(2L);
        when(loanRepository.countByUserIdAndStatus(1L, LoanStatus.RETURNED)).thenReturn(0L);
        when(loanRepository.countByUserIdAndStatus(1L, LoanStatus.OVERDUE)).thenReturn(0L);

        LoanHistorySummaryResponse result = loanService.getLoanSummary(1L);

        assertEquals(2, result.getTotalLoans());
        assertEquals(0, result.getReturnedLoans());
        assertEquals(0.0, result.getOnTimeReturnRate(), 0.001);
    }

    @Test
    void getLoanSummary_userNotFound_throwsException() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> loanService.getLoanSummary(999L));
        verify(loanRepository, never()).countByUserIdAndStatus(anyLong(), any());
    }

    @Test
    void getLoanSummary_allReturnedOnTime_rateIsOne() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(loanRepository.countByUserIdAndStatus(1L, LoanStatus.ACTIVE)).thenReturn(0L);
        when(loanRepository.countByUserIdAndStatus(1L, LoanStatus.RETURNED)).thenReturn(5L);
        when(loanRepository.countByUserIdAndStatus(1L, LoanStatus.OVERDUE)).thenReturn(0L);
        when(loanRepository.countOnTimeReturns(1L)).thenReturn(5L);

        LoanHistorySummaryResponse result = loanService.getLoanSummary(1L);

        assertEquals(5, result.getTotalLoans());
        assertEquals(1.0, result.getOnTimeReturnRate(), 0.001);
    }

    // ── LoanHistoryResponse.from (unit) ──

    @Test
    void loanHistoryResponse_from_activeHasNullReturnedOnTime() {
        LoanHistoryResponse response = LoanHistoryResponse.from(activeLoan);

        assertEquals(1L, response.getId());
        assertEquals("Refactoring", response.getBookTitle());
        assertEquals("Martin Fowler", response.getBookAuthor());
        assertEquals("978-0134757599", response.getBookIsbn());
        assertEquals("ACTIVE", response.getStatus());
        assertNull(response.getReturnDate());
        assertNull(response.getReturnedOnTime());
        assertTrue(response.getDurationDays() >= 5);
    }

    @Test
    void loanHistoryResponse_from_returnedOnTimeHasTrueFlag() {
        LoanHistoryResponse response = LoanHistoryResponse.from(returnedLoanOnTime);

        assertEquals("RETURNED", response.getStatus());
        assertNotNull(response.getReturnDate());
        assertTrue(response.getReturnedOnTime());
    }

    @Test
    void loanHistoryResponse_from_returnedLateHasFalseFlag() {
        LoanHistoryResponse response = LoanHistoryResponse.from(returnedLoanLate);

        assertEquals("RETURNED", response.getStatus());
        assertNotNull(response.getReturnDate());
        assertFalse(response.getReturnedOnTime());
    }
}
