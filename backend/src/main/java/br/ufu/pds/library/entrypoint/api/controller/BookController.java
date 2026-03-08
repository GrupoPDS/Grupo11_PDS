package br.ufu.pds.library.entrypoint.api.controller;

import br.ufu.pds.library.core.domain.Book;
import br.ufu.pds.library.core.domain.BookCopy;
import br.ufu.pds.library.entrypoint.api.dto.BookCopyDetailResponse;
import br.ufu.pds.library.entrypoint.api.dto.BookResponse;
import br.ufu.pds.library.entrypoint.api.dto.CreateBookRequest;
import br.ufu.pds.library.infrastructure.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @PostMapping
    public ResponseEntity<BookResponse> create(@Valid @RequestBody CreateBookRequest request) {
        Book book =
                Book.builder()
                        .title(request.getTitle())
                        .author(request.getAuthor())
                        .isbn(request.getIsbn())
                        .publisher(request.getPublisher())
                        .year(request.getYear())
                        .quantity(request.getQuantity())
                        .build();

        Book saved = bookService.save(book);
        List<BookCopy> copies = bookService.findCopiesByBookId(saved.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BookResponse.fromEntity(saved, saved.getQuantity(), copies));
    }

    @GetMapping
    @Operation(
            summary = "Listar ou buscar livros",
            description =
                    "Retorna todos os livros ou filtra por título/autor se parâmetro q for fornecido")
    @ApiResponse(responseCode = "200", description = "Lista de livros retornada com sucesso")
    public ResponseEntity<List<BookResponse>> list(@RequestParam(required = false) String q) {
        List<Book> books = bookService.search(q);
        Map<Long, Integer> availability = bookService.calculateAvailability(books);
        List<Long> bookIds = books.stream().map(Book::getId).toList();
        Map<Long, List<BookCopy>> copiesMap = bookService.findCopiesByBookIds(bookIds);
        List<BookResponse> responses =
                books.stream()
                        .map(
                                b ->
                                        BookResponse.fromEntity(
                                                b,
                                                availability.getOrDefault(
                                                        b.getId(), b.getQuantity()),
                                                copiesMap.getOrDefault(b.getId(), List.of())))
                        .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar livro por ID",
            description = "Retorna um livro específico pelo seu identificador")
    @ApiResponse(responseCode = "200", description = "Livro encontrado com sucesso")
    @ApiResponse(responseCode = "404", description = "Livro não encontrado")
    public ResponseEntity<BookResponse> getById(@PathVariable Long id) {
        Book book = bookService.findById(id);
        List<BookCopy> copies = bookService.findCopiesByBookId(id);
        Map<Long, Integer> availability = bookService.calculateAvailability(List.of(book));
        return ResponseEntity.ok(
                BookResponse.fromEntity(
                        book, availability.getOrDefault(id, book.getQuantity()), copies));
    }

    @GetMapping("/{id}/copies")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')")
    @Operation(
            summary = "Detalhes dos exemplares de um livro",
            description =
                    "Retorna todos os exemplares de um livro com status de empréstimo, "
                            + "nome do mutuário e data de devolução. Apenas para ADMIN/LIBRARIAN.")
    @ApiResponse(responseCode = "200", description = "Exemplares retornados com sucesso")
    @ApiResponse(responseCode = "404", description = "Livro não encontrado")
    public ResponseEntity<List<BookCopyDetailResponse>> getCopyDetails(@PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookCopyDetails(id));
    }
}
