package br.ufu.pds.library.entrypoint.api.dto;

import br.ufu.pds.library.core.domain.BookCopy;

/** DTO para representar um exemplar individual de um livro. */
public record BookCopyResponse(Long id, String copyCode) {

    public static BookCopyResponse fromEntity(BookCopy copy) {
        return new BookCopyResponse(copy.getId(), copy.getCopyCode());
    }
}
