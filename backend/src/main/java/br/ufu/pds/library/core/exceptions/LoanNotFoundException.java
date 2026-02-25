package br.ufu.pds.library.core.exceptions;

public class LoanNotFoundException extends BusinessException {

    public LoanNotFoundException(Long id) {
        super("Empréstimo não encontrado com o ID: " + id);
    }
}
