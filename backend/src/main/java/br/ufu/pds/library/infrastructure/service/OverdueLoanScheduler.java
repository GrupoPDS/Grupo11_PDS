package br.ufu.pds.library.infrastructure.service;

import br.ufu.pds.library.core.domain.Loan;
import br.ufu.pds.library.core.domain.LoanStatus;
import br.ufu.pds.library.infrastructure.persistence.LoanRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Job agendado que roda diariamente à meia-noite e marca empréstimos vencidos como OVERDUE. Sem
 * este job, o status OVERDUE nunca seria ativado e o relatório de atrasos ficaria vazio.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OverdueLoanScheduler {

    private final LoanRepository loanRepository;

    /**
     * Executa diariamente à meia-noite (00:00:00). Busca empréstimos ACTIVE com dueDate
     * estritamente anterior a hoje (< hoje, não <=).
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void markOverdueLoans() {
        LocalDate today = LocalDate.now();
        List<Loan> overdueLoans =
                loanRepository.findByStatusAndDueDateBefore(LoanStatus.ACTIVE, today);

        if (overdueLoans.isEmpty()) {
            log.info("Nenhum empréstimo vencido encontrado para marcar como OVERDUE.");
            return;
        }

        overdueLoans.forEach(loan -> loan.setStatus(LoanStatus.OVERDUE));
        loanRepository.saveAll(overdueLoans);

        log.info("Marcados {} empréstimos como OVERDUE", overdueLoans.size());
    }
}
