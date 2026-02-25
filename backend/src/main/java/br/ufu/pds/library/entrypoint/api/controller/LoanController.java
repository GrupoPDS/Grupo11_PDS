package br.ufu.pds.library.entrypoint.api.controller;

import br.ufu.pds.library.core.domain.Loan;
import br.ufu.pds.library.entrypoint.api.dto.CreateLoanRequest;
import br.ufu.pds.library.entrypoint.api.dto.LoanResponse;
import br.ufu.pds.library.infrastructure.service.LoanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @Operation(
            summary = "Listar todos os empréstimos",
            description = "Retorna uma lista de todos os empréstimos cadastrados no sistema")
    @ApiResponse(
            responseCode = "200",
            description = "Lista de empréstimos retornada com sucesso",
            content = @Content(schema = @Schema(implementation = LoanResponse.class)))
    public ResponseEntity<List<LoanResponse>> listAll() {
        List<LoanResponse> loans =
                loanService.findAll().stream().map(LoanResponse::fromEntity).toList();
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
}
