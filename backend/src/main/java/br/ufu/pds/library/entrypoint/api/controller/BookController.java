package br.ufu.pds.library.entrypoint.api.controller;

import br.ufu.pds.library.core.domain.Book;
import br.ufu.pds.library.entrypoint.api.dto.BookResponse;
import br.ufu.pds.library.entrypoint.api.dto.CreateBookRequest;
import br.ufu.pds.library.infrastructure.service.BookService;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Tag(name = "Books", description = "Operações relacionadas a livros")
public class BookController {

    private final BookService bookService;

    @PostMapping
    @Operation(
            summary = "Cadastrar um novo livro",
            description = "Cria um novo livro no sistema com título, autor, ISBN e categoria.")
    @ApiResponse(
            responseCode = "201",
            description = "Livro cadastrado com sucesso",
            content = @Content(schema = @Schema(implementation = BookResponse.class)))
    @ApiResponse(
            responseCode = "409",
            description = "ISBN já existe no sistema (Duplicate)")
    @ApiResponse(
            responseCode = "400",
            description = "Dados inválidos ou incompletos")
    public ResponseEntity<BookResponse> create(@Valid @RequestBody CreateBookRequest request) {
        Book book =
                Book.builder()
                        .title(request.getTitle())
                        .author(request.getAuthor())
                        .isbn(request.getIsbn())
                        .publisher(request.getPublisher())
                        .year(request.getYear())
                        .quantity(request.getQuantity())
                        .category(request.getCategory())
                        .build();

        Book saved = bookService.save(book);
        return ResponseEntity.status(HttpStatus.CREATED).body(BookResponse.fromEntity(saved));
    }

    @GetMapping
    @Operation(
            summary = "Listar todos os livros",
            description = "Retorna uma lista de todos os livros cadastrados no sistema")
    @ApiResponse(
            responseCode = "200",
            description = "Lista de livros retornada com sucesso",
            content = @Content(schema = @Schema(implementation = BookResponse.class)))
    public ResponseEntity<List<BookResponse>> listAll() {
        List<BookResponse> books =
                bookService.findAll().stream().map(BookResponse::fromEntity).toList();

        return ResponseEntity.ok(books);
    }
}
