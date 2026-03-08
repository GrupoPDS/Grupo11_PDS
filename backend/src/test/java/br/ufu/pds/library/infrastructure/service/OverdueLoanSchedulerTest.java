package br.ufu.pds.library.infrastructure.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.ufu.pds.library.core.domain.*;
import br.ufu.pds.library.infrastructure.persistence.LoanRepository;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OverdueLoanSchedulerTest {

    @Mock private LoanRepository loanRepository;

    @InjectMocks private OverdueLoanScheduler overdueLoanScheduler;

    private User user;
    private Book book;

    @BeforeEach
    void setup() {
        user = User.builder().id(1L).name("Maria").email("maria@ufu.br").role("STUDENT").build();
        book =
                Book.builder()
                        .id(1L)
                        .title("Clean Code")
                        .author("Robert Martin")
                        .isbn("978-0132350884")
                        .quantity(2)
                        .build();
    }

    @Test
    @DisplayName("Deve marcar corretamente 2 empréstimos vencidos como OVERDUE")
    void should_markAsOverdue_when_loansArePastDue() {
        Loan loan1 =
                Loan.builder()
                        .id(1L)
                        .user(user)
                        .book(book)
                        .loanDate(LocalDate.now().minusDays(20))
                        .dueDate(LocalDate.now().minusDays(6))
                        .status(LoanStatus.ACTIVE)
                        .build();

        Loan loan2 =
                Loan.builder()
                        .id(2L)
                        .user(user)
                        .book(book)
                        .loanDate(LocalDate.now().minusDays(18))
                        .dueDate(LocalDate.now().minusDays(4))
                        .status(LoanStatus.ACTIVE)
                        .build();

        when(loanRepository.findByStatusAndDueDateBefore(LoanStatus.ACTIVE, LocalDate.now()))
                .thenReturn(List.of(loan1, loan2));

        overdueLoanScheduler.markOverdueLoans();

        assertEquals(LoanStatus.OVERDUE, loan1.getStatus());
        assertEquals(LoanStatus.OVERDUE, loan2.getStatus());
        verify(loanRepository).saveAll(List.of(loan1, loan2));
    }

    @Test
    @DisplayName("Empréstimos com dueDate = hoje NÃO devem ser marcados (só < hoje)")
    void should_notMark_when_dueDateIsToday() {
        // findByStatusAndDueDateBefore usa "before" (estritamente <)
        // então se dueDate = hoje, o repo não os retorna
        when(loanRepository.findByStatusAndDueDateBefore(LoanStatus.ACTIVE, LocalDate.now()))
                .thenReturn(Collections.emptyList());

        overdueLoanScheduler.markOverdueLoans();

        verify(loanRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Empréstimos já RETURNED não devem ser afetados")
    void should_notAffect_when_loansAlreadyReturned() {
        // O scheduler busca apenas ACTIVE, então RETURNED nunca aparece
        when(loanRepository.findByStatusAndDueDateBefore(LoanStatus.ACTIVE, LocalDate.now()))
                .thenReturn(Collections.emptyList());

        overdueLoanScheduler.markOverdueLoans();

        verify(loanRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Zero empréstimos vencidos → nenhuma escrita no banco")
    void should_doNothing_when_noOverdueLoans() {
        when(loanRepository.findByStatusAndDueDateBefore(LoanStatus.ACTIVE, LocalDate.now()))
                .thenReturn(Collections.emptyList());

        overdueLoanScheduler.markOverdueLoans();

        verify(loanRepository, never()).saveAll(anyList());
    }
}
