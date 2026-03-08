package br.ufu.pds.library.entrypoint.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO para autoempréstimo pelo estudante. Apenas o bookId é necessário; userId vem do token e
 * dueDate é automático (14 dias).
 */
public record BorrowBookRequest(@NotNull(message = "O ID do livro é obrigatório") Long bookId) {}
