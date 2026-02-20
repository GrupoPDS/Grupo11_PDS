package br.ufu.pds.library.entrypoint.api.controller;

import br.ufu.pds.library.core.exceptions.BookNotFoundException;
import br.ufu.pds.library.core.exceptions.BusinessException;
import br.ufu.pds.library.core.exceptions.DuplicateIsbnException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tratador global de exceções da API.
 * Intercepta exceções lançadas pelos Controllers e retorna respostas HTTP
 * padronizadas.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // =============================================
    // Exceções específicas de negócio
    // =============================================

    /**
     * ISBN duplicado → 409 Conflict
     */
    @ExceptionHandler(DuplicateIsbnException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateIsbn(DuplicateIsbnException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Recurso não encontrado → 404 Not Found
     */
    @ExceptionHandler(BookNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleBookNotFound(BookNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // =============================================
    // Exceção genérica de negócio (catch-all para subclasses de BusinessException)
    // =============================================

    /**
     * Qualquer outra exceção de negócio → 400 Bad Request
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // =============================================
    // Erros de validação (@Valid nos DTOs)
    // =============================================

    /**
     * Falha na validação dos campos do DTO → 400 Bad Request com detalhes dos
     * campos
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<Map<String, String>> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> {
                    Map<String, String> fieldError = new HashMap<>();
                    fieldError.put("campo", error.getField());
                    fieldError.put("mensagem", error.getDefaultMessage());
                    return fieldError;
                })
                .collect(Collectors.toList());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Erro de validação");
        body.put("detalhes", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // =============================================
    // Exceções inesperadas (fallback)
    // =============================================

    /**
     * Qualquer exceção não tratada → 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno do servidor. Contate o administrador.");
    }

    // =============================================
    // Helper
    // =============================================

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", message);

        return ResponseEntity.status(status).body(body);
    }
}
