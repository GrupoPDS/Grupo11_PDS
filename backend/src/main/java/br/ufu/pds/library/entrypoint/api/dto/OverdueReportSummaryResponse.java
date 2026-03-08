package br.ufu.pds.library.entrypoint.api.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DTO com o resumo estatístico do relatório de atrasos. Fornece uma visão geral para o
 * bibliotecário.
 */
@Getter
@AllArgsConstructor
public class OverdueReportSummaryResponse {

    private long totalOverdue;
    private long totalActiveLoans;
    private double overduePercentage;
    private double averageDaysOverdue;
    private long lowSeverity;
    private long mediumSeverity;
    private long highSeverity;
    private long criticalSeverity;
    private LocalDateTime generatedAt;
}
