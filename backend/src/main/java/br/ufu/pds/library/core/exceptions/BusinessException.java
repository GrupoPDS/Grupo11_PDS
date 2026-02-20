package br.ufu.pds.library.core.exceptions;

/**
 * Classe base para todas as exceções de negócio do domínio.
 * Qualquer exceção que represente uma regra de negócio violada deve estender
 * esta classe.
 * O GlobalExceptionHandler trata automaticamente todas as subclasses.
 */
public abstract class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
