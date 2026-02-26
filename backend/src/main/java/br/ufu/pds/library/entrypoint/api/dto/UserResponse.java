package br.ufu.pds.library.entrypoint.api.dto;

import br.ufu.pds.library.core.domain.User;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        String role,
        Boolean active,
        LocalDateTime createdAt) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getActive(),
                user.getCreatedAt());
    }
}
