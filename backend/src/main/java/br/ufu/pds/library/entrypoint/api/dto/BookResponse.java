package br.ufu.pds.library.entrypoint.api.dto;

import br.ufu.pds.library.core.domain.Book;
import br.ufu.pds.library.core.domain.BookCopy;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BookResponse {

    private Long id;
    private String title;
    private String author;
    private String isbn;
    private String publisher;
    private Integer year;
    private Integer quantity;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer availableCopies;
    private List<BookCopyResponse> copies;

    public static BookResponse fromEntity(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getPublisher(),
                book.getYear(),
                book.getQuantity(),
                book.getCategory(),
                book.getCreatedAt(),
                book.getUpdatedAt(),
                book.getQuantity(),
                List.of());
    }

    public static BookResponse fromEntity(Book book, int availableCopies) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getPublisher(),
                book.getYear(),
                book.getQuantity(),
                book.getCategory(),
                book.getCreatedAt(),
                book.getUpdatedAt(),
                availableCopies,
                List.of());
    }

    public static BookResponse fromEntity(Book book, int availableCopies, List<BookCopy> copies) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getPublisher(),
                book.getYear(),
                book.getQuantity(),
                book.getCategory(),
                book.getCreatedAt(),
                book.getUpdatedAt(),
                availableCopies,
                copies != null
                        ? copies.stream().map(BookCopyResponse::fromEntity).toList()
                        : List.of());
    }
}
