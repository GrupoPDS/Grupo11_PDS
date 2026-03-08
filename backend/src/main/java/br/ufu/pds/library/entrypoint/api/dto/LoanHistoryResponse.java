package br.ufu.pds.library.entrypoint.api.dto;

import br.ufu.pds.library.core.domain.Loan;
import br.ufu.pds.library.core.domain.LoanStatus;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoanHistoryResponse {

    private Long id;
    private String bookTitle;
    private String bookAuthor;
    private String bookIsbn;
    private LocalDate loanDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private String status;
    private Integer durationDays;
    private Boolean returnedOnTime;
    private String copyCode;

    public static LoanHistoryResponse from(Loan loan) {
        LocalDate endDate = loan.getReturnDate() != null ? loan.getReturnDate() : LocalDate.now();
        int duration = (int) ChronoUnit.DAYS.between(loan.getLoanDate(), endDate);

        Boolean onTime = null;
        if (loan.getStatus() == LoanStatus.RETURNED && loan.getReturnDate() != null) {
            onTime = !loan.getReturnDate().isAfter(loan.getDueDate());
        }

        return new LoanHistoryResponse(
                loan.getId(),
                loan.getBook().getTitle(),
                loan.getBook().getAuthor(),
                loan.getBook().getIsbn(),
                loan.getLoanDate(),
                loan.getDueDate(),
                loan.getReturnDate(),
                loan.getStatus().name(),
                duration,
                onTime,
                loan.getBookCopy() != null ? loan.getBookCopy().getCopyCode() : null);
    }
}
