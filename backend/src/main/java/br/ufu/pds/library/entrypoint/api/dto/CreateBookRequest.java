package br.ufu.pds.library.entrypoint.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookRequest {

    @NotBlank(message = "O título é obrigatório")
    @Size(max = 255)
    private String title;

    @NotBlank(message = "O autor é obrigatório")
    @Size(max = 255)
    private String author;

    @NotBlank(message = "O ISBN é obrigatório")
    @Size(max = 20)
    private String isbn;

    @Size(max = 255)
    private String publisher;

    private Integer year;

    @NotNull(message = "A quantidade é obrigatória")
    @Positive(message = "A quantidade deve ser maior que zero")
    private Integer quantity;

    @Size(max = 100)
    private String category;
}
