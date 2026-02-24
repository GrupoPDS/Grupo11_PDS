package br.ufu.pds.library.entrypoint.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "O email é obrigatório")
    @Email(message = "Email inválido")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "A senha é obrigatória")
    @Size(max = 255)
    private String password;

    @Size(max = 50)
    private String role;
}
