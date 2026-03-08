package br.ufu.pds.library.entrypoint.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoanHistorySummaryResponse {

    private long totalLoans;
    private long activeLoans;
    private long returnedLoans;
    private long overdueLoans;
    private double onTimeReturnRate;
}
