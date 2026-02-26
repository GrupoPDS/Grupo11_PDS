package br.ufu.pds.library.entrypoint.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de criação/atualização de usuário (usado pelo Controller). Usamos record para evitar
 * exposição direta da entidade e prevenir mass-assignment.
 */
public record CreateUserRequest(
        @NotBlank(message = "O nome é obrigatório") @Size(max = 255) String name,
        @NotBlank(message = "O email é obrigatório")
                @Email(message = "Email inválido")
                @Size(max = 255)
                String email,
        @Size(max = 20) String phone,
        @Size(max = 50) String role) {
    public CreateUserRequest {
        if (role == null || role.isBlank()) {
            role = "STUDENT";
        }
    }
}
