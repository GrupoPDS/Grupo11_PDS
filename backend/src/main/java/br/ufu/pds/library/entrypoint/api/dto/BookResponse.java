package br.ufu.pds.library.entrypoint.api.dto;

import br.ufu.pds.library.core.domain.Book;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BookResponse fromEntity(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getPublisher(),
                book.getYear(),
                book.getQuantity(),
                book.getCreatedAt(),
                book.getUpdatedAt()
        );
    }
}
