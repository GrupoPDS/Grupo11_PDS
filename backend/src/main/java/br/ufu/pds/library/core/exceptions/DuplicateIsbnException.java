package br.ufu.pds.library.core.exceptions;

public class DuplicateIsbnException extends RuntimeException {

    public DuplicateIsbnException(String isbn) {
        super("Já existe um livro cadastrado com o ISBN: " + isbn);
    }
}
