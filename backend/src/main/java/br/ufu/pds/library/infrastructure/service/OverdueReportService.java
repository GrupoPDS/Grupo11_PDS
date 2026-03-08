package br.ufu.pds.library.infrastructure.service;

import br.ufu.pds.library.core.domain.Loan;
import br.ufu.pds.library.core.domain.LoanStatus;
import br.ufu.pds.library.entrypoint.api.dto.OverdueItemResponse;
import br.ufu.pds.library.entrypoint.api.dto.OverdueReportSummaryResponse;
import br.ufu.pds.library.entrypoint.api.dto.PagedResponse;
import br.ufu.pds.library.infrastructure.persistence.LoanRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OverdueReportService {

    private final LoanRepository loanRepository;

    /**
     * Retorna a lista paginada de empréstimos em atraso. Suporta busca textual por nome do usuário,
     * e-mail, título ou ISBN. Suporta filtro por dias mínimos de atraso (minDaysOverdue).
     */
    @Transactional(readOnly = true)
    public PagedResponse<OverdueItemResponse> getOverdueLoans(
            String search, Integer minDaysOverdue, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Loan> loanPage;

        if (search != null && !search.isBlank()) {
            // Busca com filtro textual — retorna lista e pagina manualmente
            List<Loan> filtered = loanRepository.searchOverdueLoans(search.trim());
            if (minDaysOverdue != null && minDaysOverdue > 0) {
                LocalDate maxDueDate = LocalDate.now().minusDays(minDaysOverdue);
                filtered =
                        filtered.stream().filter(l -> !l.getDueDate().isAfter(maxDueDate)).toList();
            }
            int start = Math.min((int) pageable.getOffset(), filtered.size());
            int end = Math.min(start + pageable.getPageSize(), filtered.size());
            loanPage = new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
        } else if (minDaysOverdue != null && minDaysOverdue > 0) {
            // Filtro apenas por dias mínimos
            LocalDate maxDueDate = LocalDate.now().minusDays(minDaysOverdue);
            List<Loan> filtered = loanRepository.findOverdueWithMinDays(maxDueDate);
            int start = Math.min((int) pageable.getOffset(), filtered.size());
            int end = Math.min(start + pageable.getPageSize(), filtered.size());
            loanPage = new PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
        } else {
            loanPage = loanRepository.findAllOverduePaged(pageable);
        }

        List<OverdueItemResponse> content =
                loanPage.getContent().stream().map(OverdueItemResponse::from).toList();

        return new PagedResponse<>(
                content,
                loanPage.getNumber(),
                loanPage.getSize(),
                loanPage.getTotalElements(),
                loanPage.getTotalPages(),
                loanPage.hasNext());
    }

    /**
     * Retorna o resumo estatístico do relatório de atrasos: total em atraso, total ativos,
     * percentual, média de dias e distribuição por severidade.
     */
    @Transactional(readOnly = true)
    public OverdueReportSummaryResponse getOverdueSummary() {
        long totalOverdue = loanRepository.countByStatus(LoanStatus.OVERDUE);
        long totalActive = loanRepository.countByStatus(LoanStatus.ACTIVE);
        long totalCombined = totalOverdue + totalActive;

        double overduePercentage =
                totalCombined > 0 ? (double) totalOverdue / totalCombined * 100.0 : 0.0;

        // Calcula média de dias em atraso e distribuição por severidade
        double averageDays = 0.0;
        long low = 0, medium = 0, high = 0, critical = 0;

        if (totalOverdue > 0) {
            List<Loan> allOverdue = loanRepository.findAllOverdueWithDetails();
            LocalDate today = LocalDate.now();
            long totalDays = 0;

            for (Loan loan : allOverdue) {
                int daysLate = (int) ChronoUnit.DAYS.between(loan.getDueDate(), today);
                totalDays += daysLate;

                if (daysLate <= 7) low++;
                else if (daysLate <= 14) medium++;
                else if (daysLate <= 30) high++;
                else critical++;
            }

            averageDays = (double) totalDays / allOverdue.size();
        }

        return new OverdueReportSummaryResponse(
                totalOverdue,
                totalActive + totalOverdue,
                Math.round(overduePercentage * 100.0) / 100.0,
                Math.round(averageDays * 10.0) / 10.0,
                low,
                medium,
                high,
                critical,
                LocalDateTime.now());
    }

    /**
     * Gera CSV com todos os empréstimos em atraso, ordenados por daysOverdue DESC (prioridade de
     * cobrança). Encoding UTF-8 com BOM para compatibilidade com Excel.
     */
    @Transactional(readOnly = true)
    public byte[] exportOverdueCsv() {
        List<Loan> allOverdue = loanRepository.findAllOverdueWithDetails();

        List<OverdueItemResponse> items =
                allOverdue.stream()
                        .map(OverdueItemResponse::from)
                        .sorted(
                                Comparator.comparingInt(OverdueItemResponse::getDaysOverdue)
                                        .reversed())
                        .toList();

        StringBuilder sb = new StringBuilder();
        // BOM UTF-8 para compatibilidade com Excel
        sb.append('\uFEFF');
        sb.append(
                "Leitor,Email,Telefone,Livro,ISBN,Data Empréstimo,Prazo,Dias Atraso,Severidade\n");

        for (OverdueItemResponse item : items) {
            sb.append(escapeCsv(item.getUserName())).append(',');
            sb.append(escapeCsv(item.getUserEmail())).append(',');
            sb.append(escapeCsv(item.getUserPhone() != null ? item.getUserPhone() : ""))
                    .append(',');
            sb.append(escapeCsv(item.getBookTitle())).append(',');
            sb.append(escapeCsv(item.getBookIsbn())).append(',');
            sb.append(item.getLoanDate()).append(',');
            sb.append(item.getDueDate()).append(',');
            sb.append(item.getDaysOverdue()).append(',');
            sb.append(item.getSeverity()).append('\n');
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Escapa valores CSV: se contém vírgula, aspas ou quebra de linha, envolve em aspas duplas. */
    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
