package br.ufu.pds.library.entrypoint.api.dto;

import jakarta.validation.constraints.NotNull;

public record ReservationRequest(@NotNull(message = "O ID do livro é obrigatório") Long bookId) {}
