package br.ufu.pds.library.core.exceptions;

public class BookNotFoundException extends RuntimeException {

    public BookNotFoundException(Long id) {
        super("Livro não encontrado com o ID: " + id);
    }
}
