package br.ufu.pds.library.core.exceptions;

public class BookAvailableException extends RuntimeException {
    public BookAvailableException(Long bookId) {
        super(
                "O livro com ID "
                        + bookId
                        + " já está disponível na estante para empréstimo imediato.");
    }
}
