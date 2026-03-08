package br.ufu.pds.library.infrastructure.jobs;

import br.ufu.pds.library.core.domain.LoanStatus;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoanStatusUpdaterJob {

    private final EntityManager entityManager;

    /**
     * Roda todos os dias de madrugada (00:01) Procura empréstimos ATIVOS cujo dueDate < hoje e
     * altera para OVERDUE.
     */
    @Scheduled(cron = "0 1 0 * * *")
    @Transactional
    public void updateOverdueLoans() {
        log.info("Iniciando rotina de atualização de empréstimos em atraso...");

        int updatedCount =
                entityManager
                        .createQuery(
                                "UPDATE Loan l SET l.status = :overdue "
                                        + "WHERE l.status = :active AND l.dueDate < CURRENT_DATE")
                        .setParameter("overdue", LoanStatus.OVERDUE)
                        .setParameter("active", LoanStatus.ACTIVE)
                        .executeUpdate();

        log.info("Rotina finalizada. Empréstimos marcados como atrasados hoje: {}", updatedCount);
    }
}
