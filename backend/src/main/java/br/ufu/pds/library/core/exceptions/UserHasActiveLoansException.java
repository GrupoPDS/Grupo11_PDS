package br.ufu.pds.library.core.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UserHasActiveLoansException extends BusinessException {
    public UserHasActiveLoansException(Long id) {
        super("Usuário com ID " + id + " possui empréstimos ativos e não pode ser desativado.");
    }
}
