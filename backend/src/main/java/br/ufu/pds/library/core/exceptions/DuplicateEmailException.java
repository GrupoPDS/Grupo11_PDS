package br.ufu.pds.library.core.exceptions;

public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException(String email) {
        super("Já existe um usuário cadastrado com o email: " + email);
    }
}
