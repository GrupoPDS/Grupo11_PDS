package br.ufu.pds.library.entrypoint.api.dto;

import java.time.LocalDate;

/**
 * DTO detalhado de um exemplar individual, incluindo informações de empréstimo. Usado no painel
 * administrativo para visualizar o status de cada cópia.
 */
public record BookCopyDetailResponse(
        Long id,
        String copyCode,
        String status,
        String borrowerName,
        String borrowerEmail,
        LocalDate loanDate,
        LocalDate dueDate) {}
