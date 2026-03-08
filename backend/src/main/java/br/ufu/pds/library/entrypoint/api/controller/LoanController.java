package br.ufu.pds.library.entrypoint.api.controller;

import br.ufu.pds.library.core.domain.Loan;
import br.ufu.pds.library.core.domain.User;
import br.ufu.pds.library.entrypoint.api.dto.BorrowBookRequest;
import br.ufu.pds.library.entrypoint.api.dto.CreateLoanRequest;
import br.ufu.pds.library.entrypoint.api.dto.LoanHistoryResponse;
import br.ufu.pds.library.entrypoint.api.dto.LoanHistorySummaryResponse;
import br.ufu.pds.library.entrypoint.api.dto.LoanResponse;
import br.ufu.pds.library.entrypoint.api.dto.PagedResponse;
import br.ufu.pds.library.infrastructure.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@Tag(name = "Loans", description = "Operações relacionadas a empréstimos de livros")
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @Operation(
            summary = "Registrar um novo empréstimo",
            description =
                    "Cria um novo empréstimo de livro para um usuário. "
                            + "Verifica disponibilidade do livro automaticamente.")
    @ApiResponse(
            responseCode = "201",
            description = "Empréstimo registrado com sucesso",
            content = @Content(schema = @Schema(implementation = LoanResponse.class)))
    @ApiResponse(responseCode = "404", description = "Usuário ou livro não encontrado")
    @ApiResponse(responseCode = "400", description = "Livro indisponível ou dados inválidos")
    public ResponseEntity<LoanResponse> create(@Valid @RequestBody CreateLoanRequest request) {
        Loan saved =
                loanService.save(request.getUserId(), request.getBookId(), request.getDueDate());
        return ResponseEntity.status(HttpStatus.CREATED).body(LoanResponse.fromEntity(saved));
    }

    @PostMapping("/borrow")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Autoempréstimo de livro",
            description =
                    "Permite que qualquer usuário autenticado registre um empréstimo para si mesmo. "
                            + "O prazo de devolução é automático (14 dias). Verifica disponibilidade e respeita a fila de reservas.")
    @ApiResponse(
            responseCode = "201",
            description = "Empréstimo registrado com sucesso",
            content = @Content(schema = @Schema(implementation = LoanResponse.class)))
    @ApiResponse(responseCode = "400", description = "Livro indisponível para empréstimo")
    @ApiResponse(responseCode = "404", description = "Livro não encontrado")
    public ResponseEntity<LoanResponse> borrow(
            @Valid @RequestBody BorrowBookRequest request,
            @AuthenticationPrincipal User currentUser) {
        LocalDate dueDate = LocalDate.now().plusDays(14);
        Loan saved = loanService.save(currentUser.getId(), request.bookId(), dueDate);
        return ResponseEntity.status(HttpStatus.CREATED).body(LoanResponse.fromEntity(saved));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @Operation(
            summary = "Listar empréstimos com filtros",
            description =
                    "Retorna empréstimos ativos/em atraso. Aceita busca por email/ISBN e filtro por status.")
    @ApiResponse(
            responseCode = "200",
            description = "Lista de empréstimos retornada com sucesso",
            content = @Content(schema = @Schema(implementation = LoanResponse.class)))
    public ResponseEntity<List<LoanResponse>> listAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        List<LoanResponse> loans =
                loanService.searchActiveLoans(search, status).stream()
                        .map(LoanResponse::fromEntity)
                        .toList();
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN', 'STUDENT')")
    @Operation(
            summary = "Buscar empréstimo por ID",
            description = "Retorna os detalhes de um empréstimo específico")
    @ApiResponse(
            responseCode = "200",
            description = "Empréstimo encontrado",
            content = @Content(schema = @Schema(implementation = LoanResponse.class)))
    @ApiResponse(responseCode = "404", description = "Empréstimo não encontrado")
    public ResponseEntity<LoanResponse> findById(@PathVariable Long id) {
        Loan loan = loanService.findById(id);
        return ResponseEntity.ok(LoanResponse.fromEntity(loan));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN', 'STUDENT')")
    @Operation(
            summary = "Listar empréstimos por usuário",
            description = "Retorna todos os empréstimos de um usuário específico")
    @ApiResponse(
            responseCode = "200",
            description = "Lista de empréstimos do usuário",
            content = @Content(schema = @Schema(implementation = LoanResponse.class)))
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    public ResponseEntity<List<LoanResponse>> findByUserId(@PathVariable Long userId) {
        List<LoanResponse> loans =
                loanService.findByUserId(userId).stream().map(LoanResponse::fromEntity).toList();
        return ResponseEntity.ok(loans);
    }

    @PatchMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @Operation(
            summary = "Registrar devolução de livro",
            description =
                    "Registra a devolução de um empréstimo, "
                            + "atualizando o status para RETURNED e preenchendo a data de devolução.")
    @ApiResponse(
            responseCode = "200",
            description = "Devolução registrada com sucesso",
            content = @Content(schema = @Schema(implementation = LoanResponse.class)))
    @ApiResponse(responseCode = "404", description = "Empréstimo não encontrado")
    @ApiResponse(responseCode = "400", description = "Empréstimo já devolvido")
    public ResponseEntity<LoanResponse> returnLoan(@PathVariable Long id) {
        Loan returned = loanService.returnLoan(id);
        return ResponseEntity.ok(LoanResponse.fromEntity(returned));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Excluir um empréstimo",
            description = "Remove um empréstimo do sistema (somente ADMIN)")
    @ApiResponse(responseCode = "204", description = "Empréstimo excluído com sucesso")
    @ApiResponse(responseCode = "404", description = "Empréstimo não encontrado")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        loanService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // =============================================
    // Histórico de Leituras (Backlog 7)
    // =============================================

    @GetMapping("/my-history")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Histórico de empréstimos do leitor logado",
            description =
                    "Retorna o histórico paginado de empréstimos do usuário autenticado, com filtro opcional por status")
    @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso")
    public ResponseEntity<PagedResponse<LoanHistoryResponse>> getMyHistory(
            @RequestParam(required = false) List<String> status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                loanService.getLoanHistory(currentUser.getId(), status, page, size));
    }

    @GetMapping("/my-summary")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Resumo estatístico do leitor logado",
            description = "Retorna contadores e taxa de pontualidade do usuário autenticado")
    @ApiResponse(responseCode = "200", description = "Resumo retornado com sucesso")
    public ResponseEntity<LoanHistorySummaryResponse> getMySummary(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(loanService.getLoanSummary(currentUser.getId()));
    }

    @GetMapping("/user/{userId}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @Operation(
            summary = "Histórico de empréstimos de um leitor",
            description =
                    "Retorna o histórico paginado de qualquer leitor (apenas ADMIN/LIBRARIAN)")
    @ApiResponse(responseCode = "200", description = "Histórico retornado com sucesso")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    public ResponseEntity<PagedResponse<LoanHistoryResponse>> getUserHistory(
            @PathVariable Long userId,
            @RequestParam(required = false) List<String> status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(loanService.getLoanHistory(userId, status, page, size));
    }
}
