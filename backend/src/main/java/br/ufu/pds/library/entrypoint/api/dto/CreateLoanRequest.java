package br.ufu.pds.library.entrypoint.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateLoanRequest {

    @NotNull(message = "O ID do usuário é obrigatório")
    private Long userId;

    @NotNull(message = "O ID do livro é obrigatório")
    private Long bookId;

    @NotNull(message = "A data de devolução prevista é obrigatória")
    @Future(message = "A data de devolução deve ser uma data futura")
    private LocalDate dueDate;
}
