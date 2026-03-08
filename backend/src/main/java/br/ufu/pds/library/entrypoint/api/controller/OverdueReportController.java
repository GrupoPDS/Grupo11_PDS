package br.ufu.pds.library.entrypoint.api.controller;

import br.ufu.pds.library.entrypoint.api.dto.OverdueItemResponse;
import br.ufu.pds.library.entrypoint.api.dto.OverdueReportSummaryResponse;
import br.ufu.pds.library.entrypoint.api.dto.PagedResponse;
import br.ufu.pds.library.infrastructure.service.OverdueReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/overdue-report")
@RequiredArgsConstructor
@Tag(
        name = "Overdue Report",
        description = "Relatório de atrasos para bibliotecários e administradores")
public class OverdueReportController {

    private final OverdueReportService overdueReportService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @Operation(
            summary = "Listar empréstimos em atraso",
            description =
                    "Retorna lista paginada de empréstimos com status OVERDUE, "
                            + "com filtro textual opcional por nome, e-mail, título ou ISBN, "
                            + "e filtro por dias mínimos de atraso")
    @ApiResponse(responseCode = "200", description = "Lista de atrasos retornada com sucesso")
    @ApiResponse(responseCode = "403", description = "Acesso negado — apenas ADMIN e LIBRARIAN")
    public ResponseEntity<PagedResponse<OverdueItemResponse>> listOverdueLoans(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer minDays,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(overdueReportService.getOverdueLoans(search, minDays, page, size));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @Operation(
            summary = "Resumo estatístico de atrasos",
            description =
                    "Retorna total em atraso, percentual sobre ativos, média de dias "
                            + "e distribuição por severidade (LOW, MEDIUM, HIGH, CRITICAL)")
    @ApiResponse(responseCode = "200", description = "Resumo retornado com sucesso")
    @ApiResponse(responseCode = "403", description = "Acesso negado — apenas ADMIN e LIBRARIAN")
    public ResponseEntity<OverdueReportSummaryResponse> getOverdueSummary() {
        return ResponseEntity.ok(overdueReportService.getOverdueSummary());
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @Operation(
            summary = "Exportar relatório de atrasos em CSV",
            description =
                    "Gera download de arquivo CSV com todos os empréstimos em atraso, "
                            + "incluindo dados do leitor (nome, email, telefone), livro, dias de atraso e severidade")
    @ApiResponse(responseCode = "200", description = "Arquivo CSV gerado com sucesso")
    @ApiResponse(responseCode = "403", description = "Acesso negado — apenas ADMIN e LIBRARIAN")
    public ResponseEntity<byte[]> exportOverdueCsv() {
        byte[] csvBytes = overdueReportService.exportOverdueCsv();

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=relatorio-atrasos.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .contentLength(csvBytes.length)
                .body(csvBytes);
    }
}
