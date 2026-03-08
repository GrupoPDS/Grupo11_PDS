package br.ufu.pds.library.entrypoint.api.dto;

import br.ufu.pds.library.core.domain.Loan;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DTO que representa um item individual no relatório de atrasos. Contém dados do empréstimo, do
 * usuário e do livro, além de dias em atraso.
 */
@Getter
@AllArgsConstructor
public class OverdueItemResponse {

    private Long loanId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private String bookTitle;
    private String bookIsbn;
    private LocalDate loanDate;
    private LocalDate dueDate;
    private int daysOverdue;
    private String severity;

    /**
     * Converte um Loan (com status OVERDUE) em OverdueItemResponse. Calcula automaticamente o
     * número de dias em atraso e a severidade.
     */
    public static OverdueItemResponse from(Loan loan) {
        int daysLate = (int) ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now());
        String severity = classifySeverity(daysLate);

        return new OverdueItemResponse(
                loan.getId(),
                loan.getUser().getName(),
                loan.getUser().getEmail(),
                loan.getUser().getPhone(),
                loan.getBook().getTitle(),
                loan.getBook().getIsbn(),
                loan.getLoanDate(),
                loan.getDueDate(),
                daysLate,
                severity);
    }

    /**
     * Classifica a severidade do atraso: - LOW: 1–7 dias - MEDIUM: 8–14 dias - HIGH: 15–30 dias -
     * CRITICAL: 31+ dias
     */
    private static String classifySeverity(int daysOverdue) {
        if (daysOverdue <= 7) return "LOW";
        if (daysOverdue <= 14) return "MEDIUM";
        if (daysOverdue <= 30) return "HIGH";
        return "CRITICAL";
    }
}
