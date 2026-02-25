package br.ufu.pds.library.core.exceptions;

public class BookNotAvailableException extends BusinessException {

    public BookNotAvailableException(Long bookId) {
        super("Livro com ID " + bookId + " não possui exemplares disponíveis para empréstimo");
    }
}
