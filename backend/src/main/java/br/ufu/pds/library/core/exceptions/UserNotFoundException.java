package br.ufu.pds.library.core.exceptions;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(Long id) {
        super("Usuário não encontrado com o ID: " + id);
    }
}
