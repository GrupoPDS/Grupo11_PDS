package br.ufu.pds.library.entrypoint.api.controller;

import br.ufu.pds.library.core.domain.Book;
import br.ufu.pds.library.entrypoint.api.dto.BookResponse;
import br.ufu.pds.library.entrypoint.api.dto.CreateBookRequest;
import br.ufu.pds.library.infrastructure.service.BookService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        return ResponseEntity.status(HttpStatus.CREATED).body(BookResponse.fromEntity(saved));
    }

    @GetMapping
    public ResponseEntity<List<BookResponse>> listAll() {
        List<BookResponse> books =
                bookService.findAll().stream().map(BookResponse::fromEntity).toList();

        return ResponseEntity.ok(books);
    }
}
