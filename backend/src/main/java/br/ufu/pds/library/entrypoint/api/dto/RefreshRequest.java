package br.ufu.pds.library.entrypoint.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshRequest {

    @NotBlank(message = "O refresh token é obrigatório")
    private String refreshToken;
}
